package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.*;
import static org.intocps.maestro.plugin.DataExchangeHandler.getBufferName;

public class DataWriterHandler implements GeneratorComponent {

    private final String data_HeadersIdentifier = "data_headers";
    private final String dataWriter = "dataWriter";
    private final String data_valuesIdentifier = "data_values";
    private final String data_configuration = "dataWriter_configuration";
    Map<RelationVariable, PExp> csvFields;

    public List<PStm> allocate(Set<Fmi2SimulationEnvironment.Relation> inputRelations,
            Map<LexIdentifier, Map<Fmi2ModelDescription.Types, List<Fmi2ModelDescription.ScalarVariable>>> outputs, Fmi2SimulationEnvironment env) {
        List<PStm> statements = new Vector<>();
        List<String> variableNames = new Vector<>();

        Function<RelationVariable, String> getLogName = k -> k.instance.getText() + "." + k.getScalarVariable().getName();

        csvFields = inputRelations.stream().map(r -> r.getTargets().values().stream().findFirst()).filter(Optional::isPresent).map(Optional::get)
                .flatMap(h -> {
                    List<RelationVariable> outputs_ = env.getVariablesToLog(h.scalarVariable.instance.getText());
                    //outputs_.add(h.scalarVariable);
                    return outputs_.stream();
                    //return h.scalarVariable;
                }).sorted(Comparator.comparing(getLogName::apply)).collect(Collectors.toMap(l -> l, r -> {


                    //the relation should be a one to one relation so just take the first one
                    RelationVariable fromVar = r;
                    PExp from = arrayGet(getBufferName(fromVar.instance, fromVar.getScalarVariable().type.type, DataExchangeHandler.UsageType.Out),
                            outputs.get(fromVar.instance).get(fromVar.getScalarVariable().getType().type).stream()
                                    .map(Fmi2ModelDescription.ScalarVariable::getName).collect(Collectors.toList())
                                    .indexOf(fromVar.scalarVariable.getName()));
                    return from;

                }, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        variableNames.addAll(csvFields.keySet().stream().map(k -> {

            FrameworkUnitInfo info = env.getUnitInfo(k.instance, Framework.FMI2);

            Stream<String> nameComponents = Stream.of(k.instance.getText(), k.getScalarVariable().getName());

            if (info instanceof ComponentInfo) {
                nameComponents = Stream.concat(Stream.of(((ComponentInfo) info).fmuIdentifier), nameComponents);
            }
            return nameComponents.collect(Collectors.joining("."));
        }).collect(Collectors.toList()));

        statements.add(newVariable(this.data_HeadersIdentifier, newAStringPrimitiveType(),
                variableNames.stream().map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList())));


        statements.add(newVariable(this.data_configuration, newANameType("DataWriterConfig"),
                call(this.dataWriter, "writeHeader", newAIdentifierExp(this.data_HeadersIdentifier))));


        Consumer<List<PStm>> declareCsvBuffer = list -> {
            list.add(newALocalVariableStm(
                    newAVariableDeclaration(newAIdentifier(this.data_valuesIdentifier), newAArrayType(newAUnknownType()), variableNames.size(),
                            newAArrayInitializer(csvFields.values().stream().map(PExp::clone).collect(Collectors.toList())))));
        };

        declareCsvBuffer.accept(statements);

        return statements;
    }


    public List<PStm> write() {
        Consumer<List<PStm>> logCsvValues = list -> {
            List<PExp> args = new Vector<>();
            args.addAll(Arrays.asList(newAIdentifierExp(this.data_configuration), newAIdentifierExp("time")));
            csvFields.values().forEach(v -> args.add(v.clone()));
            list.add(newExpressionStm(newACallExp(newAIdentifierExp(this.dataWriter), newAIdentifier("writeDataPoint"), args)));
        };

        List<PStm> statements = new Vector<>();
        logCsvValues.accept(statements);
        return statements;
    }

    @Override
    public List<PStm> deallocate() {
        return Collections.singletonList(newExpressionStm(newACallExp(newAIdentifierExp(this.dataWriter), newAIdentifier("close"), Arrays.asList())));
    }
}
