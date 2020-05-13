package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

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
public class TypeConverterPlugin implements IMaestroUnfoldPlugin {

    final AFunctionDeclaration convertBoolean2Real = newAFunctionDeclaration(newAIdentifier("convertBoolean2Real"),
            Arrays.asList(newAFormalParameter(newARealNumericPrimitiveType(), newAIdentifier("from")),
                    newAFormalParameter(new AReferenceType(newABoleanPrimitiveType()), newAIdentifier("to"))), newAVoidType());


    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(convertBoolean2Real).collect(Collectors.toSet());
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException {

        if (convertBoolean2Real == declaredFunction) {

            if (formalArguments == null || formalArguments.size() != convertBoolean2Real.getFormals().size()) {
                throw new UnfoldException("Invalid args");
            }

            if (env == null) {
                throw new UnfoldException("Simulation environment must not be null");
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

            Function<Double, PStm> set = val -> newAAssignmentStm(targetDesignator.clone(), newARealLiteralExp(val));

            stms.add(newIf(formalArguments.get(0), set.apply(1.0), set.apply(0.0)));

            return newABlockStm(stms);
        }
        throw new UnfoldException("Unknown function" + declaredFunction);
    }


    private String getFmiGetName(ModelDescription.Types type, UsageType usage) {

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

    LexIdentifier getStateName(LexIdentifier comp) {
        return newAIdentifier(comp.getText() + "State");
    }

    SPrimitiveType convert(ModelDescription.Types type) {
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

    LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }


    LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    private String getTypeId(SPrimitiveType type) {
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

    LexIdentifier getVrefName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {

        return newAIdentifier(comp.getText() + "Vref" + getTypeId(convert(type)) + usage);
    }

    @Override
    public boolean requireConfig() {
        return false;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return new FixedstepConfig(new ObjectMapper().readValue(is, Integer.class));
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    enum UsageType {
        In,
        Out
    }

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
