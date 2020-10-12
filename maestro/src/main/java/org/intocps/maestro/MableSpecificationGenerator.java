package org.intocps.maestro;

import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.parser.MablParser;
import org.intocps.maestro.parser.ParseTree2AstConverter;
import org.intocps.maestro.plugin.*;
import org.intocps.maestro.typechecker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MableSpecificationGenerator {

    //This values is multiplied with the count of plugin function declarations.
    final static Logger logger = LoggerFactory.getLogger(MableSpecificationGenerator.class);
    final boolean verbose;
    final ISimulationEnvironment simulationEnvironment;
    private final Framework framework;
    private final MaestroConfiguration configuration;

    public MableSpecificationGenerator(Framework framework, boolean verbose, ISimulationEnvironment simulationEnvironment,
            MaestroConfiguration configuration) {
        this.framework = framework;
        this.verbose = verbose;
        this.simulationEnvironment = simulationEnvironment;
        this.configuration = configuration;
    }

    public MableSpecificationGenerator(Framework framework, boolean verbose, ISimulationEnvironment simulationEnvironment) {
        this(framework, verbose, simulationEnvironment, new MaestroConfiguration());
    }

    private static PluginEnvironment loadExpansionPlugins(TypeResolver typeResolver, RootEnvironment rootEnv, File contextFile, Framework framework,
            List<String> importModules) throws IOException {
        return loadExpansionPlugins(typeResolver, rootEnv, PluginFactory.parsePluginConfiguration(contextFile), framework, importModules);
    }

    private static PluginEnvironment loadExpansionPlugins(TypeResolver typeResolver, RootEnvironment rootEnv, InputStream contextFile,
            Framework framework, List<String> importModules) throws IOException {
        return loadExpansionPlugins(typeResolver, rootEnv, PluginFactory.parsePluginConfiguration(contextFile), framework, importModules);
    }

    private static PluginEnvironment loadExpansionPlugins(TypeResolver typeResolver, RootEnvironment rootEnv,
            Map<String, String> rawPluginJsonContext, Framework framework, List<String> importModules) {
        Collection<IMaestroExpansionPlugin> plugins = PluginFactory.getPlugins(IMaestroExpansionPlugin.class, framework);

        plugins.forEach(p -> logger.debug("Located plugins: {} - {}", p.getName(), p.getVersion()));

        Collection<IMaestroExpansionPlugin> pluginsToUnfold =
                plugins.stream().filter(plugin -> importModules.contains(plugin.getName())).collect(Collectors.toList());

        logger.debug("The following plugins will be used for unfolding: {}",
                pluginsToUnfold.stream().map(p -> p.getName() + "-" + p.getVersion()).collect(Collectors.joining(",", "[", "]")));

        logger.debug("Plugins declared functions: {}", pluginsToUnfold.stream().map(p -> p.getName() + "-" + p.getVersion() +
                p.getDeclaredUnfoldFunctions().stream().map(AFunctionDeclaration::toString).collect(Collectors.joining(",\n\t", "\n\t", "")))
                .collect(Collectors.joining(",\n", "\n[\n", "\n]")));


        return new PluginEnvironment(rootEnv, pluginsToUnfold.stream()
                .collect(Collectors.toMap(p -> p, p -> p.getDeclaredUnfoldFunctions().stream().collect(Collectors.toMap(Function.identity(), f -> {
                    try {
                        return (AFunctionType) typeResolver.resolve(f, rootEnv);
                    } catch (AnalysisException e) {
                        e.printStackTrace();
                        return null;
                    }
                })))), rawPluginJsonContext);
    }

    private static List<ARootDocument> parseStreams(List<CharStream> specStreams) {
        List<ARootDocument> documentList = new Vector<>();
        for (CharStream specStream : specStreams) {
            documentList.add(parse(specStream));
        }
        return documentList;
    }

    public static ARootDocument parse(CharStream specStreams) {
        MablLexer l = new MablLexer(specStreams);
        MablParser p = new MablParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
                    RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        MablParser.CompilationUnitContext unit = p.compilationUnit();

        ARootDocument root = (ARootDocument) new ParseTree2AstConverter().visit(unit);
        return root;
    }

    public static List<ARootDocument> parse(List<File> sourceFiles) throws IOException {
        List<ARootDocument> documentList = new Vector<>();

        for (File file : sourceFiles) {
            if (!file.exists()) {
                logger.warn("Unable to parse file. File does not exist: {}", file);
                continue;
            }
            logger.info("Parting file: {}", file);

            documentList.add(parse(CharStreams.fromPath(Paths.get(file.toURI()))));
        }
        return documentList;
    }

    public IMaestroConfiguration getConfiguration() {
        return this.configuration;
    }

    private ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit inputSimulationModule,
            IErrorReporter reporter, TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env) {

        ASimulationSpecificationCompilationUnit simulationModule = inputSimulationModule.clone();

        return expandExternals(simulationModule, reporter, typeResolver, comparator, env, 0);
    }

    private ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit simulationModule, IErrorReporter reporter,
            TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env, int depth) {

        Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins = env.getTypesPlugins();


        //TODO: It is not necessary to check if it is expand as all CallExps are expand.
        // CallExps to runtime modules are part of Dot Exp.
        List<ACallExp> aExternalStms =
                NodeCollector.collect(simulationModule, ACallExp.class).orElse(new Vector<>()).stream().filter(call -> call.getExpand() != null)
                        .collect(Collectors.toList());


        Map<ACallExp, Optional<PType>> externalTypeMap = aExternalStms.stream().collect(Collectors.toMap(Function.identity(), n -> {
            try {
                PType type = typeResolver.resolve(n, env);
                if (type != null) {
                    return Optional.of(type);
                } else {
                    return Optional.empty();
                }
            } catch (AnalysisException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }));

        externalTypeMap.entrySet().stream().filter(map -> !map.getValue().isPresent())
                .forEach(map -> reporter.report(0, String.format("Unknown external: '%s' at:", map.getKey().getMethodName().toString()), null));

        if (externalTypeMap.entrySet().stream().anyMatch(map -> !map.getValue().isPresent())) {
            throw new RuntimeException("Unknown externals present cannot proceed");
        }


        if (aExternalStms.isEmpty()) {
            return simulationModule;
        } else if (depth > configuration.maximumExpansionDepth) {
            throw new RuntimeException("Recursive external expansion larger than " + configuration.maximumExpansionDepth);
        }


        logger.debug("Externals {}",
                NodeCollector.collect(simulationModule, ACallExp.class).orElse(new Vector<>()).stream().map(m -> m.getMethodName().toString())
                        .collect(Collectors.joining(" , ", "[ ", " ]")));


        externalTypeMap.forEach((node, type) -> {

            if (type.isPresent()) {
                logger.debug("Unfolding node: {}", node);

                Predicate<Map.Entry<AFunctionDeclaration, AFunctionType>> typeCompatible =
                        (fmap) -> fmap.getKey().getName().getText().equals(node.getMethodName().toString()) &&
                                fmap.getKey().getFormals().size() == node.getArgs().size() && comparator.compatible(fmap.getValue(), type.get());


                Optional<Map.Entry<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>>> pluginMatch =
                        plugins.entrySet().stream().filter(map -> map.getValue().entrySet().stream().anyMatch(typeCompatible)).findFirst();

                if (pluginMatch.isPresent()) {
                    logger.trace("matched with {}- {}", pluginMatch.get().getKey().getName(),
                            pluginMatch.get().getValue().keySet().iterator().next());
                    pluginMatch.ifPresent(map -> {
                        map.getValue().entrySet().stream().filter(typeCompatible).findFirst().ifPresent(fmap -> {
                            logger.debug("Replacing external '{}' with unfoled statement", node.getMethodName().toString());

                            List<PStm> unfoled = null;
                            IMaestroExpansionPlugin plugin = map.getKey();
                            try {
                                if (plugin.requireConfig()) {
                                    try {
                                        IPluginConfiguration config = env.getConfiguration(plugin);
                                        unfoled = plugin.expand(fmap.getKey(), node.getArgs(), config, simulationEnvironment, reporter);
                                    } catch (PluginEnvironment.PluginConfigurationNotFoundException e) {
                                        logger.error("Could not obtain configuration for plugin '{}' at {}: {}", plugin.getName(),
                                                node.getMethodName().toString(), e.getMessage());
                                    }

                                } else {
                                    unfoled = plugin.expand(fmap.getKey(), node.getArgs(), null, simulationEnvironment, reporter);
                                }
                            } catch (ExpandException e) {
                                logger.error("Internal error in plug-in '{}' at {}. Message: {}", plugin.getName(), node.getMethodName().toString(),
                                        e.getMessage());
                            }
                            if (unfoled == null) {
                                reporter.report(999, String.format("Unfold failure in plugin %s for %s", plugin.getName(), node.getMethodName() + ""),
                                        null);
                            } else {
                                //replace the call and so rounding expression statement

                                if (node.parent().parent() instanceof ABlockStm) {

                                    //construct a new block body replacing the original node with the new statements
                                    ABlockStm block = (ABlockStm) node.parent().parent();
                                    int oldIndex = block.getBody().indexOf(node.parent());
                                    List<PStm> newBlock = new Vector<>();
                                    for (int i = 0; i < oldIndex; i++) {
                                        newBlock.add(block.getBody().get(i));
                                    }
                                    newBlock.addAll(unfoled);
                                    for (int i = oldIndex + 1; i < block.getBody().size(); i++) {
                                        newBlock.add(block.getBody().get(i));
                                    }
                                    //set the new block body, movin all children to this node
                                    block.setBody(newBlock);
                                } else {
                                    node.parent().parent().replaceChild(node.parent(), new ABlockStm(unfoled));
                                }

                            }
                        });
                    });
                } else {
                    logger.error("No plugin found for: {}", node);
                }
            }
        });

        return expandExternals(simulationModule, reporter, typeResolver, comparator, env, depth + 1);
    }

    public ARootDocument generateFromStreams(List<CharStream> sourceStreams, InputStream contextFile) throws IOException {
        List<ARootDocument> documentList = parseStreams(sourceStreams);
        return generateFromDocuments(documentList, contextFile);
    }

    private ARootDocument generateFromDocuments(List<ARootDocument> documentList, InputStream contextFile) throws IOException {
        IErrorReporter reporter = new ErrorReporter();

        List<AImportedModuleCompilationUnit> importedModules =
                documentList.stream().map(d -> NodeCollector.collect(d, AImportedModuleCompilationUnit.class)).filter(Optional::isPresent)
                        .map(Optional::get).flatMap(List::stream).filter(l -> !l.getFunctions().isEmpty()).collect(Collectors.toList());

        if (verbose) {
            logger.info("Module definitions: {}",
                    importedModules.stream().map(l -> l.getName().toString()).collect(Collectors.joining(" , ", "[ ", " ]")));
        }

        long simCount =
                documentList.stream().map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class)).filter(Optional::isPresent)
                        .map(Optional::get).mapToLong(List::size).sum();
        if (verbose) {
            logger.info("Contains simulation modules: {}", simCount);
        }

        if (simCount != 1) {
            logger.error("Only a single simulation module must be present");
            return null;
        }

        Optional<ASimulationSpecificationCompilationUnit> simulationModuleOpt =
                documentList.stream().map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class)).filter(Optional::isPresent)
                        .map(Optional::get).flatMap(List::stream).findFirst();

        if (simulationModuleOpt.isPresent()) {

            MableAstFactory factory = new MableAstFactory();

            TypeResolver typeResolver = new TypeResolver(factory, reporter);
            TypeComparator comparator = new TypeComparator();

            RootEnvironment rootEnv = new RootEnvironment();


            ASimulationSpecificationCompilationUnit simulationModule = simulationModuleOpt.get();

            List<String> importedModuleNames = simulationModule.getImports().stream().map(LexIdentifier::toString).collect(Collectors.toList());

            logger.info("\tImports {}", ("[ " + String.join(" , ", importedModuleNames) + " ]"));


            // Add instance mapping statements to the unitrelationship
            handleInstanceMappingStatements(simulationModule);

            //load plugins
            PluginEnvironment pluginEnvironment = loadExpansionPlugins(typeResolver, rootEnv, contextFile, framework, importedModuleNames);

            try {

                ASimulationSpecificationCompilationUnit unfoldedSimulationModule =
                        expandExternals(simulationModule, reporter, typeResolver, comparator, pluginEnvironment);

                //expansion complete
                if (reporter.getErrorCount() > 0) {
                    //we should probably stop now
                    throw new InternalException("errors after expansion");
                }


                logger.trace(ppPrint(unfoldedSimulationModule.toString()));
                ARootDocument processedDoc =
                        new ARootDocument(Stream.concat(importedModules.stream(), Stream.of(unfoldedSimulationModule)).collect(Collectors.toList()));


                String printedSpec = null;
                try {
                    printedSpec = PrettyPrinter.printLineNumbers(processedDoc);
                } catch (AnalysisException e) {
                    printedSpec = processedDoc + "";
                }


                ARootDocument specToCheck = null;
                try {
                    specToCheck = parse(CharStreams.fromString(PrettyPrinter.print(processedDoc)));
                } catch (AnalysisException e) {
                    specToCheck = processedDoc;
                }


                if (typeCheck(specToCheck, reporter)) {
                    if (verify(specToCheck, reporter)) {
                        return specToCheck;
                    }
                }


                throw new RuntimeException("No valid spec prod.\n" + printedSpec);

            } finally {
                if (verbose) {
                    PrintWriter writer = new PrintWriter(System.err);
                    if (reporter.getErrorCount() > 0) {
                        reporter.printErrors(writer);
                    }
                    if (reporter.getWarningCount() > 0) {
                        reporter.printWarnings(writer);
                    }
                    writer.flush();
                }
            }

        } else {
            throw new InternalException("No Specification module found");
        }
    }

    /**
     * This adds instance mapping statements (@map a-> "a") to the unitrelationship.
     *
     * @param simulationModule
     */
    private void handleInstanceMappingStatements(ASimulationSpecificationCompilationUnit simulationModule) {
        if (simulationModule.getBody() instanceof ABlockStm) {
            Optional<List<AInstanceMappingStm>> instanceMappings = NodeCollector.collect(simulationModule.getBody(), AInstanceMappingStm.class);
            if (instanceMappings.isPresent()) {
                instanceMappings.get().forEach(x -> ((FmiSimulationEnvironment) this.simulationEnvironment)
                        .setLexNameToInstanceNameMapping(x.getIdentifier().getText(), x.getName()));
            }
        }
    }

    private String ppPrint(String string) {

        StringBuilder sb = new StringBuilder();


        int indentation = 0;
        for (char c : string.toCharArray()) {
            if (c == '{') {
                indentation++;
            } else if (c == '}') {
                indentation--;
            }

            sb.append(c);
            if (c == '\n') {
                sb.append(IntStream.range(0, indentation).mapToObj(t -> "").collect(Collectors.joining("\t")));
            }
        }


        return sb.toString();
    }

    public ARootDocument generate(List<File> sourceFiles, InputStream contextFile) throws IOException {
        IErrorReporter reporter = new ErrorReporter();

        List<ARootDocument> documentList = parse(sourceFiles);
        return generateFromDocuments(documentList, contextFile);

    }

    private boolean verify(final ARootDocument doc, final IErrorReporter reporter) {

        Collection<IMaestroVerifier> verifiers = PluginFactory.getPlugins(IMaestroVerifier.class, framework);

        verifiers.forEach(p -> logger.debug("Loaded verifiers: {} - {}", p.getName(), p.getVersion()));

        return verifiers.stream().allMatch(verifier -> {
            logger.info("Verifying with {} - {}", verifier.getName(), verifier.getVersion());
            return verifier.verify(doc, reporter);
        });
    }

    private boolean typeCheck(final ARootDocument doc, final IErrorReporter reporter) {
        //TODO: implement type check
        logger.warn("Type checker not yet implemented");
        try {
            doc.apply(new TypeChecker(reporter));
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return reporter.getErrorCount() == 0;
    }
}