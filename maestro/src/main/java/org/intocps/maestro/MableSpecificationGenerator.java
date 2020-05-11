package org.intocps.maestro;

import org.antlr.v4.runtime.*;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.parser.MablParser;
import org.intocps.maestro.parser.ParseTree2AstConverter;
import org.intocps.maestro.plugin.*;
import org.intocps.maestro.typechecker.PluginEnvironment;
import org.intocps.maestro.typechecker.RootEnvironment;
import org.intocps.maestro.typechecker.TypeComparator;
import org.intocps.maestro.typechecker.TypeResolver;
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
import java.util.stream.Stream;

public class MableSpecificationGenerator {

    final static Logger logger = LoggerFactory.getLogger(MableSpecificationGenerator.class);
    final boolean verbose;
    private final Framework framework;

    public MableSpecificationGenerator(Framework framework, boolean verbose) {
        this.framework = framework;
        this.verbose = verbose;
    }

    private static PluginEnvironment loadUnfoldPlugins(TypeResolver typeResolver, RootEnvironment rootEnv, File contextFile,
            Framework framework) throws IOException {
        return loadUnfoldPlugins(typeResolver, rootEnv, PluginFactory.parsePluginConfiguration(contextFile), framework);
    }

    private static PluginEnvironment loadUnfoldPlugins(TypeResolver typeResolver, RootEnvironment rootEnv, InputStream contextFile,
            Framework framework) throws IOException {
        return loadUnfoldPlugins(typeResolver, rootEnv, PluginFactory.parsePluginConfiguration(contextFile), framework);
    }


    private static PluginEnvironment loadUnfoldPlugins(TypeResolver typeResolver, RootEnvironment rootEnv, Map<String, String> rawPluginJsonContext,
            Framework framework) {
        Collection<IMaestroUnfoldPlugin> plugins = PluginFactory.getPlugins(IMaestroUnfoldPlugin.class, framework);

        plugins.forEach(p -> logger.info("Loaded plugin: {} - {}", p.getName(), p.getVersion()));


        logger.debug("The following plugins will be used for unfolding: {}",
                plugins.stream().map(p -> p.getName() + "-" + p.getVersion()).collect(Collectors.joining(",", "[", "]")));


        return new PluginEnvironment(rootEnv, plugins.stream()
                .collect(Collectors.toMap(p -> p, p -> p.getDeclaredUnfoldFunctions().stream().collect(Collectors.toMap(Function.identity(), f -> {
                    try {
                        return (AFunctionType) typeResolver.resolve(f, rootEnv);
                    } catch (AnalysisException e) {
                        e.printStackTrace();
                        return null;
                    }
                })))), rawPluginJsonContext);
    }

    private static List<ARootDocument> parse(List<File> sourceFiles) throws IOException {

        List<ARootDocument> documentList = new Vector<>();

        for (File file : sourceFiles) {
            if (!file.exists()) {
                logger.warn("Unable to parse file. File does not exist: {}", file);
                continue;
            }
            logger.info("Parting file: {}", file);


            MablLexer l = new MablLexer(CharStreams.fromPath(Paths.get(file.toURI())));
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
            documentList.add(root);


        }
        return documentList;
    }

    private static ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit inputSimulationModule,
            IErrorReporter reporter, TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env) {

        ASimulationSpecificationCompilationUnit simulationModule = inputSimulationModule.clone();

        return expandExternals(simulationModule, reporter, typeResolver, comparator, env, 0);
    }

    private static ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit simulationModule,
            IErrorReporter reporter, TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env, int depth) {

        Map<IMaestroUnfoldPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins = env.getTypesPlugins();


        List<AExternalStm> aExternalStms = NodeCollector.collect(simulationModule, AExternalStm.class).orElse(new Vector<>());


        Map<AExternalStm, Optional<PType>> externalTypeMap = aExternalStms.stream().collect(Collectors.toMap(Function.identity(), n -> {
            try {
                PType type = typeResolver.resolve(n.getCall(), env);
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
                .forEach(map -> reporter.report(0, String.format("Unknown external: '%s'", map.getKey().getCall().getRoot().toString()), null));

        if (externalTypeMap.entrySet().stream().anyMatch(map -> !map.getValue().isPresent())) {
            throw new RuntimeException("Unknown externals present cannot proceed");
        }


        if (aExternalStms.isEmpty()) {
            return simulationModule;
        } else if (depth > 3) {
            throw new RuntimeException("Recursive external expansion larger than " + depth);
        }


        logger.info("\tExternals {}",
                NodeCollector.collect(simulationModule, AExternalStm.class).orElse(new Vector<>()).stream().map(m -> m.getCall().getRoot().toString())
                        .collect(Collectors.joining(" , ", "[ ", " ]")));

        externalTypeMap.forEach((node, type) -> {

            if (type.isPresent()) {
                logger.debug("Unfolding node: {}", node);

                Predicate<Map.Entry<AFunctionDeclaration, AFunctionType>> typeCompatible = (fmap) -> fmap.getKey().getName().getText()
                        .equals(node.getCall().getRoot().toString()) && comparator.compatible(fmap.getValue(), type.get());

                Optional<Map.Entry<IMaestroUnfoldPlugin, Map<AFunctionDeclaration, AFunctionType>>> pluginMatch = plugins.entrySet().stream()
                        .filter(map -> map.getValue().entrySet().stream().anyMatch(typeCompatible)).findFirst();

                if (pluginMatch.isPresent()) {
                    pluginMatch.ifPresent(map -> {
                        map.getValue().entrySet().stream().filter(typeCompatible).findFirst().ifPresent(fmap -> {
                            logger.debug("Replacing external '{}' with unfoled statement", node.getCall().getRoot().toString());

                            PStm unfoled = null;
                            IMaestroUnfoldPlugin plugin = map.getKey();
                            try {
                                if (plugin.requireConfig()) {
                                    try {
                                        IPluginConfiguration config = env.getConfiguration(plugin);
                                        unfoled = plugin.unfold(fmap.getKey(), node.getCall().getArgs(), config, reporter);
                                    } catch (PluginEnvironment.PluginConfigurationNotFoundException e) {
                                        logger.error("Could not obtain configuration for plugin '{}' at {}: {}", plugin.getName(),
                                                node.getCall().getRoot().toString(), e.getMessage());
                                    }

                                } else {
                                    unfoled = plugin.unfold(fmap.getKey(), node.getCall().getArgs(), null, reporter);
                                }
                            } catch (UnfoldException e) {
                                logger.error("Internal error in pluginn '{}' at {}. Message: {}", plugin.getName(),
                                        node.getCall().getRoot().toString(), e.getMessage());
                            }
                            if (unfoled == null) {
                                reporter.report(999,
                                        String.format("Unfold failure in plugin %s for %s", plugin.getName(), node.getCall().getRoot() + ""), null);
                            } else {

                                node.parent().replaceChild(node, unfoled);
                            }
                        });
                    });
                }
            }
        });

        return expandExternals(simulationModule, reporter, typeResolver, comparator, env, depth + 1);
    }

    public ARootDocument generate(List<File> sourceFiles, InputStream contextFile) throws IOException {
        IErrorReporter reporter = new ErrorReporter();

        List<ARootDocument> documentList = parse(sourceFiles);

        List<AImportedModuleCompilationUnit> importedModules = documentList.stream()
                .map(d -> NodeCollector.collect(d, AImportedModuleCompilationUnit.class)).filter(Optional::isPresent).map(Optional::get)
                .flatMap(List::stream).filter(l -> !l.getFunctions().isEmpty()).collect(Collectors.toList());

        if (verbose) {
            logger.info("Module definitions: {}",
                    importedModules.stream().map(l -> l.getName().toString()).collect(Collectors.joining(" , ", "[ ", " ]")));
        }

        long simCount = documentList.stream().map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class))
                .filter(Optional::isPresent).map(Optional::get).mapToLong(List::size).sum();
        if (verbose) {
            logger.info("Contains simulation modules: {}", simCount);
        }

        if (simCount != 1) {
            logger.error("Only a single simulation module must be present");
            return null;
        }

        Optional<ASimulationSpecificationCompilationUnit> simulationModuleOpt = documentList.stream()
                .map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class)).filter(Optional::isPresent).map(Optional::get)
                .flatMap(List::stream).findFirst();

        if (simulationModuleOpt.isPresent()) {

            MableAstFactory factory = new MableAstFactory();

            TypeResolver typeResolver = new TypeResolver(factory, reporter);
            TypeComparator comparator = new TypeComparator();

            RootEnvironment rootEnv = new RootEnvironment();


            ASimulationSpecificationCompilationUnit simulationModule = simulationModuleOpt.get();

            logger.info("\tImports {}",
                    simulationModule.getImports().stream().map(LexIdentifier::toString).collect(Collectors.joining(" , ", "[ ", " ]")));

            //load plugins
            PluginEnvironment pluginEnvironment = loadUnfoldPlugins(typeResolver, rootEnv, contextFile, framework);

            try {

                ASimulationSpecificationCompilationUnit unfoldedSimulationModule = expandExternals(simulationModule, reporter, typeResolver,
                        comparator, pluginEnvironment);

                //expansion complete
                if (reporter.getErrorCount() > 0) {
                    //we should probably stop now
                    throw new InternalException("errors after expansion");
                }

                logger.info(unfoldedSimulationModule.toString());

                ARootDocument processedDoc = new ARootDocument(
                        Stream.concat(importedModules.stream(), Stream.of(unfoldedSimulationModule)).collect(Collectors.toList()));

                if (typeCheck(processedDoc, reporter)) {
                    if (verify(processedDoc, reporter)) {
                        return processedDoc;
                    }
                }


                throw new RuntimeException("No valid spec produced");

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

    private boolean verify(final ARootDocument doc, final IErrorReporter reporter) {

        Collection<IMaestroVerifier> verifiers = PluginFactory.getPlugins(IMaestroVerifier.class, framework);

        verifiers.forEach(p -> logger.info("Loaded verifiers: {} - {}", p.getName(), p.getVersion()));

        return verifiers.stream().allMatch(verifier -> {
            logger.info("Verifying with {} - {}", verifier.getName(), verifier.getVersion());
            return verifier.verify(doc, reporter);
        });
    }

    private boolean typeCheck(final ARootDocument doc, final IErrorReporter reporter) {
        //TODO: implement type check
        logger.warn("Type checker not yet implemented");
        return true;
    }
}
