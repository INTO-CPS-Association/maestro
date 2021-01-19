package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.Collections;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit> {

    static ScopeFmi2Api rootScope;
    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    private final VariableCreatorFmi2Api currentVariableCreator;

    private final BooleanVariableFmi2Api globalExecutionContinue;
    private final IntVariableFmi2Api globalFmiStatus;

    public MablApiBuilder() {
        rootScope = new ScopeFmi2Api(this);
        this.dynamicScope = new DynamicActiveBuilderScope(rootScope);
        this.currentVariableCreator = new VariableCreatorFmi2Api(dynamicScope, this);

        this.getDynamicScope().store(new ValueFmi2Api<>(newABoleanPrimitiveType(), false));

        //create global variables
        globalExecutionContinue =
                (BooleanVariableFmi2Api) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
        globalFmiStatus = (IntVariableFmi2Api) createVariable(rootScope, newIntType(), null, "status");

    }


    public BooleanVariableFmi2Api getGlobalExecutionContinue() {
        return globalExecutionContinue;
    }

    public IntVariableFmi2Api getGlobalFmiStatus() {
        return globalFmiStatus;
    }

    @SuppressWarnings("rawtypes")
    private Variable createVariable(IMablScope scope, PType type, PExp initialValue, String... prefixes) {
        String name = nameGenerator.getName(prefixes);
        PStm var = newVariable(name, type, initialValue);
        scope.add(var);
        if (type instanceof ARealNumericPrimitiveType) {
            return new DoubleVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof ABooleanPrimitiveType) {
            return new BooleanVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AIntNumericPrimitiveType) {
            return new IntVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AStringPrimitiveType) {
            return new StringVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        }

        return new VariableFmi2Api(var, type, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
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
    public <V, T> Variable<T, V> getCurrentLinkedValue(Port port) {
        PortFmi2Api mp = (PortFmi2Api) port;
        if (mp.getSharedAsVariable() == null) {
            return null;
        }
        return mp.getSharedAsVariable();
    }


    @Override
    public VariableCreatorFmi2Api variableCreator() {
        return this.currentVariableCreator;
    }

    @Override
    public PStm buildRaw() {
        return rootScope.getBlock().clone();
    }

    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        ABlockStm block = rootScope.getBlock().clone();

        //Post cleaning: Remove empty block statements
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
        unit.setFramework(Collections.singletonList(newAIdentifier("FMI2")));

        AConfigFramework config = new AConfigFramework();
        config.setName(newAIdentifier("FMI2"));
        //config.setConfig(StringEscapeUtils.escapeJava(simulationEnvironment.));
        // unit.setFrameworkConfigs(Arrays.asList(config));
        unit.setImports(Collections.singletonList(newAIdentifier("FMI2")));

        return unit;

    }

    public PExp getStartTime() {
        return null;
    }

    public PExp getEndTime() {
        return null;
    }
}
