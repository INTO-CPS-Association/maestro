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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp> {

    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    final MablSettings settings;
    private final ScopeFmi2Api rootScope;
    private final ScopeFmi2Api mainErrorHandlingScope;
    private final VariableCreatorFmi2Api currentVariableCreator;
    private final BooleanVariableFmi2Api globalExecutionContinue;
    private final IntVariableFmi2Api globalFmiStatus;
    private final MablToMablAPI mablToMablAPI;
    private final Map<FmiStatus, IntVariableFmi2Api> fmiStatusVariables;
    List<String> importedModules = new Vector<>();
    private MathBuilderFmi2Api mathBuilderApi;
    private BooleanBuilderFmi2Api booleanBuilderApi;
    private DataWriter dataWriter;
    private LoggerFmi2Api runtimeLogger;

    public MablApiBuilder() {
        this(new MablSettings());
    }

    public MablApiBuilder(MablSettings settings) {
        this.settings = settings;
        rootScope = new ScopeFmi2Api(this);

        fmiStatusVariables = new HashMap<>();
        if (settings.fmiErrorHandlingEnabled) {
            fmiStatusVariables.put(FmiStatus.FMI_OK, rootScope.store("FMI_STATUS_OK", 0));
            fmiStatusVariables.put(FmiStatus.FMI_WARNING, rootScope.store("FMI_STATUS_WARNING", 1));
            fmiStatusVariables.put(FmiStatus.FMI_DISCARD, rootScope.store("FMI_STATUS_DISCARD", 2));
            fmiStatusVariables.put(FmiStatus.FMI_ERROR, rootScope.store("FMI_STATUS_ERROR", 3));
            fmiStatusVariables.put(FmiStatus.FMI_FATAL, rootScope.store("FMI_STATUS_FATAL", 4));
            fmiStatusVariables.put(FmiStatus.FMI_PENDING, rootScope.store("FMI_STATUS_PENDING", 5));
        }

        //create global variables
        globalExecutionContinue =
                (BooleanVariableFmi2Api) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
        globalFmiStatus = (IntVariableFmi2Api) createVariable(rootScope, newIntType(), null, "status");

        mainErrorHandlingScope = rootScope.enterWhile(globalExecutionContinue.toPredicate());

        this.dynamicScope = new DynamicActiveBuilderScope(mainErrorHandlingScope);
        this.currentVariableCreator = new VariableCreatorFmi2Api(dynamicScope, this);
        this.mablToMablAPI = new MablToMablAPI(this);


    }

    public MablSettings getSettings() {
        return settings;
    }

    public IntVariableFmi2Api getFmiStatusConstant(FmiStatus status) {
        if (!settings.fmiErrorHandlingEnabled) {
            throw new IllegalStateException("Fmi error handling feature not enabled");
        }
        return this.fmiStatusVariables.get(status);
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
        if (this.mathBuilderApi == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule("Math");
            this.mathBuilderApi = new MathBuilderFmi2Api(this.dynamicScope, this, runtimeModule);
        }
        return this.mathBuilderApi;

    }

    @Override
    public IMablScope getRootScope() {
        return rootScope;
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

        AtomicReference<ABlockStm> errorHandingBlock = new AtomicReference<>();
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseAWhileStm(AWhileStm node) throws AnalysisException {
                if (node.getBody().equals(mainErrorHandlingScope.getBlock())) {
                    errorHandingBlock.set(((ABlockStm) node.getBody()));

                }
                super.caseAWhileStm(node);
            }
        });

        if (runtimeLogger != null) {
            //attempt a syntactic comparison to find the load in the clone
            VariableFmi2Api loggerVar = (VariableFmi2Api) runtimeLogger.module;
            block.apply(new DepthFirstAnalysisAdaptor() {


                @Override
                public void defaultInPStm(PStm node) throws AnalysisException {
                    if (node.equals(loggerVar.getDeclaringStm())) {
                        if (node.parent() instanceof ABlockStm) {
                            //this is the scope where the logger is loaded. Check for unload
                            LinkedList<PStm> body = ((ABlockStm) node.parent()).getBody();
                            boolean unloadFound = false;
                            for (int i = body.indexOf(node); i < body.size(); i++) {
                                PStm stm = body.get(i);
                                //newExpressionStm(newUnloadExp(Arrays.asList(getReferenceExp().clone())
                                if (stm instanceof AExpressionStm && ((AExpressionStm) stm).getExp() instanceof AUnloadExp) {
                                    AUnloadExp unload = (AUnloadExp) ((AExpressionStm) stm).getExp();
                                    if (!unload.getArgs().isEmpty() && unload.getArgs().get(0).equals(loggerVar.getReferenceExp())) {
                                        unloadFound = true;
                                    }
                                }
                            }
                            if (!unloadFound) {
                                body.add(newExpressionStm(newUnloadExp(Collections.singletonList(loggerVar.getReferenceExp().clone()))));
                            }
                        }
                    }
                }

            });
        }

        errorHandingBlock.get().getBody().add(newBreak());

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

        if (this.booleanBuilderApi == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule("BooleanLogic");
            this.booleanBuilderApi = new BooleanBuilderFmi2Api(this.dynamicScope, this, runtimeModule);
        }
        return this.booleanBuilderApi;
    }

    public LoggerFmi2Api getLogger() {
        if (this.runtimeLogger == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule("Logger");
            this.runtimeLogger = new LoggerFmi2Api(this, runtimeModule);
        }

        return this.runtimeLogger;
    }

    public enum FmiStatus {
        FMI_OK,
        FMI_WARNING,
        FMI_DISCARD,
        FMI_ERROR,
        FMI_FATAL,
        FMI_PENDING
    }

    public static class MablSettings {
        public final boolean fmiErrorHandlingEnabled = true;
    }
}
