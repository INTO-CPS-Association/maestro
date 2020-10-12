package org.intocps.maestro.plugin.verificationsuite.PrologVerifier;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.intocps.maestro.plugin.ExpandException;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PrologGenerator {
    public String CreateInitOperationOrder(List<FmiSimulationEnvironment.Variable> instantiationOrder) {
        StringBuilder initOrder = new StringBuilder();
        instantiationOrder.forEach(o -> {
            try {
                initOrder.append(String.format("%s(%s, %s),", getMethod(o), o.scalarVariable.getInstance().getText().toLowerCase(),
                        o.scalarVariable.getScalarVariable().getName().toLowerCase()));
            } catch (ExpandException e) {
                e.printStackTrace();
            }
        });

        return fixListFormat(initOrder).toString();
    }

    public String CreateFMUs(Set<FmiSimulationEnvironment.Relation> relations) {
        StringBuilder fmuString = new StringBuilder();
        var fmuList = relations.stream().map(o -> o.getSource().scalarVariable.getInstance()).collect(Collectors.toSet());
        fmuList.forEach(fmu -> {
            fmuString.append(String.format("fmu(%s, %s, %s),", fmu.getText().toLowerCase(), getInPorts(relations, fmu), getOutPorts(relations, fmu)));
        });

        return fixListFormat(fmuString).toString();
    }

    public String CreateConnections(List<FmiSimulationEnvironment.Relation> relations) {
        StringBuilder connections = new StringBuilder();
        relations.forEach(relation -> {
            relation.getTargets().values().forEach(target -> {
                connections.append(String.format("connect(%s,%s, %s, %s),", relation.getSource().scalarVariable.getInstance().getText().toLowerCase(),
                        relation.getSource().scalarVariable.getScalarVariable().getName().toLowerCase(),
                        target.scalarVariable.getInstance().getText().toLowerCase(),
                        target.scalarVariable.getScalarVariable().getName().toLowerCase()));
            });
        });
        return fixListFormat(connections).toString();
    }

    private StringBuilder fixListFormat(StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        stringBuilder.insert(0, "[");
        stringBuilder.append("]");
        return stringBuilder;
    }

    private String getInPorts(Set<FmiSimulationEnvironment.Relation> relations, LexIdentifier fmu) {
        StringBuilder inPorts = new StringBuilder();
        var inputPorts = relations.stream().map(p -> p.getTargets().values()).collect(Collectors.toList()).stream().flatMap(Collection::stream)
                .collect(Collectors.toSet()).stream().filter(o -> o.scalarVariable.getInstance().getText().equals(fmu.getText()))
                .collect(Collectors.toSet());

        inputPorts.forEach(port -> {
            inPorts.append(String.format("port(%s, delayed),", port.scalarVariable.getScalarVariable().getName().toLowerCase()));
        });

        return fixListFormat(inPorts).toString();
    }

    private String getOutPorts(Set<FmiSimulationEnvironment.Relation> relations, LexIdentifier fmu) {
        StringBuilder outPorts = new StringBuilder();
        var externalRelations = relations.stream().filter(o -> o.getOrigin() == FmiSimulationEnvironment.Relation.InternalOrExternal.External)
                .collect(Collectors.toSet());
        var internalRelations = relations.stream().filter(o -> o.getOrigin() == FmiSimulationEnvironment.Relation.InternalOrExternal.Internal)
                .collect(Collectors.toSet());
        var outputPorts = externalRelations.stream().filter(p -> p.getSource().scalarVariable.getInstance() == fmu).collect(Collectors.toSet());

        outputPorts.forEach(port -> {
            outPorts.append(String.format("port(%s, %s),", port.getSource().scalarVariable.getScalarVariable().getName().toLowerCase(),
                    getInternalDependencies(port.getSource(), internalRelations)));
        });

        return fixListFormat(outPorts).toString();
    }

    private String getInternalDependencies(FmiSimulationEnvironment.Variable source, Set<FmiSimulationEnvironment.Relation> internalRelations) {
        var sources =
                internalRelations.stream().filter(rel -> rel.getSource() == source).map(o -> o.getTargets().values()).collect(Collectors.toSet())
                        .stream().flatMap(Collection::stream).collect(Collectors.toSet());

        StringBuilder internalConnections = new StringBuilder();
        sources.forEach(s -> {
            internalConnections.append(String.format("%s,", s.scalarVariable.getScalarVariable().getName().toLowerCase()));
        });

        return fixListFormat(internalConnections).toString();
    }

    private String getMethod(FmiSimulationEnvironment.Variable variable) throws ExpandException {
        if (variable.scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Output) {
            return "getOut";
        } else if (variable.scalarVariable.getScalarVariable().causality == ModelDescription.Causality.Input) {
            return "setIn";
        } else {
            throw new ExpandException("Unknown causality of port");
        }
    }
}
