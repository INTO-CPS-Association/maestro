package org.intocps.maestro;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("coe", options);
    }

    private static String getVersion() {
        try {
            Properties prop = new Properties();
            InputStream coeProp = Main.class.getResourceAsStream("/coe.properties");
            prop.load(coeProp);
            return prop.getProperty("version");
        } catch (Exception e) {
            return "";
        }
    }

    public static void main(String[] args) throws IOException {


        Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();
        Option verboseOpt = Option.builder("v").longOpt("verbose").desc("Verbose").build();
        Option versionOpt = Option.builder("version").longOpt("version").desc("Version").build();
        Option mablOpt = Option.builder("m").longOpt("mabl").desc("Path to Mabl files").hasArg().numberOfArgs(1000).valueSeparator(' ')
                .argName("path").build();

        Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(mablOpt);
        options.addOption(verboseOpt);
        options.addOption(versionOpt);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            System.err.println("Parsing failed. Reason: " + e1.getMessage());
            showHelp(options);
            return;
        }

        if (cmd.hasOption(helpOpt.getOpt())) {
            showHelp(options);
            return;
        }

        if (cmd.hasOption(versionOpt.getOpt())) {
            System.out.println(getVersion());
            return;
        }

        boolean verbose = cmd.hasOption(verboseOpt.getOpt());

        List<File> sourceFiles = Arrays.stream(cmd.getOptionValues(mablOpt.getOpt())).map(File::new).collect(Collectors.toList());
        new MableSpecificationGenerator(verbose).generate(sourceFiles);
        //        TypeCheckerErrors reporter = new TypeCheckerErrors();
        //
        //
        //        List<ARootDocument> documentList = parse(Arrays.stream(cmd.getOptionValues(mablOpt.getOpt())).map(File::new).collect(Collectors.toList()));
        //
        //        if (verbose) {
        //            logger.info("Module definitions: {}",
        //                    documentList.stream().map(d -> NodeCollector.collect(d, AImportedModuleCompilationUnit.class)).filter(Optional::isPresent)
        //                            .map(Optional::get).flatMap(List::stream).filter(l -> !l.getFunctions().isEmpty()).map(l -> l.getName().toString())
        //                            .collect(Collectors.joining(" , ", "[ ", " ]")));
        //        }
        //
        //        long simCount = documentList.stream().map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class))
        //                .filter(Optional::isPresent).map(Optional::get).flatMap(List::stream).count();
        //        if (verbose) {
        //            logger.info("Contains simulation modules: {}", simCount);
        //        }
        //
        //        if (simCount != 1) {
        //            logger.error("Only a single simulation module must be present");
        //            return;
        //        }
        //
        //        Optional<ASimulationSpecificationCompilationUnit> simulationModuleOpt = documentList.stream()
        //                .map(d -> NodeCollector.collect(d, ASimulationSpecificationCompilationUnit.class)).filter(Optional::isPresent).map(Optional::get)
        //                .flatMap(List::stream).findFirst();
        //
        //        if (simulationModuleOpt.isPresent()) {
        //
        //            MableAstFactory factory = new MableAstFactory();
        //
        //            TypeResolver typeResolver = new TypeResolver(factory, reporter);
        //            TypeComparator comparator = new TypeComparator();
        //
        //            RootEnvironment rootEnv = new RootEnvironment();
        //
        //
        //            ASimulationSpecificationCompilationUnit simulationModule = simulationModuleOpt.get();
        //
        //            logger.info("\tImports {}",
        //                    simulationModule.getImports().stream().map(LexIdentifier::toString).collect(Collectors.joining(" , ", "[ ", " ]")));
        //
        //
        //            //load plugins
        //            PluginEnvironment pluginEnvironment = loadPlugins(typeResolver, rootEnv);
        //
        //
        //            try {
        //
        //
        //                ASimulationSpecificationCompilationUnit unfoldedSimulationModule = expandExternals(simulationModule, reporter, typeResolver,
        //                        comparator, pluginEnvironment);
        //
        //                //expansion complete
        //                if (reporter.getErrorCount() > 0) {
        //                    //we should probably stop now
        //                    throw new RuntimeException("errors after expansion");
        //                }
        //
        //                logger.info(unfoldedSimulationModule.toString());
        //
        //            } finally {
        //                if (verbose) {
        //                    PrintWriter writer = new PrintWriter(System.err);
        //                    if (reporter.getErrorCount() > 0) {
        //                        reporter.printErrors(writer);
        //                    }
        //                    if (reporter.getWarningCount() > 0) {
        //                        reporter.printWarnings(writer);
        //                    }
        //                    writer.flush();
        //                }
        //
        //                System.exit(reporter.getErrorCount() > 0 ? 1 : 0);
        //            }
        //        } else {
        //            logger.info("No Specification module found");
        //            System.exit(1);
        //        }
    }

    //    private static PluginEnvironment loadPlugins(TypeResolver typeResolver, RootEnvironment rootEnv) {
    //        Collection<IMaestroPlugin> plugins = PluginFactory.getPlugins();
    //
    //        plugins.forEach(p -> logger.info("Loaded plugin: {} - {}", p.getName(), p.getVersion()));
    //
    //
    //        logger.debug("The following plugins will be used for unfolding: {}",
    //                plugins.stream().map(p -> p.getName() + "-" + p.getVersion()).collect(Collectors.joining(",", "[", "]")));
    //
    //
    //        return new PluginEnvironment(rootEnv, plugins.stream()
    //                .collect(Collectors.toMap(p -> p, p -> p.getDeclaredUnfoldFunctions().stream().collect(Collectors.toMap(Function.identity(), f -> {
    //                    try {
    //                        return (AFunctionType) typeResolver.resolve(f, rootEnv);
    //                    } catch (AnalysisException e) {
    //                        e.printStackTrace();
    //                        return null;
    //                    }
    //                })))));
    //    }
    //
    //    private static List<ARootDocument> parse(List<File> sourceFiles) throws IOException {
    //
    //        List<ARootDocument> documentList = new Vector<>();
    //
    //        for (File file : sourceFiles) {
    //            if (!file.exists()) {
    //                logger.warn("Unable to parse file. File does not exist: {}", file);
    //                continue;
    //            }
    //            logger.info("Parting file: {}", file);
    //
    //
    //            MablLexer l = new MablLexer(CharStreams.fromPath(Paths.get(file.toURI())));
    //            MablParser p = new MablParser(new CommonTokenStream(l));
    //            p.addErrorListener(new BaseErrorListener() {
    //                @Override
    //                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg,
    //                        RecognitionException e) {
    //                    throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
    //                }
    //            });
    //            MablParser.CompilationUnitContext unit = p.compilationUnit();
    //
    //            ARootDocument root = (ARootDocument) new ParseTree2AstConverter().visit(unit);
    //            documentList.add(root);
    //
    //
    //        }
    //        return documentList;
    //    }
    //
    //    private static ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit inputSimulationModule,
    //            TypeCheckerErrors reporter, TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env) {
    //        return expandExternals(inputSimulationModule, reporter, typeResolver, comparator, env, 0);
    //    }
    //
    //    private static ASimulationSpecificationCompilationUnit expandExternals(ASimulationSpecificationCompilationUnit inputSimulationModule,
    //            TypeCheckerErrors reporter, TypeResolver typeResolver, TypeComparator comparator, PluginEnvironment env, int depth) {
    //
    //        Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins = env.getTypesPlugins();
    //
    //        ASimulationSpecificationCompilationUnit simulationModule = inputSimulationModule.clone();
    //
    //        List<AExternalStm> aExternalStms = NodeCollector.collect(simulationModule, AExternalStm.class).orElse(new Vector<>());
    //
    //
    //        Map<AExternalStm, Optional<PType>> externalTypeMap = aExternalStms.stream().collect(Collectors.toMap(Function.identity(), n -> {
    //            try {
    //                PType type = typeResolver.resolve(n.getCall(), env);
    //                if (type != null) {
    //                    return Optional.of(type);
    //                } else {
    //                    return Optional.empty();
    //                }
    //            } catch (AnalysisException e) {
    //                e.printStackTrace();
    //                return Optional.empty();
    //            }
    //        }));
    //
    //        externalTypeMap.entrySet().stream().filter(map -> !map.getValue().isPresent())
    //                .forEach(map -> reporter.report(0, String.format("Unknown external: '%s'", map.getKey().getCall().getIdentifier().toString()), null));
    //
    //        if (externalTypeMap.entrySet().stream().anyMatch(map -> !map.getValue().isPresent())) {
    //            throw new RuntimeException("Unknown externals present cannot proceed");
    //        }
    //
    //
    //        if (aExternalStms.isEmpty()) {
    //            return simulationModule;
    //        } else if (depth > 3) {
    //            throw new RuntimeException("Recursive external expansion larger than " + depth);
    //        }
    //
    //
    //        logger.info("\tExternals {}", NodeCollector.collect(simulationModule, AExternalStm.class).orElse(new Vector<>()).stream()
    //                .map(m -> m.getCall().getIdentifier().toString()).collect(Collectors.joining(" , ", "[ ", " ]")));
    //
    //        externalTypeMap.forEach((node, type) -> {
    //
    //            if (type.isPresent()) {
    //                logger.debug("Unfolding node: {}", node);
    //
    //                Predicate<Map.Entry<AFunctionDeclaration, AFunctionType>> typeCompatible = (fmap) -> fmap.getKey().getName().getText()
    //                        .equals(node.getCall().getIdentifier().toString()) && comparator.compatible(fmap.getValue(), type.get());
    //
    //                Optional<Map.Entry<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>>> pluginMatch = plugins.entrySet().stream()
    //                        .filter(map -> map.getValue().entrySet().stream().anyMatch(typeCompatible)).findFirst();
    //
    //                if (pluginMatch.isPresent()) {
    //                    pluginMatch.ifPresent(map -> {
    //                        map.getValue().entrySet().stream().filter(typeCompatible).findFirst().ifPresent(fmap -> {
    //                            logger.debug("Replacing external '{}' with unfoled statement", node.getCall().getIdentifier().toString());
    //                            PStm unfoled = map.getKey().unfold(fmap.getKey(), node.getCall().getArgs(), null);
    //                            if (unfoled == null) {
    //                                reporter.report(999, String.format("Unfold failure in plugin %s for %s", map.getKey().getName(),
    //                                        node.getCall().getIdentifier() + ""), null);
    //                            } else {
    //
    //                                node.parent().replaceChild(node, unfoled);
    //                            }
    //                        });
    //                    });
    //                }
    //            }
    //        });
    //
    //        return expandExternals(simulationModule, reporter, typeResolver, comparator, env, depth + 1);
    //    }
}
