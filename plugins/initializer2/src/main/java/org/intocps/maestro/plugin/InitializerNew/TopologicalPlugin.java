package org.intocps.maestro.plugin.InitializerNew;

import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.intocps.topologicalsorting.TarjanGraph;
import org.intocps.topologicalsorting.data.AcyclicDependencyResult;
import org.intocps.topologicalsorting.data.CyclicDependencyResult;
import org.intocps.topologicalsorting.data.Edge11;
import scala.jdk.CollectionConverters;

import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

public class TopologicalPlugin {
    //This method find the right instantiation order using the topological sort plugin. The plugin is in scala so some mapping between java and
    // scala is needed
    public List<UnitRelationship.Variable> FindInstantiationOrder(Set<UnitRelationship.Relation> relations) throws UnfoldException {
        var externalRelations =
                relations.stream().filter(o -> o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External).collect(Collectors.toList());
        var internalRelations =
                relations.stream().filter(o -> o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.Internal).collect(Collectors.toList());


        internalRelations =
                internalRelations.stream().filter(o -> o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Input
                || o.getSource().scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Output && externalRelations.stream().anyMatch(i -> o.getSource() == i.getSource())).collect(Collectors.toList());

        var edges = new Vector<Edge11<UnitRelationship.Variable, UnitRelationship.Relation.InternalOrExternal>>();
        externalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(o.getSource(), e, o.getOrigin()));
        }));
        internalRelations.forEach(o -> o.getTargets().values().forEach(e -> {
            edges.add(new Edge11(e, o.getSource(), o.getOrigin()));
        }));

        var graphSolver = new TarjanGraph(CollectionConverters.IterableHasAsScala(edges).asScala());

        var topologicalOrderToInstantiate = graphSolver.topologicalSort();
        if (topologicalOrderToInstantiate instanceof CyclicDependencyResult) {
            CyclicDependencyResult cycles = (CyclicDependencyResult) topologicalOrderToInstantiate;
            throw new UnfoldException("Cycles are present in the systems: " + cycles.cycle());
        }

        return scala.jdk.javaapi.CollectionConverters.asJava(((AcyclicDependencyResult) topologicalOrderToInstantiate).totalOrder());
    }
}
