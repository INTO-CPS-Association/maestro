package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class TypeConverterPlugin implements IMaestroExpansionPlugin {

    final AFunctionDeclaration convertBoolean2Real = newAFunctionDeclaration(newAIdentifier("convertBoolean2Real"),
            Arrays.asList(newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newABoleanPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    final AFunctionDeclaration convertBoolean2Integer = newAFunctionDeclaration(newAIdentifier("convertBoolean2Integer"),
            Arrays.asList(newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newABoleanPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    final AFunctionDeclaration convertInteger2Boolean = newAFunctionDeclaration(newAIdentifier("convertInteger2Boolean"),
            Arrays.asList(newAFormalParameter(newABoleanPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newAIntNumericPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    final AFunctionDeclaration convertReal2Boolean = newAFunctionDeclaration(newAIdentifier("convertReal2Boolean"),
            Arrays.asList(newAFormalParameter(newABoleanPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newARealNumericPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    final AFunctionDeclaration convertInteger2Real = newAFunctionDeclaration(newAIdentifier("convertInteger2Real"),
            Arrays.asList(newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newAIntNumericPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    final AFunctionDeclaration convertReal2Integer = newAFunctionDeclaration(newAIdentifier("convertReal2Integer"),
            Arrays.asList(newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("to")),
                    newAFormalParameter(new AReferenceType(newARealNumericPrimitiveType()), newAIdentifier("from"))), newAVoidType());

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(convertBoolean2Real, convertBoolean2Integer, convertInteger2Boolean, convertReal2Boolean).collect(Collectors.toSet());
    }

    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {

        if (getDeclaredUnfoldFunctions().contains(declaredFunction)) {

            if (formalArguments == null || formalArguments.size() != declaredFunction.getFormals().size()) {
                throw new ExpandException("Invalid args");
            }

            if (env == null) {
                throw new ExpandException("Simulation environment must not be null");
            }

            List<PStm> stms = new Vector<>();


            PStateDesignator to = null;

            PExp target = formalArguments.get(1);
            if (target instanceof AIdentifierExp) {
                to = newAIdentifierStateDesignator(((AIdentifierExp) target).getName());
            } else if (target instanceof AArrayIndexExp) {
                AArrayIndexExp indexExp = (AArrayIndexExp) target;
                LexIdentifier name = ((AIdentifierExp) indexExp.getArray()).getName();
                to = newAArayStateDesignator(newAIdentifierStateDesignator(name), (SLiteralExp) indexExp.getIndices().iterator().next());
            }

            final PStateDesignator targetDesignator = to;

            stms.add(createAssignStm(declaredFunction, targetDesignator, formalArguments));

            return stms;
        }
        throw new ExpandException("Unknown function" + declaredFunction);
    }

    private PStm createAssignStm(AFunctionDeclaration declaredFunction, PStateDesignator targetDesignator,
            List<PExp> formalArguments) throws ExpandException {
        if (convertBoolean2Real.equals(declaredFunction)) {
            Function<Double, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newARealLiteralExp(val));
            return newIf(formalArguments.get(0), set.apply(1.0), set.apply(0.0));
        } else if (convertBoolean2Integer.equals(declaredFunction)) {
            Function<Integer, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newAIntLiteralExp(val));
            return newIf(formalArguments.get(0), set.apply(1), set.apply(0));
        } else if (convertInteger2Boolean.equals(declaredFunction)) {
            Function<Boolean, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newABoolLiteralExp(val));
            return newIf(formalArguments.get(0), set.apply(true), set.apply(false));
        } else if (convertReal2Boolean.equals(declaredFunction)) {
            Function<Boolean, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newABoolLiteralExp(val));
            return newIf(formalArguments.get(0), set.apply(true), set.apply(false));
        } else if (convertInteger2Real.equals(declaredFunction)) {
            Function<Double, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newARealLiteralExp(val));
            return newIf(formalArguments.get(0), set.apply(1.0), set.apply(0.0));
            //TODO look at the conversion
            //return set.apply(formalArguments.get(0).clone());
        } else if (convertReal2Integer.equals(declaredFunction)) {
            Function<Integer, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newAIntLiteralExp(val));
  /*          if (formalArguments.get(0) instanceof AIntNumericPrimitiveType) {
                ((AIntNumericPrimitiveType) formalArguments.get(0));

    */
            return newIf(formalArguments.get(0), set.apply(1), set.apply(0));
        }
        throw new ExpandException("Unknown convert function" + declaredFunction);
    }


    @Override
    public boolean requireConfig() {
        return false;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName().substring(0, getClass().getSimpleName().indexOf("Plugin"));
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }


}
