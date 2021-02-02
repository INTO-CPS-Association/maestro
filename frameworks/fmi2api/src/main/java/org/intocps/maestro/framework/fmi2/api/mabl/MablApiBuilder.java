package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp> {

    static ScopeFmi2Api rootScope;
    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    private final VariableCreatorFmi2Api currentVariableCreator;
    private final BooleanVariableFmi2Api globalExecutionContinue;
    private final IntVariableFmi2Api globalFmiStatus;
    private final MablToMablAPI mablToMablAPI;
    private final MathBuilderFmi2Api mathBuilderApi;
    private final BooleanBuilderFmi2Api booleanBuilderApi;
    List<String> importedModules = new Vector<>();
    private DataWriter dataWriter;

    public MablApiBuilder() {
        rootScope = new ScopeFmi2Api(this);
        this.dynamicScope = new DynamicActiveBuilderScope(rootScope);
        this.currentVariableCreator = new VariableCreatorFmi2Api(dynamicScope, this);
        this.mablToMablAPI = new MablToMablAPI(this);
        this.mathBuilderApi = new MathBuilderFmi2Api(dynamicScope, this);
        this.booleanBuilderApi = new BooleanBuilderFmi2Api(dynamicScope, this);
        //        this.dataWriter = new DataWriter(dynamicScope, this);
        //        this.

        //create global variables
        globalExecutionContinue =
                (BooleanVariableFmi2Api) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
        globalFmiStatus = (IntVariableFmi2Api) createVariable(rootScope, newIntType(), null, "status");

    }

    public MablToMablAPI getMablToMablAPI() {
        return this.mablToMablAPI;
    }

    public DataWriter getDataWriter() {
        if (this.dataWriter == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule("DataWriter");
            this.dataWriter = new DataWriter(this.dynamicScope, this, runtimeModule);
        }

        return this.dataWriter;
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

    public MathBuilderFmi2Api getMathBuilder() {
        return this.mathBuilderApi;

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


    Pair<PStateDesignator, PExp> getDesignatorAndReferenceExp(PExp exp) {
        if (exp instanceof AArrayIndexExp) {
            AArrayIndexExp exp_ = (AArrayIndexExp) exp;
            // TODO
        } else if (exp instanceof AIdentifierExp) {
            AIdentifierExp exp_ = (AIdentifierExp) exp;
            return Pair.of(newAIdentifierStateDesignator(exp_.getName()), exp_);
        }

        throw new RuntimeException("Invalid expression of class: " + exp.getClass());
    }

    @Override
    public DoubleVariableFmi2Api getDoubleVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new DoubleVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());

    }

    @Override
    public IntVariableFmi2Api getIntVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new IntVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public StringVariableFmi2Api getStringVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new StringVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public BooleanVariableFmi2Api getBooleanVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new BooleanVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public FmuVariableFmi2Api getFmuVariableFrom(PExp exp) {
        return null;
    }

    @Override
    public PStm buildRaw() {
        return rootScope.getBlock().clone();
    }

    @Override
    public RuntimeModule<PStm> loadRuntimeModule(String name, Object... args) {
        return loadRuntimeModule(dynamicScope.getActiveScope(), name, args);
    }

    @Override
    public RuntimeModule<PStm> loadRuntimeModule(Scope<PStm> scope, String name, Object... args) {
        String varName = getNameGenerator().getName(name);
        List<PExp> argList = BuilderUtil.toExp(args);
        argList.add(0, newAStringLiteralExp(name));
        PStm var = newVariable(varName, newANameType(name), newALoadExp(argList));
        scope.add(var);
        RuntimeModuleVariable module =
                new RuntimeModuleVariable(var, newANameType(name), (IMablScope) scope, dynamicScope, this, newAIdentifierStateDesignator(varName),
                        newAIdentifierExp(varName));
        importedModules.add(name);
        return module;
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
        unit.setImports(Stream.concat(Stream.of(newAIdentifier("FMI2")), importedModules.stream().map(MableAstFactory::newAIdentifier))
                .collect(Collectors.toList()));

        return unit;

    }

    public FunctionBuilder getFunctionBuilder() {
        return new FunctionBuilder();
    }

    public BooleanBuilderFmi2Api getBooleanBuilder() {
        return new BooleanBuilderFmi2Api(this.dynamicScope, this);
    }
}
