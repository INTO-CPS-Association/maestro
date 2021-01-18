package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.AMaBLScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.values.AMablValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements Fmi2Builder<ASimulationSpecificationCompilationUnit> {

    static AMaBLScope rootScope;
    static Map<String, AMablVariable> specialVariables = new HashMap<>();
    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    private final AMaBLVariableCreator currentVariableCreator;

    private final AMablBooleanVariable globalExecutionContinue;
    private final AMablIntVariable globalFmiStatus;

    public MablApiBuilder(String... existingIdentifiers) {
        rootScope = new AMaBLScope(this);
        this.dynamicScope = new DynamicActiveBuilderScope(rootScope);
        this.currentVariableCreator = new AMaBLVariableCreator(dynamicScope, this);

        this.getDynamicScope().store(new AMablValue<>(newABoleanPrimitiveType(), false));
        if (existingIdentifiers.length > 0) {
            this.nameGenerator.identifiers.addAll(Arrays.asList(existingIdentifiers));
        }
        //create global variables
        globalExecutionContinue =
                (AMablBooleanVariable) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
        globalFmiStatus = (AMablIntVariable) createVariable(rootScope, newIntType(), null, "status");

    }

    public static AMablVariable getStatus() {
        return specialVariables.get("status");
    }

    public AMablBooleanVariable getGlobalExecutionContinue() {
        return globalExecutionContinue;
    }

    public AMablIntVariable getGlobalFmiStatus() {
        return globalFmiStatus;
    }

    private Variable createVariable(IMablScope scope, PType type, PExp initialValue, String... prefixes) {
        String name = nameGenerator.getName(prefixes);
        PStm var = newVariable(name, type, initialValue);
        scope.add(var);
        if (type instanceof ARealNumericPrimitiveType) {
            return new AMablDoubleVariable(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof ABooleanPrimitiveType) {
            return new AMablBooleanVariable(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AIntNumericPrimitiveType) {
            return new AMablIntVariable(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AStringPrimitiveType) {
            return new AMablStringVariable(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        }

        return new AMablVariable(var, type, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
    }

    public TagNameGenerator getNameGenerator() {
        return nameGenerator;
    }


    @Override
    public IMablScope getRootScope() {
        return MablApiBuilder.rootScope;
    }

    @Override
    public DynamicActiveBuilderScope getDynamicScope() {
        return this.dynamicScope;
    }


    @Override
    public Time getCurrentTime() {
        return null;
    }

    @Override
    public Time getTime(double time) {
        return null;
    }

    @Override
    public Value getCurrentLinkedValue(Port port) {
        return null;
    }

    @Override
    public TimeDeltaValue createTimeDeltaValue(double getMinimum) {
        return null;
    }


    @Override
    public AMaBLVariableCreator variableCreator() {
        return this.currentVariableCreator;
    }

    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        ABlockStm block = rootScope.getBlock().clone();

        //run post cleaning
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseABlockStm(ABlockStm node) throws AnalysisException {
                if (node.getBody().isEmpty()) {
                    if (node.parent() instanceof ABlockStm) {
                        ABlockStm pb = (ABlockStm) node.parent();
                        pb.getBody().remove(node);
                    } else if (node.parent() instanceof AIfStm) {
                        AIfStm ifStm = (AIfStm) node.parent();

                        if (ifStm.getElse() == node) {
                            ifStm.setElse(null);
                        }
                    }
                } else {
                    super.caseABlockStm(node);
                }

            }
        });

        ASimulationSpecificationCompilationUnit unit = new ASimulationSpecificationCompilationUnit();
        unit.setBody(block);
        unit.setFramework(Arrays.asList(newAIdentifier("FMI2")));

        AConfigFramework config = new AConfigFramework();
        config.setName(newAIdentifier("FMI2"));
        //config.setConfig(StringEscapeUtils.escapeJava(simulationEnvironment.));
        // unit.setFrameworkConfigs(Arrays.asList(config));
        unit.setImports(Arrays.asList(newAIdentifier("FMI2")));

        return unit;
    }

    public PExp getStartTime() {
        return null;
    }

    public PExp getEndTime() {
        return null;
    }
}
