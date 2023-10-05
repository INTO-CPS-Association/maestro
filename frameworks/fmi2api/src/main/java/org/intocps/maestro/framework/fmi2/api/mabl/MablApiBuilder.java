package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.DerivativeEstimator;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.TryMaBlScope;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, MablApiBuilder.MablSettings> {

    static ScopeFmi2Api rootScope;
    final ScopeFmi2Api externalScope = new ScopeFmi2Api(this);
    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    final TryMaBlScope mainErrorHandlingScope;
    private final IntVariableFmi2Api globalFmiStatus;
    private final MablToMablAPI mablToMablAPI;
    private final MablSettings settings;
    private final Map<FmiStatusInterface, IntVariableFmi2Api> fmiStatusVariables;
    private final Set<String> externalLoadedModuleIdentifier = new HashSet<>();
    int dynamicScopeInitialSize;
    List<String> importedModules = new Vector<>();
    List<RuntimeModuleVariable> loadedModules = new Vector<>();
    Map<String, Object> instanceCache = new HashMap<>();
    private MathBuilderFmi2Api mathBuilderApi;

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
                        fmiStatusVariables.put(s, rootScope.store(() -> this.getNameGenerator().getNameIgnoreCase(s.name()), s.getValue()));
                    } else {
                        //if exists then link to previous declaration
                        fmiStatusVariables.put(s, f.apply(decl.getName().getText()));
                    }
                }
            }
        }

        String status_varname = "status";

        if (createdFromExistingSpec) {

            AVariableDeclaration decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, "global_execution_continue");

            decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, status_varname);
            if (decl == null) {
                globalFmiStatus = rootScope.store(status_varname, FmiStatus.FMI_OK.getValue());
            } else {
                globalFmiStatus = (IntVariableFmi2Api) createVariableExact(rootScope, newIntType(), null, decl.getName().getText(), true);
            }

        } else {

            globalFmiStatus = rootScope.store(status_varname, FmiStatus.FMI_OK.getValue());
        }

        mainErrorHandlingScope = rootScope.enterTry();
        this.dynamicScope = new DynamicActiveBuilderScope(mainErrorHandlingScope.getBody());
        this.mablToMablAPI = new MablToMablAPI(this);

        if (createdFromExistingSpec) {
            AVariableDeclaration decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, "logger");
            if (decl != null) {
                this.getMablToMablAPI().createExternalRuntimeLogger();
            }

            //reserve all previously names to avoid clashing with these
            MablToMablAPI.getPreviouslyUsedNamed(lastNodePriorToBuilderTakeOver).forEach(this.nameGenerator::addUsedIdentifier);
        }


        resetDirty();

    }

    @Override
    public boolean isDirty() {
        return dynamicScopeInitialSize != ((ScopeFmi2Api) this.dynamicScope.activate()).getBlock().getBody().size();
    }

    @Override
    public void resetDirty() {
        dynamicScopeInitialSize = ((ScopeFmi2Api) this.dynamicScope.activate()).getBlock().getBody().size();
    }

    public void setRuntimeLogger(LoggerFmi2Api runtimeLogger) {
        this.instanceCache.put("Logger", runtimeLogger);
    }

    @Override
    public MablSettings getSettings() {
        return this.settings;
    }

    interface FmiStatusInterface {
        int getValue();

        String getName();
    }

    private IntVariableFmi2Api getFmiStatusConstant_aux(FmiStatusInterface status) {
        if(!this.fmiStatusVariables.containsKey(status)) {
            IntVariableFmi2Api var = rootScope.store(status.getName(), status.getValue());
            rootScope.addAfterOrTop(null, var.getDeclaringStm());
            fmiStatusVariables.put(status, var);
        }
        return this.fmiStatusVariables.get(status);
    }

    public IntVariableFmi2Api getFmiStatusConstant(FmiStatus status) {return getFmiStatusConstant_aux(status);}
    public IntVariableFmi2Api getFmiStatusConstant(Fmi3Status status) {return getFmiStatusConstant_aux(status);}

    public MablToMablAPI getMablToMablAPI() {
        return this.mablToMablAPI;
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
        this.externalScope.add(var);
        if (type instanceof ARealNumericPrimitiveType) {
            return new DoubleVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof ABooleanPrimitiveType) {
            return new BooleanVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AIntNumericPrimitiveType) {
            return new IntVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AStringPrimitiveType) {
            return new StringVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        }

        return new VariableFmi2Api(var, type, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
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
        //        SBlockStm errorHandlingBlock = this.getErrorHandlingBlock(block);
        //        if (errorHandlingBlock == null) {
        //            return null;
        //        }


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


        //        errorHandlingBlock.getBody().add(newBreak());
        postClean(block);
        return block;
    }

    @Override
    public RuntimeModuleVariable loadRuntimeModule(String name, Object... args) {

        ScopeElement<PStm> scope = dynamicScope.getActiveScope();

        if (!(scope instanceof TryScope)) {
            while ((scope = scope.parent()) != null) {
                if (scope instanceof TryScope) {
                    break;
                }
            }
        }

        return loadRuntimeModule((TryScope<PStm>) scope, name, args);
    }

    @Override
    public RuntimeModuleVariable loadRuntimeModule(TryScope<PStm> scope, String name, Object... args) {
        return loadRuntimeModule(scope, (s, var) -> s.addAll(var), name, args);
    }

    //    private SBlockStm getErrorHandlingBlock(SBlockStm block) throws AnalysisException {
    //        AtomicReference<SBlockStm> errorHandingBlock = new AtomicReference<>();
    //        block.apply(new DepthFirstAnalysisAdaptor() {
    //            @Override
    //            public void caseAWhileStm(AWhileStm node) throws AnalysisException {
    //                if (node.getBody().equals(mainErrorHandlingScope.getBlock())) {
    //                    errorHandingBlock.set(((SBlockStm) node.getBody()));
    //
    //                }
    //                super.caseAWhileStm(node);
    //            }
    //        });
    //        return errorHandingBlock.get();
    //    }

    public RuntimeModuleVariable loadRuntimeModule(TryScope<PStm> scope, BiConsumer<Scope<PStm>, List<PStm>> variableStoreFunc, String name,
            Object... args) {
        String varName = getNameGenerator().getName(name);
        List<PExp> argList = BuilderUtil.toExp(args);
        argList.add(0, newAStringLiteralExp(name));
        PStm var = newVariable(varName, newANameType(name));

        var thisScope = scope.findParent(ScopeFmi2Api.class);
        thisScope.addBefore(scope.getDeclaration(), var);

        RuntimeModuleVariable module =
                new RuntimeModuleVariable(var, newANameType(name), thisScope, dynamicScope, this, newAIdentifierStateDesignator(varName),
                        newAIdentifierExp(varName));

        variableStoreFunc.accept(scope.getBody(), Arrays.asList(newAAssignmentStm(newAIdentifierStateDesignator(varName), newALoadExp(argList)),
                newIf(newEqual(module.getReferenceExp().clone(), newNullExp()), new AErrorStm(newAStringLiteralExp("Failed load of: " + varName)),
                        null)));


        ((IMablScope) scope.getFinallyBody()).addAfterOrTop(null, newIf(newNotEqual(module.getReferenceExp().clone(), newNullExp()),
                newABlockStm(newExpressionStm(newUnloadExp(List.of(module.getReferenceExp().clone()))),
                        newAAssignmentStm(module.getDesignator().clone(), newNullExp())), null));
        importedModules.add(name);
        //        loadedModules.add(module);
        return module;
    }

    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        SBlockStm block = rootScope.getBlock().clone();

        //        SBlockStm errorHandingBlock = this.getErrorHandlingBlock(block);

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

        //        errorHandingBlock.getBody().add(newBreak());


        postClean(block);

        ASimulationSpecificationCompilationUnit unit = new ASimulationSpecificationCompilationUnit();
        unit.setBody(block);
        unit.setFramework(Collections.singletonList(newAIdentifier("FMI2")));

        AConfigFramework config = new AConfigFramework();
        config.setName(newAIdentifier("FMI2"));
        //config.setConfig(StringEscapeUtils.escapeJava(simulationEnvironment.));
        // unit.setFrameworkConfigs(Arrays.asList(config));

        // TODO: added "import FMI3" after "import FMI2". Should probably figure out a smarter way to do this
        unit.setImports(Stream.concat(Stream.of(newAIdentifier("FMI2")),
                        Stream.concat(Stream.of(newAIdentifier("FMI3")), importedModules.stream().map(MableAstFactory::newAIdentifier)))
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

    public void addExternalLoadedModuleIdentifier(String name) {
        this.externalLoadedModuleIdentifier.add(name);

    }

    public Set<String> getExternalLoadedModuleIdentifiers() {
        return this.externalLoadedModuleIdentifier;
    }

    public BooleanBuilderFmi2Api getBooleanBuilder() {
        return load("BooleanLogic", runtime -> new BooleanBuilderFmi2Api(this, runtime));
    }

    public ExecutionEnvironmentFmi2Api getExecutionEnvironment() {
        return load("MEnv", runtime -> new ExecutionEnvironmentFmi2Api(this, runtime));
    }

    public LoggerFmi2Api getLogger() {
        return load("Logger", runtime -> new LoggerFmi2Api(this, runtime));
    }

    public FaultInject getFaultInject(Object... args) {
        return load("FaultInject", runtime -> new FaultInject(this, runtime), args);
    }

    public VariableStep getVariableStep(StringVariableFmi2Api config) {
        return load("VariableStep", runtime -> new VariableStep(this, runtime), config);
    }

    public DerivativeEstimator getDerivativeEstimator() {
        return load("DerivativeEstimator", runtime -> new DerivativeEstimator(this.getDynamicScope(), this, runtime));
    }

    public DataWriter getDataWriter() {
        return load("DataWriter", runtime -> new DataWriter(this, runtime));
    }

    public SimulationControl getSimulationControl() {
        return load("SimulationControl", runtime -> new SimulationControl(this, runtime));
    }

    public ConsolePrinter getConsolePrinter() {
        return load("ConsolePrinter", runtime -> new ConsolePrinter(this, runtime));
    }

    public RealTime getRealTimeModule() {
        return load("RealTime", runtime -> new RealTime(this, runtime));
    }

    <T> T load(String moduleType, Function<FmiBuilder.RuntimeModule<PStm>, T> creator, Object... args) {
        if (instanceCache.containsKey(moduleType)) {
            return (T) instanceCache.get(moduleType);
        }

        FmiBuilder.RuntimeModule<PStm> runtimeModule = this.loadRuntimeModule(this.mainErrorHandlingScope, (s, var) -> {
            if (args == null || args.length == 0) {
                ((ScopeFmi2Api) s).getBlock().getBody().addAll(0, var);
            } else {
                //we need to mare sure we don't move the creation before the arguments passed
                int index = 0;
                var b = ((ScopeFmi2Api) s).getBlock();

                for (Object arg : args) {
                    if (arg instanceof VariableFmi2Api) {
                        INode argDecl = ((VariableFmi2Api<?>) arg).getDeclaringStm();
                        while (!b.equals(argDecl.parent()) && (argDecl = argDecl.parent()) != null) {
                        }

                        if (argDecl != null && b.equals(argDecl.parent())) {
                            var argIndex = b.getBody().indexOf(argDecl) + 1;
                            if (argIndex > index) {
                                index = argIndex;
                            }
                        }

                    }
                }
                b.getBody().addAll(index, var);
            }
        }, moduleType, args);
        var m = creator.apply(runtimeModule);
        instanceCache.put(moduleType, m);
        return m;
    }

    public enum FmiStatus implements FmiStatusInterface {
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
        public int getValue() { return this.value; }

        // Interface method, not sure how to best preserve enum name() method.
        public String getName() {
            for (FmiStatus status : FmiStatus.values()) {
                if (status.value == this.value) {
                    return status.name();
                }
            }
            return null;
        }
    }

    public enum Fmi3Status implements FmiStatusInterface {
        FMI_OK(0),
        FMI_WARNING(1),
        FMI_DISCARD(2),
        FMI_ERROR(3),
        FMI_FATAL(4);
        private final int value;
        private Fmi3Status(final int value) {
            this.value = value;
        }
        public int getValue() {
            return this.value;
        }

        public String getName() {
            for (FmiStatus status : FmiStatus.values()) {
                if (status.value == this.value) {
                    return status.name();
                }
            }
            return null;
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
