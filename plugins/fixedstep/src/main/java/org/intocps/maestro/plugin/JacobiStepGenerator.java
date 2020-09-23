package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class JacobiStepGenerator {
    static String getFmiGetName(ModelDescription.Types type, UsageType usage) {

        String fun = usage == UsageType.In ? "set" : "get";
        switch (type) {
            case Boolean:
                return fun + "Boolean";
            case Real:
                return fun + "Real";
            case Integer:
                return fun + "Integer";
            case String:
                return fun + "String";
            case Enumeration:
            default:
                return null;
        }
    }

    static SPrimitiveType convert(ModelDescription.Types type) {
        switch (type) {

            case Boolean:
                return newABoleanPrimitiveType();
            case Real:
                return newARealNumericPrimitiveType();
            case Integer:
                return newAIntNumericPrimitiveType();
            case String:
                return newAStringPrimitiveType();
            case Enumeration:
            default:
                return null;
        }
    }

    static LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }

    static LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    static String getTypeId(SPrimitiveType type) {
        String t = type.getClass().getSimpleName();

        if (type instanceof ARealNumericPrimitiveType) {
            t = "R";
        } else if (type instanceof AIntNumericPrimitiveType) {
            t = "I";
        } else if (type instanceof AStringPrimitiveType) {
            t = "S";
        } else if (type instanceof ABooleanPrimitiveType) {
            t = "B";
        }
        return t;
    }

    //    final static String fixedStepStatus = "fix_status";
    //    final Function<LexIdentifier, PExp> getCompStatusExp = comp -> arrayGet(fixedStepStatus, componentNames.indexOf(comp));
    //    final Function<LexIdentifier, PStateDesignator> getCompStatusDesignator =
    //            comp -> newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(fixedStepStatus)),
    //                    newAIntLiteralExp(componentNames.indexOf(comp)));
    //    private LexIdentifier compIndexVar;
    //    BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus = (inLoopAndMessage, list) -> {
    //        List<PStm> body = new Vector<>(Arrays.asList(newExpressionStm(
    //                call("logger", "log", newAIntLiteralExp(4), newAStringLiteralExp(inLoopAndMessage.getValue() + " %d "),
    //                        arrayGet(fixedStepStatus, newAIdentifierExp((LexIdentifier) compIndexVar.clone())))),
    //                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")), newABoolLiteralExp(false))));
    //
    //        if (inLoopAndMessage.getKey()) {
    //            body.add(newBreak());
    //        }
    //
    //        list.getValue().add(newIf(newOr((newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_ERROR))),
    //                (newEqual(getCompStatusExp.apply(list.getKey()), newAIntLiteralExp(FMI_FATAL)))), newABlockStm(body), null));
    //    };
    //
    //    public List<PStm> allocateGlobalState(Set<UnitRelationship.Relation> inputRelations, PExp stepSize, PExp startTime, PExp endTime) {
    //
    //        List<PStm> statements = new Vector<>();
    //        LexIdentifier end = newAIdentifier("end");
    //
    //        statements.add(newVariable(end, newAIntNumericPrimitiveType(), newMinusExp(endTime, stepSize)));
    //
    //        LexIdentifier time = newAIdentifier("time");
    //        statements.add(newVariable(time, newARealNumericPrimitiveType(), startTime));
    //        String fix_stepSize = "fix_stepSize";
    //        statements.add(newVariable(fix_stepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));
    //
    //        String fix_recoveryStepSize = "fix_recoveryStepSize";
    //        statements.add(newVariable(fix_recoveryStepSize, newARealNumericPrimitiveType(), newARealLiteralExp(0d)));
    //
    //        String fix_recovering = "fix_recovering";
    //        statements.add(newVariable(fix_recovering, newABoleanPrimitiveType(), newABoolLiteralExp(false)));
    //
    //        LexIdentifier fixedStepOverallStatus = newAIdentifier("fix_global_status");
    //        statements.add(newVariable(fixedStepOverallStatus, newABoleanPrimitiveType(), newABoolLiteralExp(false)));
    //
    //        compIndexVar = newAIdentifier("fix_comp_index");
    //
    //        statements.add(newVariable(compIndexVar, newAIntNumericPrimitiveType(), newAIntLiteralExp(0)));
    //
    //
    //        Map<LexIdentifier, Map<ModelDescription.Types, List<ModelDescription.ScalarVariable>>> inputs =
    //                inputRelations.stream().map(r -> r.getSource().scalarVariable.instance).distinct().collect(Collectors.toMap(Function.identity(),
    //                        s -> inputRelations.stream().filter(r -> r.getSource().scalarVariable.instance.equals(s))
    //                                .map(r -> r.getSource().scalarVariable.getScalarVariable()).collect(Collectors.groupingBy(sv -> sv.getType().type))));
    //
    //        return statements;
    //    }
    static enum UsageType {
        In,
        Out
    }
}
