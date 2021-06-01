package org.intocps.maestro.plugin.Initializer;

import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.plugin.ExpandException;
import org.intocps.topologicalsorting.TarjanGraph;
import org.intocps.topologicalsorting.data.AcyclicDependencyResult;
import org.intocps.topologicalsorting.data.CyclicDependencyResult;
import org.intocps.topologicalsorting.data.Edge11;
import scala.collection.JavaConverters;

import java.util.*;
import java.util.stream.Collectors;

public class TopologicalPlugin {
    //This method find the right instantiation order using the topological sort plugin. The plugin is in scala so some mapping between java and
    // scala is needed
    public List<Fmi2SimulationEnvironment.Variable> findInstantiationOrder(Set<Fmi2SimulationEnvironment.Relation> relations) throws ExpandException {
        TarjanGraph graphSolver = getTarjanGraph(relations);

        var topologicalOrderToInstantiate = graphSolver.topologicalSort();
        if (topologicalOrderToInstantiate instanceof CyclicDependencyResult) {
            CyclicDependencyResult cycles = (CyclicDependencyResult) topologicalOrderToInstantiate;
            throw new ExpandException("Cycles are present in the systems: " + cycles.cycle());
        }

        return (List<Fmi2SimulationEnvironment.Variable>) JavaConverters
                .seqAsJavaListConverter(((AcyclicDependencyResult) topologicalOrderToInstantiate).totalOrder()).asJava();
    }

    public List<Set<Fmi2SimulationEnvironment.Variable>> findInstantiationOrderStrongComponents(Set<Fmi2SimulationEnvironment.Relation> relations) {
        TarjanGraph graphSolver = getTarjanGraph(relations);

        var topologicalOrderToInstantiate = graphSolver.topologicalSCC();

        Map<Fmi2SimulationEnvironment.Variable, Integer> javaMap =
                (Map<Fmi2SimulationEnvironment.Variable, Integer>) JavaConverters.mapAsJavaMapConverter(topologicalOrderToInstantiate).asJava();

        return groupSCC(javaMap);
    }

    private List<Set<Fmi2SimulationEnvironment.Variable>> groupSCC(Map<Fmi2SimulationEnvironment.Variable, Integer> javaMap) {
        List<Set<Fmi2SimulationEnvironment.Variable>> sccs = new Vector<>();
        var list = javaMap.entrySet().stream().collect(Collectors.groupingBy(o -> o.getValue()));
        javaMap.values().stream().sorted().forEach(scc -> {
            HashSet<Fmi2SimulationEnvironment.Variable> variablesSet = new HashSet<>();
            list.get(scc).forEach(v -> variablesSet.add(v.getKey()));
            sccs.add(variablesSet);
        });
        return sccs;
    }


    private TarjanGraph getTarjanGraph(Set<Fmi2SimulationEnvironment.Relation> relations) {
        var externalRelations = relations.stream().filter(RelationsPredicates.external()).collect(Collectors.toList());
        var internalRelations = relations.stream().filter(RelationsPredicates.internal()).collect(Collectors.toList());

        internalRelations = internalRelations.stream().filter(RelationsPredicates.inputSource()
                .or(RelationsPredicates
                        .outputSource().and(o -> externalRelations.stream().anyMatch(i -> o.getSource() == i.getSource()))))
                .collect(Collectors.toList());

        var edges = new Vector<Edge11<Fmi2SimulationEnvironment.Variable, Fmi2SimulationEnvironment.Relation.InternalOrExternal>>();
        externalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(o.getSource(), e, o.getOrigin()));
        }));
        internalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(e, o.getSource(), o.getOrigin()));
        }));

        return new TarjanGraph(JavaConverters.iterableAsScalaIterableConverter(edges).asScala());
    }


}
