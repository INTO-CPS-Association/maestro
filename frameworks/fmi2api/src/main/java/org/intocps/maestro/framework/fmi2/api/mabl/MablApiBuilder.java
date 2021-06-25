package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.DerivativeEstimator;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    private final MablSettings settings;
    private final Map<FmiStatus, IntVariableFmi2Api> fmiStatusVariables;
    private final ScopeFmi2Api mainErrorHandlingScope;
    private final Set<String> externalLoadedModuleIdentifier = new HashSet<>();
    List<String> importedModules = new Vector<>();
    List<RuntimeModuleVariable> loadedModules = new Vector<>();
    private MathBuilderFmi2Api mathBuilderApi;
    private BooleanBuilderFmi2Api booleanBuilderApi;
    private DataWriter dataWriter;
    private RealTime realTime;
    private VariableStep variableStep;
    private DerivativeEstimator derivativeEstimator;
    private LoggerFmi2Api runtimeLogger;
    private ConsolePrinter consolePrinter;
    private ExecutionEnvironmentFmi2Api executionEnvironment;


    public MablApiBuilder() {
        this(new MablSettings(), null);
    }

    public MablApiBuilder(MablSettings settings) {
        this(settings, null);
    }


    /**
     * Create a MablApiBuilder
     *
     * @param settings
     */
    public MablApiBuilder(MablSettings settings, INode lastNodePriorToBuilderTakeOver) {

        boolean createdFromExistingSpec = lastNodePriorToBuilderTakeOver != null;


        this.settings = settings;
        rootScope = new ScopeFmi2Api(this);

        fmiStatusVariables = new HashMap<>();
        if (settings.fmiErrorHandlingEnabled) {
            if (createdFromExistingSpec) {
                //create new variables
                Function<String, IntVariableFmi2Api> f = (str) -> new IntVariableFmi2Api(null, null, null, null, newAIdentifierExp(str));
                for (FmiStatus s : FmiStatus.values()) {
                    //if not existing then create
                    AVariableDeclaration decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, s.name());
                    if (decl == null) {
                        //create the status as it was not found
                        fmiStatusVariables.put(s, rootScope.store(s.name(), s.getValue()));
                    } else {
                        //if exists then link to previous declaration
                        fmiStatusVariables.put(s, f.apply(s.name()));
                    }
                }
            }
        }

        String global_execution_continue_varname = "global_execution_continue";
        String status_varname = "status";

        if (createdFromExistingSpec) {

            AVariableDeclaration decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, "global_execution_continue");
            if (decl == null) {
                globalExecutionContinue = rootScope.store(global_execution_continue_varname, true);
            } else {
                globalExecutionContinue =
                        (BooleanVariableFmi2Api) createVariableExact(rootScope, newBoleanType(), newABoolLiteralExp(true), decl.getName().getText(),
                                true);
            }

            decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, status_varname);
            if (decl == null) {
                globalFmiStatus = rootScope.store(status_varname, FmiStatus.FMI_OK.getValue());
            } else {
                globalFmiStatus = (IntVariableFmi2Api) createVariableExact(rootScope, newIntType(), null, decl.getName().getText(), true);
            }

        } else {

            globalExecutionContinue = rootScope.store(global_execution_continue_varname, true);
            globalFmiStatus = rootScope.store(status_varname, FmiStatus.FMI_OK.getValue());
            //            globalExecutionContinue =
            //                    (BooleanVariableFmi2Api) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
            //            globalFmiStatus = (IntVariableFmi2Api) createVariable(rootScope, newIntType(), null, "status");
        }

        mainErrorHandlingScope = rootScope.enterWhile(globalExecutionContinue.toPredicate());
        this.dynamicScope = new DynamicActiveBuilderScope(mainErrorHandlingScope);
        this.currentVariableCreator = new VariableCreatorFmi2Api(dynamicScope, this);
        this.mablToMablAPI = new MablToMablAPI(this);

        if (createdFromExistingSpec) {
            AVariableDeclaration decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, "logger");
            if (decl != null) {
                this.getMablToMablAPI().createExternalRuntimeLogger();
            }
        }


    }

    public void setRuntimeLogger(LoggerFmi2Api runtimeLogger) {
        this.runtimeLogger = runtimeLogger;
    }

    public MablSettings getSettings() {
        return this.settings;
    }

    public IntVariableFmi2Api getFmiStatusConstant(FmiStatus status) {
        //if (!settings.fmiErrorHandlingEnabled) {
        //   throw new IllegalStateException("Fmi error handling feature not enabled");
        // }

        if (!this.fmiStatusVariables.containsKey(status)) {
            switch (status) {
                case FMI_OK: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_OK.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_OK, var);
                }
                break;
                case FMI_WARNING: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_WARNING.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_WARNING, var);
                    break;
                }
                case FMI_DISCARD: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_DISCARD.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_DISCARD, var);
                }
                break;
                case FMI_ERROR: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_ERROR.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_ERROR, var);
                    break;
                }
                case FMI_FATAL: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_FATAL.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_FATAL, var);
                    break;
                }
                case FMI_PENDING: {
                    IntVariableFmi2Api var = rootScope.store(status.name(), FmiStatus.FMI_PENDING.getValue());
                    //relocate to top of scope
                    rootScope.addAfter(getGlobalExecutionContinue().getDeclaringStm(), var.getDeclaringStm());
                    fmiStatusVariables.put(FmiStatus.FMI_PENDING, var);
                    break;
                }
            }

        }

        return this.fmiStatusVariables.get(status);
    }

    public MablToMablAPI getMablToMablAPI() {
        return this.mablToMablAPI;
    }

    public VariableStep getVariableStep(VariableFmi2Api config) {
        if (this.variableStep == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule(this.mainErrorHandlingScope, "VariableStep", config);
            this.variableStep = new VariableStep(this.dynamicScope, this, runtimeModule);
        }
        return this.variableStep;
    }

    public DerivativeEstimator getDerivativeEstimator() {
        if (this.derivativeEstimator == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule(this.mainErrorHandlingScope, "DerivativeEstimator");
            this.derivativeEstimator = new DerivativeEstimator(this.dynamicScope, this, runtimeModule);
        }
        return this.derivativeEstimator;
    }

    public DataWriter getDataWriter() {
        if (this.dataWriter == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule(this.mainErrorHandlingScope, "DataWriter");
            this.dataWriter = new DataWriter(this.dynamicScope, this, runtimeModule);
        }

        return this.dataWriter;
    }

    public ConsolePrinter getConsolePrinter() {
        if (this.consolePrinter == null) {
            RuntimeModule<PStm> runtimeModule =
                    this.loadRuntimeModule(this.mainErrorHandlingScope, (s, var) -> ((ScopeFmi2Api) s).getBlock().getBody().add(0, var),
                            "ConsolePrinter");
            this.consolePrinter = new ConsolePrinter(this, runtimeModule);
        }

        return this.consolePrinter;
    }

    public RealTime getRealTimeModule() {
        if (this.realTime == null) {
            RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule(this.mainErrorHandlingScope, "RealTime");
            this.realTime = new RealTime(this.dynamicScope, this, runtimeModule);
        }
        return this.realTime;
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
        return createVariableExact(scope, type, initialValue, name, false);
    }

    private Variable createVariableExact(IMablScope scope, PType type, PExp initialValue, String name, boolean external) {
        PStm var = newVariable(name, type, initialValue);
        if (!external) {
            scope.add(var);
        }
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
            RuntimeModuleVariable runtimeModule = this.loadRuntimeModule("Math");

            this.mathBuilderApi = new MathBuilderFmi2Api(this.dynamicScope, this, runtimeModule.getReferenceExp());
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
    public PStm buildRaw() throws AnalysisException {
        SBlockStm block = rootScope.getBlock().clone();
        if (block == null) {
            return null;
        }
        SBlockStm errorHandlingBlock = this.getErrorHandlingBlock(block);
        if (errorHandlingBlock == null) {
            return null;
        }


        /**
         * Automatically created unloads for all modules loaded by the builder.
         */
        for (RuntimeModuleVariable module : this.loadedModules) {
            VariableFmi2Api var = module;
            block.apply(new DepthFirstAnalysisAdaptor() {
                @Override
                public void defaultInPStm(PStm node) throws AnalysisException {
                    if (node.equals(module.getDeclaringStm())) {
                        if (node.parent() instanceof SBlockStm) {
                            //this is the scope where the logger is loaded. Check for unload
                            LinkedList<PStm> body = ((SBlockStm) node.parent()).getBody();
                            boolean unloadFound = false;
                            for (int i = body.indexOf(node); i < body.size(); i++) {
                                PStm stm = body.get(i);
                                if (stm instanceof AExpressionStm && ((AExpressionStm) stm).getExp() instanceof AUnloadExp) {
                                    AUnloadExp unload = (AUnloadExp) ((AExpressionStm) stm).getExp();
                                    if (!unload.getArgs().isEmpty() && unload.getArgs().get(0).equals(module.getReferenceExp())) {
                                        unloadFound = true;
                                    }
                                }
                            }
                            if (!unloadFound) {
                                body.add(newIf(newNotEqual(module.getReferenceExp().clone(), newNullExp()),
                                        newExpressionStm(newUnloadExp(Collections.singletonList(module.getReferenceExp().clone()))), null));
                            }
                        }
                    }
                }
            });
        }


        errorHandlingBlock.getBody().add(newBreak());
        postClean(block);
        return block;
    }

    @Override
    public RuntimeModuleVariable loadRuntimeModule(String name, Object... args) {
        return loadRuntimeModule(dynamicScope.getActiveScope(), name, args);
    }

    @Override
    public RuntimeModuleVariable loadRuntimeModule(Scope<PStm> scope, String name, Object... args) {
        return loadRuntimeModule(scope, (s, var) -> s.add(var), name, args);
    }

    public RuntimeModuleVariable loadRuntimeModule(Scope<PStm> scope, BiConsumer<Scope<PStm>, PStm> variableStoreFunc, String name, Object... args) {
        String varName = getNameGenerator().getName(name);
        List<PExp> argList = BuilderUtil.toExp(args);
        argList.add(0, newAStringLiteralExp(name));
        PStm var = newVariable(varName, newANameType(name), newALoadExp(argList));
        variableStoreFunc.accept(scope, var);
        RuntimeModuleVariable module =
                new RuntimeModuleVariable(var, newANameType(name), (IMablScope) scope, dynamicScope, this, newAIdentifierStateDesignator(varName),
                        newAIdentifierExp(varName));
        importedModules.add(name);
        loadedModules.add(module);
        return module;
    }

    private SBlockStm getErrorHandlingBlock(SBlockStm block) throws AnalysisException {
        AtomicReference<SBlockStm> errorHandingBlock = new AtomicReference<>();
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseAWhileStm(AWhileStm node) throws AnalysisException {
                if (node.getBody().equals(mainErrorHandlingScope.getBlock())) {
                    errorHandingBlock.set(((SBlockStm) node.getBody()));

                }
                super.caseAWhileStm(node);
            }
        });
        return errorHandingBlock.get();
    }

    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        SBlockStm block = rootScope.getBlock().clone();

        SBlockStm errorHandingBlock = this.getErrorHandlingBlock(block);

        /**
         * Automatically created unloads for all modules loaded by the builder.
         */
        for (RuntimeModuleVariable module : this.loadedModules) {
            VariableFmi2Api var = module;
            block.apply(new DepthFirstAnalysisAdaptor() {
                @Override
                public void defaultInPStm(PStm node) throws AnalysisException {
                    if (node.equals(module.getDeclaringStm())) {
                        if (node.parent() instanceof SBlockStm) {
                            //this is the scope where the logger is loaded. Check for unload
                            LinkedList<PStm> body = ((SBlockStm) node.parent()).getBody();
                            boolean unloadFound = false;
                            for (int i = body.indexOf(node); i < body.size(); i++) {
                                PStm stm = body.get(i);
                                if (stm instanceof AExpressionStm && ((AExpressionStm) stm).getExp() instanceof AUnloadExp) {
                                    AUnloadExp unload = (AUnloadExp) ((AExpressionStm) stm).getExp();
                                    if (!unload.getArgs().isEmpty() && unload.getArgs().get(0).equals(module.getReferenceExp())) {
                                        unloadFound = true;
                                    }
                                }
                            }
                            if (!unloadFound) {
                                body.add(newIf(newNotEqual(module.getReferenceExp().clone(), newNullExp()),
                                        newExpressionStm(newUnloadExp(Collections.singletonList(module.getReferenceExp().clone()))), null));
                            }
                        }
                    }
                }
            });
        }

        //        if (runtimeLogger != null && this.getSettings().externalRuntimeLogger == false) {
        //            //attempt a syntactic comparison to find the load in the clone
        //            VariableFmi2Api loggerVar = (VariableFmi2Api) runtimeLogger.module;
        //            block.apply(new DepthFirstAnalysisAdaptor() {
        //
        //
        //                @Override
        //                public void defaultInPStm(PStm node) throws AnalysisException {
        //                    if (node.equals(loggerVar.getDeclaringStm())) {
        //                        if (node.parent() instanceof ABlockStm) {
        //                            //this is the scope where the logger is loaded. Check for unload
        //                            LinkedList<PStm> body = ((ABlockStm) node.parent()).getBody();
        //                            boolean unloadFound = false;
        //                            for (int i = body.indexOf(node); i < body.size(); i++) {
        //                                PStm stm = body.get(i);
        //                                //newExpressionStm(newUnloadExp(Arrays.asList(getReferenceExp().clone())
        //                                if (stm instanceof AExpressionStm && ((AExpressionStm) stm).getExp() instanceof AUnloadExp) {
        //                                    AUnloadExp unload = (AUnloadExp) ((AExpressionStm) stm).getExp();
        //                                    if (!unload.getArgs().isEmpty() && unload.getArgs().get(0).equals(loggerVar.getReferenceExp())) {
        //                                        unloadFound = true;
        //                                    }
        //                                }
        //                            }
        //                            if (!unloadFound) {
        //                                body.add(newIf(newNotEqual(loggerVar.getReferenceExp().clone(), newNullExp()),
        //                                        newExpressionStm(newUnloadExp(Collections.singletonList(loggerVar.getReferenceExp().clone()))), null));
        //                            }
        //                        }
        //                    }
        //                }
        //
        //            });
        //        }

        errorHandingBlock.getBody().add(newBreak());


        postClean(block);

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

    private void postClean(SBlockStm block) throws AnalysisException {
        //Post cleaning: Remove empty block statements
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseABasicBlockStm(ABasicBlockStm node) throws AnalysisException {
                if (node.getBody().isEmpty()) {
                    if (node.parent() instanceof SBlockStm) {
                        SBlockStm pb = (SBlockStm) node.parent();
                        pb.getBody().remove(node);
                    } else if (node.parent() instanceof AIfStm) {
                        AIfStm ifStm = (AIfStm) node.parent();

                        if (ifStm.getElse() == node) {
                            ifStm.setElse(null);
                        }
                    }
                } else {
                    super.caseABasicBlockStm(node);
                }
            }
        });

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
            RuntimeModule<PStm> runtimeModule =
                    this.loadRuntimeModule(this.mainErrorHandlingScope, (s, var) -> ((ScopeFmi2Api) s).getBlock().getBody().add(0, var), "Logger");
            this.runtimeLogger = new LoggerFmi2Api(this, runtimeModule);
        }

        return this.runtimeLogger;
    }


    public ExecutionEnvironmentFmi2Api getExecutionEnvironment() {
        if (this.executionEnvironment == null) {
            RuntimeModule<PStm> runtimeModule =
                    this.loadRuntimeModule(this.mainErrorHandlingScope, (s, var) -> ((ScopeFmi2Api) s).getBlock().getBody().add(0, var), "MEnv");
            this.executionEnvironment = new ExecutionEnvironmentFmi2Api(this, runtimeModule);
        }

        return this.executionEnvironment;
    }

    public void addExternalLoadedModuleIdentifier(String name) {
        this.externalLoadedModuleIdentifier.add(name);

    }

    public Set<String> getExternalLoadedModuleIdentifiers() {
        return this.externalLoadedModuleIdentifier;
    }


    public enum FmiStatus {
        FMI_OK(0),
        FMI_WARNING(1),
        FMI_DISCARD(2),
        FMI_ERROR(3),
        FMI_FATAL(4),
        FMI_PENDING(5);

        private final int value;

        private FmiStatus(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

    }

    public static class MablSettings {
        /**
         * Automatically perform FMI2ErrorHandling
         */
        public boolean fmiErrorHandlingEnabled = true;
        /**
         * Automatically retrieves and sets derivatives if possible
         */
        public boolean setGetDerivatives = true;
    }
}
