package org.intocps.maestro;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.StringAnnotationProcessor;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.parser.MablLexer;
import org.intocps.maestro.parser.MablParserUtil;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.template.MaBLTemplateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mabl {
    public static final String MAIN_SPEC_DEFAULT_FILENAME = "spec.mabl";
    public static final String MAIN_SPEC_DEFAULT_RUNTIME_FILENAME = "spec.runtime.json";
    final static Logger logger = LoggerFactory.getLogger(Mabl.class);
    final IntermediateSpecWriter intermediateSpecWriter;
    private final File specificationFolder;
    private final MableSettings settings = new MableSettings();
    private final Set<ARootDocument> importedDocument = new HashSet<>();
    private boolean verbose;
    private List<Framework> frameworks;
    private ARootDocument document;
    private Map<Framework, Map.Entry<AConfigFramework, String>> frameworkConfigs = new HashMap<>();
    private IErrorReporter reporter = new IErrorReporter.SilentReporter();

    public Mabl(File specificationFolder, File debugOutputFolder) {
        this.specificationFolder = specificationFolder;
        this.intermediateSpecWriter = new IntermediateSpecWriter(debugOutputFolder, debugOutputFolder != null);
    }

    public MableSettings getSettings() {
        return settings;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setReporter(IErrorReporter reporter) {
        this.reporter = reporter;
    }

    public void parse(List<File> sourceFiles) throws IOException {
        if (sourceFiles.isEmpty()) {
            return;
        }
        ARootDocument main = mergeDocuments(MablParserUtil.parse(sourceFiles));
        this.document = main == null ? document : main;
        postProcessParsing();
    }

    private ARootDocument mergeDocuments(List<ARootDocument> documentList) {
        ARootDocument main = null;
        for (ARootDocument doc : documentList) {
            Optional<List<ASimulationSpecificationCompilationUnit>> collect =
                    NodeCollector.collect(doc, ASimulationSpecificationCompilationUnit.class);
            if (collect.isPresent() && !collect.get().isEmpty()) {
                main = doc;
            } else {
                importedDocument.add(doc);
            }
        }


        return main;
    }

    public void parse(CharStream specStreams) throws IOException {
        if (reporter.getErrorCount() != 0) {
            throw new IllegalArgumentException("Parsing cannot be called with errors");
        }
        document = mergeDocuments(Collections.singletonList(MablParserUtil.parse(specStreams, reporter)));
        if (reporter.getErrorCount() == 0) {
            postProcessParsing();
        }
    }

    private void postProcessParsing() throws IOException {

        if (document != null) {
            intermediateSpecWriter.write(document);
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


    }

    public void expand() throws Exception {

        if (reporter.getErrorCount() != 0) {
            throw new IllegalArgumentException("Expansion cannot be called with errors");
        }
        if (frameworks != null && frameworkConfigs != null && frameworks.contains(Framework.FMI2) && frameworkConfigs.containsKey(Framework.FMI2)) {
            //            if (!ShouldExpandAnalysis.shouldExpand(document)) {
            //                return;
            //            }


            if (!frameworks.contains(Framework.FMI2) || !frameworkConfigs.containsKey(Framework.FMI2)) {
                throw new Exception("Framework annotations required for expansion. Please specify: " +
                        MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_FRAMEWORK) + " and " +
                        MablLexer.VOCABULARY.getDisplayName(MablLexer.AT_FRAMEWORK_CONFIG));
            }

            ISimulationEnvironment env = getSimulationEnv();

            if (env == null) {
                throw new Exception("No env found");
            }

            MablSpecificationGenerator mablSpecificationGenerator =
                    new MablSpecificationGenerator(Framework.FMI2, verbose, env, specificationFolder, this.intermediateSpecWriter);

            List<ARootDocument> allDocs = Stream.concat(Stream.of(document), importedDocument.stream()).collect(Collectors.toList());

            ARootDocument doc = mablSpecificationGenerator.generateFromDocuments(allDocs);
            removeFrameworkAnnotations(doc);
            document = doc;
        }


    }

    private void removeFrameworkAnnotations(ARootDocument doc) {
        if (!settings.preserveFrameworkAnnotations) {
            NodeCollector.collect(doc, ASimulationSpecificationCompilationUnit.class).ifPresent(list -> list.forEach(unit -> {
                unit.getFrameworkConfigs().clear();
                unit.getFramework().clear();
            }));
        }
    }

    public void generateSpec(MaBLTemplateConfiguration configuration) throws Exception {

        if (configuration == null) {
            throw new Exception("No configuration");
        }

        String template = PrettyPrinter.print(MaBLTemplateGenerator.generateTemplate(configuration));
        logger.trace("Generated template:\n{}", template);
        document = MablParserUtil.parse(CharStreams.fromString(template));
        postProcessParsing();
    }

    //FIXME should be private
    public ISimulationEnvironment getSimulationEnv() throws Exception {
        if (this.frameworks.contains(Framework.FMI2) && frameworkConfigs.get(Framework.FMI2) != null) {

            return Fmi2SimulationEnvironment.of(new ByteArrayInputStream(
                    StringEscapeUtils.unescapeJava(frameworkConfigs.get(Framework.FMI2).getValue()).getBytes(StandardCharsets.UTF_8)), reporter);
        }
        logger.error("No framework env found");
        return null;
    }

    public ARootDocument getMainSimulationUnit() {
        return this.document;
    }

    public Object getRuntimeData() throws Exception {
        return new MablRuntimeDataGenerator(getSimulationEnv()).getRuntimeData();
    }

    public String getRuntimeDataAsJsonString() throws Exception {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(getRuntimeData());
    }

    public void dump(File folder) throws Exception {
        FileUtils.write(new File(folder, MAIN_SPEC_DEFAULT_FILENAME), PrettyPrinter.print(getMainSimulationUnit()), StandardCharsets.UTF_8);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(new File(folder, MAIN_SPEC_DEFAULT_RUNTIME_FILENAME), getRuntimeData());
    }

    public static class MableSettings {
        public boolean inlineFrameworkConfig = true;
        public boolean dumpIntermediateSpecs = true;
        public boolean preserveFrameworkAnnotations = false;
    }

    private static class ShouldExpandAnalysis extends DepthFirstAnalysisAdaptor {
        boolean shouldExpand = false;

        static boolean shouldExpand(INode node) throws AnalysisException {
            ShouldExpandAnalysis analysis = new ShouldExpandAnalysis();
            node.apply(analysis);
            return analysis.shouldExpand;
        }

        @Override
        public void caseACallExp(ACallExp node) throws AnalysisException {
            super.caseACallExp(node);
            if (node.getObject() == null) {
                shouldExpand = true;
            }
        }
    }
}
