package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.FmuVariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableUtil;
import org.intocps.maestro.typechecker.TypeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class BuilderUtil {
    final static Logger logger = LoggerFactory.getLogger(BuilderUtil.class);

    public static List<PStm> createTypeConvertingAssignment(PStateDesignator designator, PExp value, PType valueType, PType targetType) {

        List<PStm> statements = new Vector<>();
        TypeComparator typeComparator = new TypeComparator();
        if (typeComparator.compatible(targetType, valueType)) {
            statements.add(newAAssignmentStm(designator, value));
        } else {

            //   String varName = builder.getNameGenerator().getName();
            //   statements.add(newVariable(varName, targetType));

            if (typeComparator.compatible(newBoleanType(), valueType) && (typeComparator.compatible(newRealType(), targetType)) ||
                    typeComparator.compatible(newRealType(), targetType) || typeComparator.compatible(newRealType(), targetType)) {
                //bool to number
                PExp trueToken = newAIntLiteralExp(1);
                PExp falseToken = newAIntLiteralExp(0);

                if (typeComparator.compatible(newRealType(), targetType)) {
                    //Fix for the bad interpreter
                    trueToken = newARealLiteralExp(1d);
                    falseToken = newARealLiteralExp(0d);
                }

                statements.add(newIf(value, newAAssignmentStm(designator.clone(), trueToken), newAAssignmentStm(designator.clone(), falseToken)));
            } else if (typeComparator.compatible(newBoleanType(), targetType) && (typeComparator.compatible(newRealType(), valueType)) ||
                    typeComparator.compatible(newRealType(), valueType) || typeComparator.compatible(newRealType(), valueType)) {
                // number to bool
                statements.add(newAAssignmentStm(designator.clone(), newEqual(value, newAIntLiteralExp(1))));

            } else if ((typeComparator.compatible(newIntType(), valueType) || typeComparator.compatible(newUIntType(), valueType)) &&
                    (typeComparator.compatible(newIntType(), targetType) || typeComparator.compatible(newUIntType(), targetType))) {
                // (u)int to (u)int
                statements.add(newAAssignmentStm(designator, value));
            } else {
                //FIXME type conversion missing
                logger.error("No implementation for type conversion");
                //no solution so lets make the type checker report this error for now
                statements.add(newAAssignmentStm(designator, value));
            }


        }
        return statements;
    }

    public static List<PStm> createTypeConvertingAssignment(MablApiBuilder builder, FmiBuilder.Scope<PStm> scope, PStateDesignator designator,
            PExp value, PType valueType, PType targetType) {

        return createTypeConvertingAssignment(designator, value, valueType, targetType);
    }

    public static List<PExp> toExp(Object... args) {
        if (args == null || args.length == 0) {
            return new Vector<>();
        }
        List<PExp> expressions = new Vector<>();
        for (Object obj : args) {
            if (obj instanceof Double) {
                expressions.add(newARealLiteralExp((Double) obj));
            } else if (obj instanceof Integer) {
                expressions.add(newAIntLiteralExp((Integer) obj));
            } else if (obj instanceof Long) {
                expressions.add(newAUIntLiteralExp((Long) obj));
            } else if (obj instanceof Boolean) {
                expressions.add(newABoolLiteralExp((Boolean) obj));
            } else if (obj instanceof String) {
                expressions.add(newAStringLiteralExp((String) obj));
            } else if (obj instanceof FmuVariableFmi2Api) {
                expressions.add(VariableUtil.getAsExp((VariableFmi2Api) obj));
            } else if (obj instanceof ComponentVariableFmi2Api) {
                expressions.add(VariableUtil.getAsExp((VariableFmi2Api) obj));
            } else if (obj instanceof VariableFmi2Api) {
                expressions.add(VariableUtil.getAsExp((VariableFmi2Api) obj));
            } else if (obj instanceof Object[]) {
                //should check if this is suppose to be vargs
                Object[] arr = (Object[]) obj;
                for (Object o : arr) {
                    expressions.addAll(toExp(o));
                }
            }
        }
        return expressions;
    }
}
