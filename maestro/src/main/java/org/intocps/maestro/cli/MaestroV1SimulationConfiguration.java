package org.intocps.maestro.cli;

import com.fasterxml.jackson.annotation.*;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.core.dto.MultiModel;
import scala.reflect.internal.Mode;

import java.util.List;
import java.util.Map;

public class MaestroV1SimulationConfiguration extends MultiModel {

    @JsonProperty("startTime")
    private final double startTime;
    @JsonProperty("endTime")
    private final double endTime;
    @JsonProperty("reportProgress")
    private final Boolean reportProgress;

    @JsonCreator
    public MaestroV1SimulationConfiguration(@JsonProperty("fmus") Map<String, String> fmus,
            @JsonProperty("connections") Map<String, List<String>> connections, @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("logVariables") Map<String, List<String>> logVariables, @JsonProperty("parallelSimulation") boolean parallelSimulation,
            @JsonProperty("stabalizationEnabled") boolean stabalizationEnabled,
            @JsonProperty("global_absolute_tolerance") double global_absolute_tolerance,
            @JsonProperty("global_relative_tolerance") double global_relative_tolerance, @JsonProperty("loggingOn") boolean loggingOn,
            @JsonProperty("visible") boolean visible, @JsonProperty("simulationProgramDelay") boolean simulationProgramDelay,
            @JsonProperty("algorithm") IAlgorithmConfig algorithm, @JsonProperty("overrideLogLevel") InitializeLogLevel overrideLogLevel,
            @JsonProperty("environmentParameters") List<String> environmentParameters,
            @JsonProperty("logLevels") Map<String, List<String>> logLevels, @JsonProperty("startTime") double startTime,
            @JsonProperty("endTime") double endTime, @JsonProperty("reportProgress") Boolean reportProgress,
            @JsonProperty("faultInjectConfigurationPath") String faultInjectConfigurationPath,
            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances,
            @JsonProperty("convergenceAttempts") int convergenceAttempts,
            @JsonProperty("modelTransfers") Map<String, String> modelTransfers,
            @JsonProperty("modelSwaps") Map<String, ModelSwap> modelSwaps) {
        super(fmus, connections, parameters, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters,
                logLevels, faultInjectConfigurationPath, faultInjectInstances, convergenceAttempts, modelTransfers, modelSwaps);
        this.startTime = startTime;
        this.endTime = endTime;
        this.reportProgress = reportProgress;
    }

    public double getStartTime() {return startTime;}
    public double getEndTime(){return endTime;}
    public Boolean getReportProgress() {return reportProgress;}
    public String getFaultInjectConfigurationPath(){return faultInjectConfigurationPath;}
    public Map<String, String> getFaultInjectInstances() {return faultInjectInstances;}
    public Map<String, String> getModelTransfers() {return modelTransfers;}
    public Map<String, ModelSwap> getModelSwaps() {return modelSwaps;}
}
