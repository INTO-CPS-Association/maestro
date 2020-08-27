package org.intocps.maestro.plugin.env;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.maestro.plugin.env.fmi2.RelationVariable;

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
    Set<UnitRelationship.Relation> getRelations(LexIdentifier... identifiers);

    List<RelationVariable> getVariablesToLog(String instanceName);

    /**
     * Returns a list of all scalar variables to log in CSV for a given instance
     *
     * @param instanceName
     * @return
     */
    List<RelationVariable> getCsvVariablesToLog(String instanceName);

    Map<String, List<String>> getLivestreamVariablesToLog();

    Set<Map.Entry<String, ComponentInfo>> getInstances();

    Set<UnitRelationship.Relation> getRelations(List<LexIdentifier> identifiers);

    EnvironmentMessage getEnvironmentMessage();

    /**
     * Returns information about the unit
     *
     * @param identifier
     * @param framework
     * @param <T>
     * @return
     */
    <T extends UnitRelationship.FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework);
}
