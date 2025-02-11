package org.intocps.maestro.webapi.maestro2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spencerwi.either.Either;
import org.intocps.verification.scenarioverifier.core.ScenarioLoaderFMI2;
import org.intocps.verification.scenarioverifier.core.masterModel.MasterModel;
import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.INode;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.cli.ImportCmd;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.dto.MultiModel;
import org.intocps.maestro.core.messages.ErrorReporter;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironmentConfiguration;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.extensions.SimulationControlDefaultLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.simulationcontrol.SimulationControlValue;
import org.intocps.maestro.plugin.JacobianStepConfig;
import org.intocps.maestro.plugin.MasterModelMapper;
import org.intocps.maestro.template.MaBLTemplateConfiguration;
import org.intocps.maestro.template.ScenarioConfiguration;
import org.intocps.maestro.typechecker.TypeChecker;
import org.intocps.maestro.webapi.maestro2.dto.InitializationData;
import org.intocps.maestro.webapi.maestro2.dto.SigverSimulateRequestBody;
import org.intocps.maestro.webapi.maestro2.dto.SimulateRequestBody;
import org.intocps.maestro.webapi.maestro2.interpreter.WebApiInterpreterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Maestro2Broker {
    private final static Logger logger = LoggerFactory.getLogger(Maestro2Broker.class);
    final Mabl mabl;
    final File workingDirectory;
    final ErrorReporter reporter;
    private final Supplier<Boolean> isStopRequsted;
    private final Function<Map<String, List<String>>, List<String>> flattenFmuIds =
            map -> map.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(v -> entry.getKey() + "." + v))
                    .collect(Collectors.toList());
    private Map.Entry<Boolean, Map<INode, PType>> typeCheckResult;

    public Maestro2Broker(File workingDirectory, ErrorReporter reporter, Supplier<Boolean> isStopRequsted) {
        this.workingDirectory = workingDirectory;
        this.isStopRequsted = isStopRequsted;
        Mabl.MableSettings mableSettings = new Mabl.MableSettings();
        mableSettings.dumpIntermediateSpecs = false;
        mableSettings.inlineFrameworkConfig = true;
        this.mabl = new Mabl(workingDirectory, null, mableSettings);
        this.reporter = reporter;

        mabl.setReporter(this.reporter);
    }

    public <T extends MultiModel> void buildAndRunMasterModel(Map<String, List<String>> livestreamVariables, WebSocketSession socket, T multiModel,
                                                              SigverSimulateRequestBody body, File csvOutputFile) throws Exception {
        MasterModel masterModel = ScenarioLoaderFMI2.load(new ByteArrayInputStream(body.getMasterModel().getBytes()));
        Fmi2SimulationEnvironmentConfiguration simulationConfiguration =
                new Fmi2SimulationEnvironmentConfiguration(MasterModelMapper.Companion.masterModelConnectionsToMultiModelConnections(masterModel),
                        multiModel.getFmus());
        simulationConfiguration.logVariables = multiModel.getLogVariables();
        if (simulationConfiguration.logVariables == null) {
            simulationConfiguration.variablesToLog = new HashMap<>();
        }

        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, reporter);
        ScenarioConfiguration configuration =
                new ScenarioConfiguration(simulationEnvironment, masterModel, multiModel.getParameters(), multiModel.getGlobal_relative_tolerance(),
                        multiModel.getGlobal_absolute_tolerance(), multiModel.getConvergenceAttempts(), body.getStartTime(), body.getEndTime(),
                        multiModel.getAlgorithm().getStepSize(), Pair.of(Framework.FMI2, simulationConfiguration), multiModel.isLoggingOn(),
                        multiModel.getLogLevels());

        String runtimeJsonConfigString = generateSpecification(configuration, null);

        typeCheckResult = mabl.typeCheck();

        if (!typeCheckResult.getKey()) {
            throw new Exception("Specification did not type check");
        }

        if (!mabl.verify(Framework.FMI2)) {
            throw new Exception("Specification did not verify");
        }

        List<String> portsToLog = Stream.concat(simulationEnvironment.getConnectedOutputs().stream().map(x -> {
            ComponentInfo i = simulationEnvironment.getUnitInfo(new LexIdentifier(x.instance.getText(), null), Framework.FMI2);
            return String.format("%s.%s.%s", i.fmuIdentifier, x.instance.getText(), x.getName());
        }), multiModel.getLogVariables() == null ? Stream.of() : multiModel.getLogVariables().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(v -> entry.getKey() + "." + v))).collect(Collectors.toList());

        List<String> liveStreamFilter = livestreamVariables == null ? List.of() : flattenFmuIds.apply(livestreamVariables);

        executeInterpreter(socket, portsToLog, liveStreamFilter, body.getLiveLogInterval(), csvOutputFile,
                new ByteArrayInputStream(runtimeJsonConfigString.getBytes()));
    }

    public void buildAndRun(InitializationData initializeRequest, SimulateRequestBody body, WebSocketSession socket,
                            File csvOutputFile) throws Exception {

        //Initially resolve any FMUs to the local folder in case they are uploaded
        ImportCmd.resolveFmuPaths(Collections.singletonList(workingDirectory), initializeRequest.getFmus());

        Fmi2SimulationEnvironmentConfiguration simulationConfiguration =
                new Fmi2SimulationEnvironmentConfiguration(initializeRequest.getConnections(), initializeRequest.getFmus());

        simulationConfiguration.logVariables = initializeRequest.getLogVariables();
        if (simulationConfiguration.logVariables == null) {
            simulationConfiguration.variablesToLog = new HashMap<>();
        }
        simulationConfiguration.livestream = initializeRequest.getLivestream();
        simulationConfiguration.faultInjectInstances = initializeRequest.faultInjectInstances;
        simulationConfiguration.faultInjectConfigurationPath = initializeRequest.faultInjectConfigurationPath;

        Map<String, Object> initialize = new HashMap<>();
        Map<String, Object> parameters = initializeRequest.getParameters();

        if (parameters != null) {
            initialize.put("parameters", parameters);
        }

        if (initializeRequest.getEnvironmentParameters() != null) {
            initialize.put("environmentParameters", initializeRequest.getEnvironmentParameters());
        }
        Fmi2SimulationEnvironment simulationEnvironment = Fmi2SimulationEnvironment.of(simulationConfiguration, this.reporter);

        // Loglevels from app consists of {key}.instance: [loglevel1, loglevel2,...] but have to be: instance: [loglevel1, loglevel2,...].
        Map<String, List<String>> removedFMUKeyFromLogLevels = body.getLogLevels() == null ? new HashMap<>() : body.getLogLevels().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getFmuInstanceFromFmuKeyInstance(entry.getKey()),
                        Map.Entry::getValue));

        // Setup step config
        JacobianStepConfig config = new JacobianStepConfig();
        config.stabilisation = initializeRequest.isStabalizationEnabled();
        config.absoluteTolerance = initializeRequest.getGlobal_absolute_tolerance();
        config.relativeTolerance = initializeRequest.getGlobal_relative_tolerance();
        config.stabilisationLoopMaxIterations = 5;
        config.simulationProgramDelay = initializeRequest.isSimulationProgramDelay();
        config.stepAlgorithm = initializeRequest.getAlgorithm();
        config.startTime = body.getStartTime();
        config.endTime = body.getEndTime();

        MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder builder =
                MaBLTemplateConfiguration.MaBLTemplateConfigurationBuilder.getBuilder().setFrameworkConfig(Framework.FMI2, simulationConfiguration)
                        .useInitializer(true, new ObjectMapper().writeValueAsString(initialize)).setFramework(Framework.FMI2)
                        .setLogLevels(removedFMUKeyFromLogLevels).setVisible(initializeRequest.isVisible())
                        .setLoggingOn(initializeRequest.isLoggingOn()).setStepAlgorithmConfig(config);


        MaBLTemplateConfiguration configuration = builder.build();
        // Validate that external spec files only includes mabl files that can be resolved
        AtomicBoolean didLocateFaultInjectionFile = new AtomicBoolean(false);
        if (initializeRequest.getExternalSpecs() != null) {
            List<File> filesNotResolved = new ArrayList<>();
            List<File> nonMablFiles = initializeRequest.getExternalSpecs().stream().filter(file -> {
                // While filtering also test that the file actually exists
                if (!file.exists()) {
                    filesNotResolved.add(file);
                }
                if (file.getName().toLowerCase().endsWith("faultinject.mabl")) {
                    didLocateFaultInjectionFile.set(true);
                }
                return !file.getName().toLowerCase(Locale.ROOT).endsWith(".mabl");
            }).collect(Collectors.toList());
            String errMsg = "";
            if (filesNotResolved.size() > 0) {
                errMsg = "Cannot resolve path to spec files: " + nonMablFiles.stream().map(File::getName).reduce("", (prev, cur) -> prev + " " + cur);
            } else if (nonMablFiles.size() > 0) {
                errMsg = "Cannot load spec files: " + nonMablFiles.stream().map(File::getName).reduce("", (prev, cur) -> prev + " " + cur) +
                        ". Only mabl files should be " + "included as " + "external specs.";
            }

            if (!errMsg.equals("")) {
                throw new Exception(errMsg);
            }

            mabl.parse(initializeRequest.getExternalSpecs());
        }
        // If fault injection is configured then the faultinject.mabl file should be included as an external spec.
        if (initializeRequest.faultInjectConfigurationPath != null && !initializeRequest.faultInjectConfigurationPath.equals("") &&
                !didLocateFaultInjectionFile.get()) {
            throw new Exception("Remember to include FaultInject.mabl as an external spec");
        }
        String runtimeJsonConfigString = generateSpecification(configuration, parameters);

        if (!mabl.typeCheck().getKey()) {
            throw new Exception("Specification did not type check");
        }


        if (!mabl.verify(Framework.FMI2)) {
            throw new Exception("Specification did not verify");
        }

        List<String> connectedOutputs = simulationEnvironment.getConnectedOutputs().stream().map(x -> {
            ComponentInfo i = simulationEnvironment.getUnitInfo(new LexIdentifier(x.instance.getText(), null), Framework.FMI2);
            return String.format("%s.%s.%s", i.fmuIdentifier, x.instance.getText(), x.getName());
        }).collect(Collectors.toList());


        executeInterpreter(socket, Stream.concat(connectedOutputs.stream(),
                        (initializeRequest.getLogVariables() == null ? new Vector<String>() : flattenFmuIds.apply(
                                initializeRequest.getLogVariables())).stream()).collect(Collectors.toList()),
                initializeRequest.getLivestream() == null ? new Vector<>() : flattenFmuIds.apply(initializeRequest.getLivestream()),
                body.getLiveLogInterval() == null ? 0d : body.getLiveLogInterval(), csvOutputFile,
                new ByteArrayInputStream(runtimeJsonConfigString.getBytes()));
    }

    public String generateSpecification(ScenarioConfiguration config, Map<String, Object> parameters) throws Exception {
        mabl.generateSpec(config);
        return postGenerate(parameters);
    }

    public String generateSpecification(MaBLTemplateConfiguration config, Map<String, Object> parameters) throws Exception {
        mabl.generateSpec(config);
        return postGenerate(parameters);
    }

    private String postGenerate(Map<String, Object> parameters) throws Exception {
        mabl.expand();
        mabl.setRuntimeEnvironmentVariables(parameters);
        mabl.dump(workingDirectory);
        logger.debug(PrettyPrinter.printLineNumbers(mabl.getMainSimulationUnit()));
        return new ObjectMapper().writeValueAsString(mabl.getRuntimeData());
    }

    public void executeInterpreter(WebSocketSession webSocket, List<String> csvFilter, List<String> webSocketFilter, double interval,
                                   File csvOutputFile,
                                   InputStream config) throws IOException, AnalysisException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        WebApiInterpreterFactory factory;
        if (webSocket != null) {
            factory = new WebApiInterpreterFactory(workingDirectory, webSocket, interval, webSocketFilter, new File(workingDirectory, "outputs.csv"),
                    csvFilter, name -> TypeChecker.findModule(typeCheckResult.getValue(), name), config);
        } else {
            factory = new WebApiInterpreterFactory(workingDirectory, csvOutputFile, csvFilter,
                    name -> TypeChecker.findModule(typeCheckResult.getValue(), name), config);
        }

        // create and install the overrides' simulation control lifecycle handler that links this simulation to the current session
        @IValueLifecycleHandler.ValueLifecycle(name = "SimulationControl")
        class WebSimulationControlDefaultLifecycleHandler extends SimulationControlDefaultLifecycleHandler {
            @Override
            public Either<Exception, Value> instantiate(List<Value> args) {
                return Either.right(new SimulationControlValue(isStopRequsted));
            }
        }

        factory.addLifecycleHandler(new WebSimulationControlDefaultLifecycleHandler());
        new MableInterpreter(factory).execute(mabl.getMainSimulationUnit());
    }

    public void setVerbose(boolean verbose) {
        mabl.setVerbose(verbose);
    }
}
