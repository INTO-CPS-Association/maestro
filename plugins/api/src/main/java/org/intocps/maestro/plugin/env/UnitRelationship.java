package org.intocps.maestro.plugin.env;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.core.Framework;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UnitRelationship implements ISimulationEnvironment {


    @Override
    public Set<UnitRelationship.Relation> getRelations(List<LexIdentifier> identifiers) {

        // a, b

        Map<LexIdentifier, Object> internalMapping = identifiers.stream().collect(Collectors.toMap(Function.identity(), id -> {

            //check context to find which FMU in the internal map that id points to in terms of FMU.modelinstance
            return null;

        }));

        //a -> FMUA, b-> FMUB

        /*
         * use mapping of a.#1 -> b.#4 and produce a relation for these
         *
         * Relation a.#1 Outputs to [b.#4, ....]
         * Relation b.#4 Inputs from [a.#1]
         *
         * not sure if we need to return all permutations
         * */


        //construct relational information with the framework relevant attributes

        /*
         * user of this for stepping will first look for all outputs from here and collect these or directly set or use these outputs + others and
         * then use the relation to set these*/

        return null;
    }

    @Override
    public Set<Relation> getRelations(LexIdentifier... identifiers) {
        if (identifiers == null) {
            return Collections.emptySet();
        }
        return this.getRelations(Arrays.asList(identifiers));
    }

    @Override
    public <T extends FrameworkUnitInfo> T getUnitInfo(LexIdentifier identifier, Framework framework) {
        return null;
    }

    public interface FrameworkVariableInfo {
    }

    public interface FrameworkUnitInfo {
    }

    public static class Relation {
        Variable source;
        Direction direction;
        //TODO: other info about the relation that may be relevant
        Set<Map<LexIdentifier, Variable>> targets;

        public Variable getSource() {
            return source;
        }

        public Direction getDirection() {
            return direction;
        }

        public Set<Map<LexIdentifier, Variable>> getTargets() {
            return targets;
        }

        enum Direction {
            Output,
            Input
        }
    }

    public class Variable {

        <T extends FrameworkVariableInfo> T getFrameworkInfo(Framework framework) {
            return null;
        }
    }


}
