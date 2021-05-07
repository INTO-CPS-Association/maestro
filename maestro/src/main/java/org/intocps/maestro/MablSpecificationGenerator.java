package org.intocps.maestro;

import org.antlr.v4.runtime.CharStreams;
import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.NodeCollector;
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
import org.intocps.maestro.typechecker.TypeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    public static Collection<IMaestroExpansionPlugin> loadExpansionPlugins(Framework framework, List<String> importModules) {
        Collection<IMaestroExpansionPlugin> plugins = PluginFactory.getPlugins(IMaestroExpansionPlugin.class, framework);

        plugins.forEach(p -> logger.debug("Located plugins: {} - {}", p.getName(), p.getVersion()));

        Collection<IMaestroExpansionPlugin> pluginsToUnfold =
                plugins.stream().filter(plugin -> importModules.contains(plugin.getName())).collect(Collectors.toList());

        //load recursive plugin dependencies

        List<IMaestroExpansionPlugin> pluginsToUnfoldWithDependencies = Stream.concat(pluginsToUnfold.stream(),
                pluginsToUnfold.stream().map(p -> collectPluginDependencies(plugins, p, new Vector<>()).stream()).flatMap(Function.identity()))
                .distinct().collect(Collectors.toList());


        logger.debug("The following plugins will be used for unfolding: {}",
                pluginsToUnfoldWithDependencies.stream().map(p -> p.getName() + "-" + p.getVersion()).collect(Collectors.joining(",", "[", "]")));


        logger.debug("Plugins declared functions: {}", pluginsToUnfoldWithDependencies.stream().map(p -> p.getName() + "-" + p.getVersion() +
                p.getDeclaredImportUnit().getModule().getFunctions().stream().map(AFunctionDeclaration::toString)
                        .collect(Collectors.joining("," + "\n\t", "\n\t", ""))).collect(Collectors.joining(",\n", "\n[\n", "\n]")));

        return pluginsToUnfoldWithDependencies;
    }

    static Collection<IMaestroExpansionPlugin> collectPluginDependencies(Collection<IMaestroExpansionPlugin> plugins, IMaestroExpansionPlugin plugin,
            List<String> checked) {

        checked.add(plugin.getName());
        List<IMaestroExpansionPlugin> requiredPlugins = new Vector<>();
        for (LexIdentifier importName : plugin.getDeclaredImportUnit().getImports()) {
            if (checked.contains(importName.getText())) {
                continue;
            }

            requiredPlugins.addAll(plugins.stream().filter(p -> p.getName().equals(importName.getText())).collect(Collectors.toList()));
        }

        return Stream.concat(requiredPlugins.stream(),
                requiredPlugins.stream().map(p -> collectPluginDependencies(plugins, p, checked).stream()).flatMap(Function.identity()))
                .collect(Collectors.toList());
    }

    public IMaestroConfiguration getConfiguration() {
        return this.configuration;
    }

    private ARootDocument expandExternals(List<ARootDocument> importedDocumentList, ARootDocument doc, IErrorReporter reporter,
            Collection<IMaestroExpansionPlugin> plugins) throws ExpandException {

        ARootDocument docClone = doc.clone();

        intermediateSpecWriter.write(docClone);
        return expandExternals(importedDocumentList, docClone, reporter, plugins, 1);
    }

    private ARootDocument expandExternals(List<ARootDocument> importedDocumentList, ARootDocument simulationModule, IErrorReporter reporter,
            Collection<IMaestroExpansionPlugin> plugins, int depth) throws ExpandException {

        //TODO do not add these functions as global but wrap in a module instead
        List<AFunctionDeclaration> globalFunctions = new Vector<>();

        List<AImportedModuleCompilationUnit> pluginUnits =
                plugins.stream().map(IMaestroExpansionPlugin::getDeclaredImportUnit).collect(Collectors.toList());
        //                plugins.stream().flatMap(plugin -> plugin.getDeclaredUnfoldFunctions().stream()).collect(Collectors.toList());
        List<ARootDocument> documentList = Stream.concat(Stream.of(simulationModule), importedDocumentList.stream()).collect(Collectors.toList());
        documentList.add(new ARootDocument(pluginUnits));

        //TODO add the real module of the plugin
        //        documentList.addAll(plugins.stream().map(plugin -> new ARootDocument(Collections.singletonList(
        //                new AImportedModuleCompilationUnit(new AModuleDeclaration(new LexIdentifier(plugin.getName(), null), new Vector<>()),
        //                        new Vector<>())))).collect(Collectors.toList()));

        Map.Entry<Boolean, Map<INode, PType>> tcRes = Mabl.typeCheck(documentList, globalFunctions, reporter);
        if (!tcRes.getKey()) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            reporter.printErrors(printWriter);
            printWriter.close();
            throw new RuntimeException("Expansion not possible type errors: " + stringWriter);
        } else if (depth > configuration.maximumExpansionDepth) {
            throw new RuntimeException("Recursive external expansion larger than " + configuration.maximumExpansionDepth);
        }


        //        Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins = plugins.getTypesPlugins();


        //TODO: It is not necessary to check if it is expand as all CallExps are expand.
        // CallExps to runtime modules are part of Dot Exp.

        TypeComparator typeComparator = new TypeComparator();
        List<ACallExp> aExternalStms =
                NodeCollector.collect(simulationModule, ACallExp.class).orElse(new Vector<>()).stream().filter(call -> call.getExpand() != null)
                        .collect(Collectors.toList());

        if (aExternalStms.isEmpty()) {
            NodeCollector.collect(simulationModule, ASimulationSpecificationCompilationUnit.class).stream().flatMap(List::stream).findFirst()
                    .ifPresent(unit -> NodeCollector.collect(unit, INode.class).ifPresent(nodes -> {
                        List<LexIdentifier> requiredImports =
                                tcRes.getValue().entrySet().stream().filter(map -> nodes.contains(map.getKey())).map(Map.Entry::getValue)
                                        .filter(AModuleType.class::isInstance).map(AModuleType.class::cast).map(AModuleType::getName).distinct()
                                        .sorted(Comparator.comparing(LexIdentifier::getText)).collect(Collectors.toList());


                        unit.setImports(requiredImports);
                    }));
            return simulationModule;
        }

        logger.debug("Externals {}", aExternalStms.stream().map(m -> m.getMethodName().toString()).collect(Collectors.joining(" , ", "[ ", " ]")));

        Map<ACallExp, Optional<Map.Entry<AImportedModuleCompilationUnit, AFunctionDeclaration>>> replaceWith =
                aExternalStms.stream().collect(Collectors.toMap(Function.identity(), call -> {

                    PType callType = tcRes.getValue().get(call);
                    PType object = tcRes.getValue().get(call.getObject());

                    if (object instanceof AModuleType) {
                        String pluginName = ((AModuleType) object).getName().getText();


                        List<Map.Entry<AImportedModuleCompilationUnit, AFunctionDeclaration>> tmp1 =
                                pluginUnits.stream().filter(m -> m.getModule().getName().getText().equals(pluginName)).flatMap(
                                        m -> m.getModule().getFunctions().stream().filter(fun -> fun.getName().equals(call.getMethodName()))
                                                .map(f -> Map.entry(m, f))).collect(Collectors.toList());

                        return tmp1.stream().filter(fun -> typeComparator.compatible(tcRes.getValue().get(fun.getValue()), callType)).findFirst();
                    }
                    return Optional.empty();

                }));

        if (replaceWith.values().stream().anyMatch(Optional::isEmpty)) {
            throw new ExpandException("Unresolved external function: " +
                    replaceWith.entrySet().stream().filter(map -> map.getValue().isEmpty()).map(map -> map.getKey().getMethodName().getText())
                            .collect(Collectors.joining(",")));
        }


        for (Map.Entry<ACallExp, Optional<Map.Entry<AImportedModuleCompilationUnit, AFunctionDeclaration>>> callReplacement : replaceWith
                .entrySet()) {
            ACallExp call = callReplacement.getKey();
            AFunctionDeclaration replacement = callReplacement.getValue().get().getValue();
            IMaestroExpansionPlugin replacementPlugin =
                    plugins.stream().filter(plugin -> plugin.getDeclaredImportUnit().getModule().getFunctions().contains(replacement)).findFirst()
                            .get();

            logger.debug("Replacing external '{}' with unfoled statement '{}' from plugin: {}", call.getMethodName().getText(),
                    replacement.getName().getText(), replacementPlugin.getName() + " " + replacementPlugin.getVersion());

            replaceCall(call, replacement, replacementPlugin, reporter);
            intermediateSpecWriter.write(simulationModule);
        }

        //update simulation module unit with required imports
        NodeCollector.collect(simulationModule, ASimulationSpecificationCompilationUnit.class).stream().flatMap(List::stream).findFirst()
                .ifPresent(unit -> {

                    Stream<? extends LexIdentifier> imports =
                            replaceWith.values().stream().filter(Optional::isPresent).map(Optional::get).map(p -> p.getKey().getImports().stream())
                                    .flatMap(Function.identity());
                    unit.setImports(Stream.concat(unit.getImports().stream(), imports).sorted(Comparator.comparing(LexIdentifier::getText))
                            .collect(Collectors.toList()));
                });

        return expandExternals(importedDocumentList, simulationModule, reporter, plugins, depth + 1);
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
        if (node.parent().parent() instanceof SBlockStm) {

            //construct a new block body replacing the original node with the new statements
            SBlockStm block = (SBlockStm) node.parent().parent();
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
            node.parent().parent().replaceChild(node.parent(), new ABasicBlockStm(unfoled));
        }
    }

    private AConfigStm findConfig(ACallExp node) {
        INode parentBlock = node.parent().parent();

        if (parentBlock instanceof SBlockStm) {
            SBlockStm block = (SBlockStm) parentBlock;

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
                ARootDocument processedDoc = expandExternals(importedDocks, simulationDoc, reporter, plugins);

                //expansion complete
                if (reporter.getErrorCount() > 0) {
                    //we should probably stop now
                    throw new InternalException("errors after expansion");
                }


                try {
                    logger.trace("Specification:\n{}", PrettyPrinter.print(processedDoc));
                } catch (AnalysisException e) {
                    logger.trace("Pretty printing failed: ", e);
                }

                ARootDocument specToCheck;
                try {
                    specToCheck = parse(CharStreams.fromString(PrettyPrinter.print(processedDoc)), reporter);
                } catch (AnalysisException | IllegalStateException e) {
                    specToCheck = processedDoc;
                }

                return specToCheck;

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
        if (simulationModule.getBody() instanceof SBlockStm) {
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


}