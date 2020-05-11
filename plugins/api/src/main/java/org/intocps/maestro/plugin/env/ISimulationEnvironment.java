package org.intocps.maestro.plugin.env;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;

import java.util.List;
import java.util.Set;

public interface ISimulationEnvironment {

    /**
     * Returns information about the relationship between the state in the units represented by the identifiers
     *
     * @param identifiers
     * @return
     */
    Set<UnitRelationship.Relation> getRelations(LexIdentifier... identifiers);

    Set<UnitRelationship.Relation> getRelations(List<LexIdentifier> identifiers);

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
