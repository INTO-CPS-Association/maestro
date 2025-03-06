package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.StringAnnotationProcessor;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.values.datawriter.WebSocketDataWriter;
import org.intocps.maestro.optimization.UnusedDeclarationOptimizer;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.plugin.IMaestroVerifier;
import org.intocps.maestro.plugin.PluginFactory;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.intocps.maestro.template.TemplateGenerator;
import org.intocps.maestro.typechecker.TypeChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mabl {

    static class RuntimeData {
        private Map<String, Object> runtimeEnvironmentVariables;
        private Map<String, List<String>> outputVariables;
        private Double liveStreamInterval;

        public Integer getWebsocketPort() {
            return websocketPort;
        }

        private Integer websocketPort;

        public Double getLiveStreamInterval() {
            return liveStreamInterval;
        }

        public Map<String, List<String>> getLivestreamVariables() {
            return livestreamVariables;
        }

        public Map<String, List<String>> getOutputVariables() {
            return outputVariables;
        }

        public Map<String, Object> getRuntimeEnvironmentVariables() {
            return runtimeEnvironmentVariables;
        }

        private Map<String, List<String>> livestreamVariables;
    }

    public static final String MAIN_SPEC_DEFAULT_FILENAME = "spec.mabl";
    public static final String MAIN_SPEC_DEFAULT_RUNTIME_FILENAME = "spec.runtime.json";
    final static Logger logger = LoggerFactory.getLogger(Mabl.class);
    final IntermediateSpecWriter intermediateSpecWriter;
    private final File specificationFolder;
    private final MableSettings settings;
    private final Set<ARootDocument> importedDocument = new HashSet<>();
    private boolean verbose;
    private List<Framework> frameworks;
    private ARootDocument document;
    private Map<Framework, Map.Entry<AConfigFramework, String>> frameworkConfigs = new HashMap<>();
    private IErrorReporter reporter = new IErrorReporter.SilentReporter();

    private ISimulationEnvironment environment;

    public RuntimeData runtimeData = new RuntimeData();

    public Mabl(File specificationFolder, File debugOutputFolder) {
        this(specificationFolder, debugOutputFolder, new MableSettings());
    }

    public Mabl(File specificationFolder, File debugOutputFolder, MableSettings settings) {
        this.specificationFolder = specificationFolder;
        this.settings = settings;
        this.intermediateSpecWriter = new IntermediateSpecWriter(debugOutputFolder, settings.dumpIntermediateSpecs);
    }

    static Map.Entry<Boolean, Map<INode, PType>> typeCheck(final List<ARootDocument> documentList, List<? extends PDeclaration> globalFunctions,
                                                           final IErrorReporter reporter) {

        try {
            TypeChecker typeChecker = new TypeChecker(reporter);
            boolean res = typeChecker.typeCheck(documentList, globalFunctions);
            return Map.entry(res, typeChecker.getCheckedTypes());
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return Map.entry(reporter.getErrorCount() == 0, new HashedMap());
    }

    public static List<String> extractModuleNames(List<ARootDocument> rootDocuments) {
        List<String> existingModules = new ArrayList<>();
        for (ARootDocument doc : rootDocuments) {
            Optional<List<AModuleDeclaration>> moduleDeclarations = NodeCollector.collect(doc, AModuleDeclaration.class);
            if (moduleDeclarations.isPresent()) {
                List<AModuleDeclaration> moduleDeclaration_ = moduleDeclarations.get();
                existingModules.addAll(moduleDeclaration_.stream().map(x -> x.getName().getText()).collect(Collectors.toList()));
            }
        }
        return existingModules;
    }

    public static ARootDocument createDocumentWithMissingModules(List<ARootDocument> existingDocuments) throws IOException {
        // Get the modules passed as arguments
        List<String> existingModules = extractModuleNames(existingDocuments);

        // Already added modules take priority over typechecker modules.
        // Therefore, remove existing modules from typechecker modules.
        List<AImportedModuleCompilationUnit> maestro2EmbeddedModules =
                getModuleDocuments(TypeChecker.getRuntimeModules()).stream().map(x -> NodeCollector.collect(x, AImportedModuleCompilationUnit.class))
                        .filter(x -> x.isPresent()).flatMap(x -> x.get().stream())
                        .filter(x -> existingModules.contains(x.getModule().getName().getText()) == false).collect(Collectors.toList());
        if (!maestro2EmbeddedModules.isEmpty()) {
            ARootDocument defaultModules = new ARootDocument();
            defaultModules.setContent(maestro2EmbeddedModules);
            return defaultModules;
        }
        return null;
    }

    public static ARootDocument getRuntimeModule(String module) throws IOException {
        InputStream resourceAsStream = TypeChecker.getRuntimeModule(module);
        if (resourceAsStream == null) {
            return null;
        }
        try {
            ARootDocument parse = MablParserUtil.parse(CharStreams.fromStream(resourceAsStream));
            return parse;
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + " in module " + module, e);
        }
    }

    public static List<ARootDocument> getModuleDocuments(List<String> modules) throws IOException {
        List<String> allModules = TypeChecker.getRuntimeModules();
        List<ARootDocument> documents = new ArrayList<>();
        if (modules != null) {
            for (String module : modules) {
                if (allModules.contains(module)) {
                    documents.add(getRuntimeModule(module));
                }
            }
        }
        return documents;
    }

    public IErrorReporter getReporter() {
        return reporter;
    }

    public void setReporter(IErrorReporter reporter) {
        this.reporter = reporter;
    }

    private List<String> getResourceFiles(String path) throws IOException {
        return IOUtils.readLines(this.getClass().getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
    }

    public MableSettings getSettings() {
        return settings;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void parse(List<File> sourceFiles) throws Exception {
        if (sourceFiles.isEmpty()) {
            return;
        }
        ARootDocument main = mergeDocuments(MablParserUtil.parse(sourceFiles));
        this.document = main == null ? document : main;
        postProcessParsing();
    }

    private ARootDocument mergeDocuments(List<ARootDocument> documentList) {
        ARootDocument main = this.document;
        for (ARootDocument doc : documentList) {
            if (doc == null) {
                continue;
            }
            Optional<List<ASimulationSpecificationCompilationUnit>> collect =
                    NodeCollector.collect(doc, ASimulationSpecificationCompilationUnit.class);
            if (collect.isPresent() && !collect.get().isEmpty()) {
                if (main != null) {
                    // A simulationSpecification already exists!
                    reporter.report(0, "A simulation specification compilation unit already exists.", new LexToken("", 0, 0));
                } else {
                    main = doc;
                }
            } else {
                importedDocument.add(doc);
            }
        }


        return main;
    }

    public void parse(CharStream specStreams) throws Exception {
        if (reporter.getErrorCount() != 0) {
            throw new IllegalArgumentException("Parsing cannot be called with errors");
        }
        environment = null;
        document = mergeDocuments(Collections.singletonList(MablParserUtil.parse(specStreams, reporter)));
        if (reporter.getErrorCount() == 0) {
            postProcessParsing();
        }
    }

    private void postProcessParsing() throws Exception {
        if (document != null) {
            List<ARootDocument> existingModules = new ArrayList<>(this.importedDocument);
            existingModules.add(document);
            ARootDocument docWithMissingModules = createDocumentWithMissingModules(existingModules);
            if (docWithMissingModules != null) {
                importedDocument.add(docWithMissingModules);
            }
            NodeCollector.collect(document, ASimulationSpecificationCompilationUnit.class).ifPresent(unit -> unit.forEach(u -> {
                frameworks = u.getFramework().stream().map(LexIdentifier::getText).map(Framework::valueOf).collect(Collectors.toList());
                frameworkConfigs = u.getFrameworkConfigs().stream()
                        .collect(Collectors.toMap(c -> Framework.valueOf(c.getName().getText()), c -> Map.entry(c, c.getConfig())));
            }));
            logger.debug("Frameworks: " + frameworks.stream().map(Object::toString).collect(Collectors.joining(",", "[", "]")));

            for (Map.Entry<Framework, Map.Entry<AConfigFramework, String>> pair : frameworkConfigs.entrySet()) {

                String data = StringAnnotationProcessor.processStringAnnotations(specificationFolder, pair.getValue().getValue());

                if (settings.inlineFrameworkConfig) {
                    pair.getValue().getKey().setConfig(data);
                }
                frameworkConfigs.put(pair.getKey(), Map.entry(pair.getValue().getKey(), data));
            }

            if (settings.inlineFrameworkConfig) {

                this.intermediateSpecWriter.write(document);
            }
        }

        if (environment == null && this.frameworks != null && this.frameworks.contains(Framework.FMI2) && frameworkConfigs != null &&
                frameworkConfigs.get(Framework.FMI2) != null) {
            logger.debug("Creating FMI2 simulation environment");
            environment = Fmi2SimulationEnvironment.of(new ByteArrayInputStream(
                    StringEscapeUtils.unescapeJava(frameworkConfigs.get(Framework.FMI2).getValue()).getBytes(StandardCharsets.UTF_8)), reporter);
        }

        if (environment != null) {
            environment.check(reporter);
        }

    }

    public void optimize() throws AnalysisException {
        if (document != null) {
            document.apply(new UnusedDeclarationOptimizer());
        }
    }

    public void expand() throws Exception {

        if (reporter.getErrorCount() != 0) {
            throw new IllegalArgumentException("Expansion cannot be called with errors");
        }

        ISimulationEnvironment env = null;
        if (frameworks != null && frameworkConfigs != null && frameworks.contains(Framework.FMI2) && frameworkConfigs.containsKey(Framework.FMI2)) {

            if (!frameworks.contains(Framework.FMI2) || !frameworkConfigs.containsKey(Framework.FMI2)) {
                throw new Exception("Framework annotations required for expansion. Please specify: " +
                        MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_FRAMEWORK) + " and " +
                        MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_FRAMEWORK_CONFIG));
            }

            env = getSimulationEnv();

            if (env == null) {
                throw new Exception("No env found");
            }
        }

        if (frameworks != null && frameworks.contains(Framework.FMI2) || frameworkConfigs != null && frameworkConfigs.containsKey(Framework.FMI2)) {
            MablSpecificationGenerator mablSpecificationGenerator =
                    new MablSpecificationGenerator(Framework.FMI2, verbose, env, specificationFolder, this.intermediateSpecWriter);

            List<ARootDocument> allDocs = Stream.concat(Stream.of(document), importedDocument.stream()).collect(Collectors.toList());

            ARootDocument missingModules = createDocumentWithMissingModules(allDocs);
            if (missingModules != null) {
                allDocs.add(missingModules);
            }

            ARootDocument doc = mablSpecificationGenerator.generateFromDocuments(allDocs);
            removeFrameworkAnnotations(doc);
            document = doc;
        }
    }

    public Map.Entry<Boolean, Map<INode, PType>> typeCheck() {
        logger.debug("Type checking");
        List<ARootDocument> docs = new Vector<>();
        docs.addAll(importedDocument);
        docs.add(document);
        return typeCheck(docs, new Vector<>(), reporter);
    }

    public boolean verify(Framework framework) {

        return verify(document, framework, reporter);

    }

    private boolean verify(final ARootDocument doc, Framework framework, final IErrorReporter reporter) {

        Collection<IMaestroVerifier> verifiers = PluginFactory.getPlugins(IMaestroVerifier.class, framework);

        verifiers.forEach(p -> logger.debug("Loaded verifiers: {} - {}", p.getName(), p.getVersion()));

        return verifiers.stream().allMatch(verifier -> {
            logger.info("Verifying with {} - {}", verifier.getName(), verifier.getVersion());
            return verifier.verify(doc, reporter);
        });
    }

    private void removeFrameworkAnnotations(ARootDocument doc) {
        if (!settings.preserveFrameworkAnnotations) {
            NodeCollector.collect(doc, ASimulationSpecificationCompilationUnit.class).ifPresent(list -> list.forEach(unit -> {
                unit.getFrameworkConfigs().clear();
                unit.getFramework().clear();
            }));
        }
    }

    private void processTemplate(ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit,
                                 Fmi2SimulationEnvironment simulationEnvironment) throws Exception {
        List<? extends LexIdentifier> imports = aSimulationSpecificationCompilationUnit.getImports();
        List<ARootDocument> moduleDocuments = getModuleDocuments(imports.stream().map(LexIdentifier::getText).collect(Collectors.toList()));
        String template = PrettyPrinter.print(aSimulationSpecificationCompilationUnit);
        environment = simulationEnvironment;
        logger.trace("Generated template:\n{}", template);
        ARootDocument templateParsed = MablParserUtil.parse(CharStreams.fromString(template));
        moduleDocuments.add(templateParsed);
        document = this.mergeDocuments(moduleDocuments);

        postProcessParsing();
    }

    public void generateSpec(ScenarioConfiguration configuration) throws Exception {
        if (configuration == null) {
            throw new Exception("No configuration");
        }
        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = TemplateGenerator.generateTemplate(configuration);
        processTemplate(aSimulationSpecificationCompilationUnit, configuration.getSimulationEnvironment());
    }

    public void generateSpec(MaBLTemplateConfiguration configuration) throws Exception {
        if (configuration == null) {
            throw new Exception("No configuration");
        }
        runtimeData.websocketPort = configuration.getWebsocketPort();
        ASimulationSpecificationCompilationUnit aSimulationSpecificationCompilationUnit = TemplateGenerator.generateTemplate(configuration);
        if (configuration.getFrameworkConfig() != null) {
            Fmi2SimulationEnvironmentConfiguration conf = configuration.getFrameworkConfig().getValue();
            this.runtimeData.livestreamVariables = conf.livestream;
            if ((conf.variablesToLog != null && conf.variablesToLog.size() > 0) || (conf.logVariables != null && conf.logVariables.size() > 0)) {
                this.runtimeData.outputVariables = new HashMap<>();
                if (conf.variablesToLog != null) {
                    this.runtimeData.outputVariables.putAll(conf.variablesToLog);
                }
                if (conf.logVariables != null) {
                    this.runtimeData.outputVariables.putAll(conf.logVariables);
                }
            }
            this.runtimeData.liveStreamInterval = 0.1;

        }
        processTemplate(aSimulationSpecificationCompilationUnit, configuration.getSimulationEnvironment());
    }

    //FIXME should be private
    public ISimulationEnvironment getSimulationEnv() throws Exception {
        return environment;
    }

    public ARootDocument getMainSimulationUnit() {
        return this.document;
    }

    public Object getRuntimeData() throws Exception {
        return new MablRuntimeDataGenerator(getSimulationEnv(), this.runtimeData).getRuntimeData();
    }

    public String getRuntimeDataAsJsonString() throws Exception {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(getRuntimeData());
    }

    public void dump(File folder) throws Exception {
        FileUtils.write(new File(folder, MAIN_SPEC_DEFAULT_FILENAME), PrettyPrinter.print(getMainSimulationUnit()), StandardCharsets.UTF_8);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(new File(folder, MAIN_SPEC_DEFAULT_RUNTIME_FILENAME), getRuntimeData());
        if (runtimeData.getWebsocketPort() != null) {
            WebSocketDataWriter.dumpWebsocketUi(folder.toPath().resolve("graph.html"), runtimeData.getWebsocketPort());
        }
    }

    /**
     * //FIXME Temporary function
     * Temporary function. This should be removed once the API for expansion plugins is updated with the ability for them to report back
     * required
     * runtime extensions
     *
     * @param runtimeEnvironmentVariables
     */
    public void setRuntimeEnvironmentVariables(Map<String, Object> runtimeEnvironmentVariables) {
        this.runtimeData.runtimeEnvironmentVariables = runtimeEnvironmentVariables;
    }

    public static class MableSettings {
        public boolean inlineFrameworkConfig = true;
        public boolean dumpIntermediateSpecs = true;
        public boolean preserveFrameworkAnnotations = false;
    }
}
