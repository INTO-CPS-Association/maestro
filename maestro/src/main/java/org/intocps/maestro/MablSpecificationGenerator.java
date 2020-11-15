package org.intocps.maestro;

import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.collections.map.HashedMap;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.plugin.*;
import org.intocps.maestro.typechecker.TypeChecker;
import org.intocps.maestro.typechecker.TypeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.parser.MablParserUtil.parse;

public class MablSpecificationGenerator {

    //This values is multiplied with the count of plugin function declarations.
    final static Logger logger = LoggerFactory.getLogger(MablSpecificationGenerator.class);
    final boolean verbose;
    final ISimulationEnvironment simulationEnvironment;
    final IntermediateSpecWriter intermediateSpecWriter;
    private final Framework framework;
    private final MaestroConfiguration configuration;
    private final File specificationFolder;

    public MablSpecificationGenerator(Framework framework, boolean verbose, ISimulationEnvironment simulationEnvironment,
            MaestroConfiguration configuration, File specificationFolder, IntermediateSpecWriter intermediateSpecWriter) {
        this.framework = framework;
        this.verbose = verbose;
        this.simulationEnvironment = simulationEnvironment;
        this.configuration = configuration;
        this.specificationFolder = specificationFolder;
        this.intermediateSpecWriter = intermediateSpecWriter;
    }

    public MablSpecificationGenerator(Framework framework, boolean verbose, ISimulationEnvironment simulationEnvironment, File specificationFolder,
            IntermediateSpecWriter intermediateSpecWriter) {
        this(framework, verbose, simulationEnvironment, new MaestroConfiguration(), specificationFolder, intermediateSpecWriter);
    }


    private static Collection<IMaestroExpansionPlugin> loadExpansionPlugins(Framework framework, List<String> importModules) {
        Collection<IMaestroExpansionPlugin> plugins = PluginFactory.getPlugins(IMaestroExpansionPlugin.class, framework);

        plugins.forEach(p -> logger.debug("Located plugins: {} - {}", p.getName(), p.getVersion()));

        Collection<IMaestroExpansionPlugin> pluginsToUnfold =
                plugins.stream().filter(plugin -> importModules.contains(plugin.getName())).collect(Collectors.toList());

        logger.debug("The following plugins will be used for unfolding: {}",
                pluginsToUnfold.stream().map(p -> p.getName() + "-" + p.getVersion()).collect(Collectors.joining(",", "[", "]")));

        logger.debug("Plugins declared functions: {}", pluginsToUnfold.stream().map(p -> p.getName() + "-" + p.getVersion() +
                p.getDeclaredUnfoldFunctions().stream().map(AFunctionDeclaration::toString).collect(Collectors.joining(",\n\t", "\n\t", "")))
                .collect(Collectors.joining(",\n", "\n[\n", "\n]")));

        return pluginsToUnfold;
    }


    public IMaestroConfiguration getConfiguration() {
        return this.configuration;
    }

    private ASimulationSpecificationCompilationUnit expandExternals(List<ARootDocument> importedDocumentList, ARootDocument doc,
            IErrorReporter reporter, Collection<IMaestroExpansionPlugin> env) throws ExpandException {

        ARootDocument docClone = doc.clone();

        intermediateSpecWriter.write(docClone);
        return expandExternals(importedDocumentList, docClone, reporter, env, 1);
    }

    private ASimulationSpecificationCompilationUnit expandExternals(List<ARootDocument> importedDocumentList, ARootDocument simulationModule,
            IErrorReporter reporter, Collection<IMaestroExpansionPlugin> env, int depth) throws ExpandException {

        List<AFunctionDeclaration> globalFunctions =
                env.stream().flatMap(plugin -> plugin.getDeclaredUnfoldFunctions().stream()).collect(Collectors.toList());
        Map.Entry<Boolean, Map<INode, PType>> tcRes =
                typeCheck(Stream.concat(Stream.of(simulationModule), importedDocumentList.stream()).collect(Collectors.toList()), globalFunctions,
                        reporter);
        if (!tcRes.getKey()) {
            throw new RuntimeException("Expansion not possible type errors");
        }


        //        Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins = env.getTypesPlugins();


        //TODO: It is not necessary to check if it is expand as all CallExps are expand.
        // CallExps to runtime modules are part of Dot Exp.

        TypeComparator typeComparator = new TypeComparator();
        List<ACallExp> aExternalStms =
                NodeCollector.collect(simulationModule, ACallExp.class).orElse(new Vector<>()).stream().filter(call -> call.getExpand() != null)
                        .collect(Collectors.toList());

        Map<ACallExp, Optional<AFunctionDeclaration>> replaceWith = aExternalStms.stream().collect(Collectors.toMap(Function.identity(), call -> {

            PType callType = tcRes.getValue().get(call);
            return globalFunctions.stream()
                    .filter(fun -> fun.getName().equals(call.getMethodName()) && typeComparator.compatible(tcRes.getValue().get(fun), callType))
                    .findFirst();

        }));

        if (replaceWith.values().stream().anyMatch(o -> !o.isPresent())) {
            throw new ExpandException("Unresolved external function: " +
                    replaceWith.entrySet().stream().filter(map -> !map.getValue().isPresent()).map(map -> map.getKey().getMethodName().getText())
                            .collect(Collectors.joining(",")));
        }


        for (Map.Entry<ACallExp, Optional<AFunctionDeclaration>> callReplacement : replaceWith.entrySet()) {
            ACallExp call = callReplacement.getKey();
            AFunctionDeclaration replacement = callReplacement.getValue().get();
            IMaestroExpansionPlugin replacementPlugin =
                    env.stream().filter(plugin -> plugin.getDeclaredUnfoldFunctions().contains(replacement)).findFirst().get();

            logger.debug("Replacing external '{}' with unfoled statement '{}' from plugin: {}", call.getMethodName().getText(),
                    replacement.getName().getText(), replacementPlugin.getName() + " " + replacementPlugin.getVersion());

            replaceCall(call, replacement, replacementPlugin, reporter);
            intermediateSpecWriter.write(simulationModule);
        }


        logger.debug("Externals {}",
                NodeCollector.collect(simulationModule, ACallExp.class).orElse(new Vector<>()).stream().map(m -> m.getMethodName().toString())
                        .collect(Collectors.joining(" , ", "[ ", " ]")));

        //
        //        AtomicInteger typeIndex = new AtomicInteger(0);
        //        externalTypeMap.forEach((node, type) -> {
        //
        //            if (type.isPresent()) {
        //                logger.debug("Unfolding node: {}", node);
        //
        //                Predicate<Map.Entry<AFunctionDeclaration, AFunctionType>> typeCompatible =
        //                        (fmap) -> fmap.getKey().getName().getText().equals(node.getMethodName().toString()) &&
        //                                fmap.getKey().getFormals().size() == node.getArgs().size() && comparator.compatible(fmap.getValue(), type.get());
        //
        //
        //                Optional<Map.Entry<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>>> pluginMatch =
        //                        plugins.entrySet().stream().filter(map -> map.getValue().entrySet().stream().anyMatch(typeCompatible)).findFirst();
        //
        //                if (pluginMatch.isPresent()) {
        //                    logger.trace("matched with {}- {}", pluginMatch.get().getKey().getName(),
        //                            pluginMatch.get().getValue().keySet().iterator().next());
        //                    pluginMatch.ifPresent(map -> {
        //                        map.getValue().entrySet().stream().filter(typeCompatible).findFirst().ifPresent(fmap -> {
        //                            logger.debug("Replacing external '{}' with unfoled statement", node.getMethodName().toString());
        //
        //                            List<PStm> unfoled = null;
        //                            AConfigStm configRightAbove = null;
        //                            IMaestroExpansionPlugin plugin = map.getKey();
        //                            try {
        //                                if (plugin.requireConfig()) {
        //                                    try {
        //                                        configRightAbove = findConfig(node);
        //
        //                                        if (plugin.requireConfig() && configRightAbove == null) {
        //                                            throw new ExpandException("Cannot expand no " + MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_CONFIG) +
        //                                                    " specified on line: " + (node.getMethodName().getSymbol().getLine() - 1));
        //                                        }
        //
        //                                        IPluginConfiguration config = env.getConfiguration(plugin, configRightAbove, specificationFolder);
        //                                        unfoled = plugin.expand(fmap.getKey(), node.getArgs(), config, simulationEnvironment, reporter);
        //                                    } catch (IOException e) {
        //                                        logger.error("Could not obtain configuration for plugin '{}' at {}: {}", plugin.getName(),
        //                                                node.getMethodName().toString(), e.getMessage());
        //                                    }
        //
        //                                } else {
        //                                    unfoled = plugin.expand(fmap.getKey(), node.getArgs(), null, simulationEnvironment, reporter);
        //                                }
        //                            } catch (ExpandException e) {
        //                                logger.error("Internal error in plug-in '{}' at {}. Message: {}", plugin.getName(), node.getMethodName().toString(),
        //                                        e.getMessage());
        //                            }
        //                            if (unfoled == null) {
        //                                reporter.report(999, String.format("Unfold failure in plugin %s for %s", plugin.getName(), node.getMethodName() + ""),
        //                                        null);
        //                            } else {
        //                                //replace the call and so rounding expression statement
        //
        //                                replaceExpandedCall(node, configRightAbove, unfoled);
        //                                //                                writeIntermediateSpec(depth, typeIndex.getAndAdd(1), simulationModule);
        //                                intermediateSpecWriter.write(simulationModule);
        //
        //                            }
        //                        });
        //                    });
        //                } else {
        //                    logger.error("No plugin found for: {}", node);
        //                }
        //            }
        //        });

        return expandExternals(importedDocumentList, simulationModule, reporter, env, depth + 1);
    }

    private void replaceCall(ACallExp callToBeReplaced, AFunctionDeclaration replacement, IMaestroExpansionPlugin replacementPlugin,
            IErrorReporter reporter) {
        List<PStm> unfoled = null;
        AConfigStm configRightAbove = null;
        try {
            if (replacementPlugin.requireConfig()) {
                try {
                    configRightAbove = findConfig(callToBeReplaced);

                    if (replacementPlugin.requireConfig() && configRightAbove == null) {
                        throw new ExpandException(
                                "Cannot expand no " + MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_CONFIG) + " specified on line: " +
                                        (callToBeReplaced.getMethodName().getSymbol().getLine() - 1));
                    }

                    IPluginConfiguration config = PluginUtil.getConfiguration(replacementPlugin, configRightAbove, specificationFolder);
                    unfoled = replacementPlugin.expand(replacement, callToBeReplaced.getArgs(), config, simulationEnvironment, reporter);
                } catch (IOException e) {
                    logger.error("Could not obtain configuration for plugin '{}' at {}: {}", replacementPlugin.getName(),
                            callToBeReplaced.getMethodName().toString(), e.getMessage());
                }

            } else {
                unfoled = replacementPlugin.expand(replacement, callToBeReplaced.getArgs(), null, simulationEnvironment, reporter);
            }
        } catch (ExpandException e) {
            logger.error("Internal error in plug-in '{}' at {}. Message: {}", replacementPlugin.getName(),
                    callToBeReplaced.getMethodName().toString(), e.getMessage());
        }
        if (unfoled == null) {
            reporter.report(999,
                    String.format("Unfold failure in plugin %s for %s", replacementPlugin.getName(), callToBeReplaced.getMethodName() + ""), null);
        } else {
            //replace the call and so rounding expression statement

            replaceExpandedCall(callToBeReplaced, configRightAbove, unfoled);
            //                                writeIntermediateSpec(depth, typeIndex.getAndAdd(1), simulationModule);


        }
    }


    private void replaceExpandedCall(ACallExp node, AConfigStm config, List<PStm> unfoled) {
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
            if (config != null) {
                newBlock.remove(config);
            }
            //set the new block body, move all children to this node
            block.setBody(newBlock);
        } else {
            node.parent().parent().replaceChild(node.parent(), new ABlockStm(unfoled));
        }
    }

    private AConfigStm findConfig(ACallExp node) {
        INode parentBlock = node.parent().parent();

        if (parentBlock instanceof ABlockStm) {
            ABlockStm block = (ABlockStm) parentBlock;

            int index = block.getBody().indexOf(node.parent());
            if (index > 0) {
                PStm configCondidate = block.getBody().get(index - 1);
                if (configCondidate instanceof AConfigStm) {
                    return (AConfigStm) configCondidate;
                }
            }
        }
        return null;
    }


    //    public ARootDocument generateFromStreams(List<CharStream> sourceStreams, InputStream contextFile) throws IOException {
    //        List<ARootDocument> documentList = parseStreams(sourceStreams);
    //        return generateFromDocuments(documentList, contextFile);
    //    }

    public ARootDocument generateFromDocuments(List<ARootDocument> documentList) throws IOException, ExpandException {
        IErrorReporter reporter = new ErrorReporter();

        List<AImportedModuleCompilationUnit> importedModules =
                documentList.stream().map(d -> NodeCollector.collect(d, AImportedModuleCompilationUnit.class)).filter(Optional::isPresent)
                        .map(Optional::get).flatMap(List::stream).filter(l -> !l.getModule().getFunctions().isEmpty()).collect(Collectors.toList());

        if (verbose) {
            logger.info("Module definitions: {}",
                    importedModules.stream().map(l -> l.getModule().getName().toString()).collect(Collectors.joining(" , ", "[ ", " ]")));
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

            ASimulationSpecificationCompilationUnit simulationModule = simulationModuleOpt.get();

            List<String> importedModuleNames = simulationModule.getImports().stream().map(LexIdentifier::toString).collect(Collectors.toList());

            logger.info("\tImports {}", ("[ " + String.join(" , ", importedModuleNames) + " ]"));


            // Add instance mapping statements to the unitrelationship
            handleInstanceMappingStatements(simulationModule);

            //load plugins
            Collection<IMaestroExpansionPlugin> plugins = loadExpansionPlugins(framework, importedModuleNames);

            try {

                ARootDocument simulationDoc = simulationModule.getAncestor(ARootDocument.class);
                List<ARootDocument> importedDocks = documentList.stream().filter(d -> !d.equals(simulationDoc)).collect(Collectors.toList());
                ASimulationSpecificationCompilationUnit unfoldedSimulationModule = expandExternals(importedDocks, simulationDoc, reporter, plugins);

                //expansion complete
                if (reporter.getErrorCount() > 0) {
                    //we should probably stop now
                    throw new InternalException("errors after expansion");
                }


                try {
                    logger.trace("Specification:\n{}", PrettyPrinter.print(unfoldedSimulationModule));
                } catch (AnalysisException e) {
                    logger.trace("Pretty printing failed: ", e);
                }
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
                    specToCheck = parse(CharStreams.fromString(PrettyPrinter.print(processedDoc)), reporter);
                } catch (AnalysisException | IllegalStateException e) {
                    specToCheck = processedDoc;
                }


                if (typeCheck(Stream.concat(Stream.of(specToCheck), importedDocks.stream()).collect(Collectors.toList()), new Vector<>(), reporter)
                        .getKey()) {
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
                instanceMappings.get().forEach(x -> ((Fmi2SimulationEnvironment) this.simulationEnvironment)
                        .setLexNameToInstanceNameMapping(x.getIdentifier().getText(), x.getName()));
            }
        }
    }


    private boolean verify(final ARootDocument doc, final IErrorReporter reporter) {

        Collection<IMaestroVerifier> verifiers = PluginFactory.getPlugins(IMaestroVerifier.class, framework);

        verifiers.forEach(p -> logger.debug("Loaded verifiers: {} - {}", p.getName(), p.getVersion()));

        return verifiers.stream().allMatch(verifier -> {
            logger.info("Verifying with {} - {}", verifier.getName(), verifier.getVersion());
            return verifier.verify(doc, reporter);
        });
    }

    private Map.Entry<Boolean, Map<INode, PType>> typeCheck(final List<ARootDocument> documentList, List<? extends PDeclaration> globalFunctions,
            final IErrorReporter reporter) {
        logger.debug("Type checking");
        try {
            TypeChecker typeChecker = new TypeChecker(reporter);
            boolean res = typeChecker.typeCheck(documentList, globalFunctions);
            return Map.entry(res, typeChecker.getCheckedTypes());
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return Map.entry(reporter.getErrorCount() == 0, new HashedMap());
    }


}