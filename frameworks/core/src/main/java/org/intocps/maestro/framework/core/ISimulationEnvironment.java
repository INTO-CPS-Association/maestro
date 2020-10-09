package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISimulationEnvironment {

    /**
     * Returns information about the relationship between the state in the units represented by the identifiers
     *
     * @param identifiers
     * @return
     */
    Set<? extends FrameworkVariableInfo> getRelations(LexIdentifier... identifiers);

    List<? extends RelationVariable> getVariablesToLog(String instanceName);

    /**
     * Returns a list of all scalar variables to log in CSV for a given instance
     *
     * @param instanceName
     * @return
     */
    List<? extends RelationVariable> getCsvVariablesToLog(String instanceName);

    Map<String, List<String>> getLivestreamVariablesToLog();

    Set<? extends Map.Entry<String, ? extends FrameworkUnitInfo>> getInstances();

    Set<? extends FrameworkVariableInfo> getRelations(List<LexIdentifier> identifiers);

    EnvironmentMessage getEnvironmentMessage();

    /**
     * Returns information about the unit
     *
     * @param identifier
     * @param framework
     * @param <T>
     * @return
     */
    <T extends FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework);
}
