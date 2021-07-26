package org.intocps.maestro.cli;

import com.fasterxml.jackson.annotation.*;
import org.intocps.maestro.core.dto.IAlgorithmConfig;
import org.intocps.maestro.core.dto.MultiModel;

import java.util.List;
import java.util.Map;

public class MaestroV1SimulationConfiguration extends MultiModel {

    @JsonProperty("startTime")
    private final double startTime;
    @JsonProperty("endTime")
    private final double endTime;
    @JsonProperty("reportProgress")
    private final Boolean reportProgress;

    @JsonProperty("faultInjectConfigurationPath")
    private final String faultInjectConfigurationPath;
    @JsonProperty("faultInjectInstances")
    private final Map<String, String> faultInjectInstances;

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
            @JsonProperty("faultInjectInstances") Map<String, String> faultInjectInstances) {
        super(fmus, connections, parameters, logVariables, parallelSimulation, stabalizationEnabled, global_absolute_tolerance,
                global_relative_tolerance, loggingOn, visible, simulationProgramDelay, algorithm, overrideLogLevel, environmentParameters, logLevels);
        this.startTime = startTime;
        this.endTime = endTime;
        this.reportProgress = reportProgress;
        this.faultInjectConfigurationPath = faultInjectConfigurationPath;
        this.faultInjectInstances = faultInjectInstances;
    }

    public double getStartTime() {return startTime;}
    public double getEndTime(){return endTime;}
    public Boolean getReportProgress() {return reportProgress;}
    public String getFaultInjectConfigurationPath(){return faultInjectConfigurationPath;}
    public Map<String, String> getFaultInjectInstances() {return faultInjectInstances;}
}
