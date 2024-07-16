package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.BuilderUtil;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.List;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class RuntimeModuleVariable extends VariableFmi2Api<FmiBuilder.NamedVariable<PStm>> implements FmiBuilder.RuntimeModule<PStm> {
    private final MablApiBuilder builder;
    private boolean external = false;

    public RuntimeModuleVariable(PStm declaration, PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            MablApiBuilder builder, PStateDesignator designator, PExp referenceExp) {
        this(declaration, type, declaredScope, dynamicScope, builder, designator, referenceExp, false);
    }

    public RuntimeModuleVariable(PStm declaration, PType type, IMablScope declaredScope, FmiBuilder.DynamicActiveScope<PStm> dynamicScope,
            MablApiBuilder builder, PStateDesignator designator, PExp referenceExp, boolean external) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.builder = builder;
        this.external = external;
    }

    @Override
    public void initialize(List<FmiBuilder.RuntimeFunction> declaredFuncs) {

    }

    @Override
    public void initialize(FmiBuilder.RuntimeFunction... declaredFuncs) {
    }

    @Override
    public void callVoid(FmiBuilder.RuntimeFunction functionId, Object... args) {
        callVoid(dynamicScope, functionId, args);
    }

    @Override
    public void callVoid(FmiBuilder.Scope<PStm> scope, FmiBuilder.RuntimeFunction functionId, Object... args) {
        PStm stm = newExpressionStm(MableBuilder.call(this.getReferenceExp().clone(), functionId.getName(),
                BuilderUtil.toExp(args).stream().map(PExp::clone).collect(Collectors.toList())));
        scope.add(stm);
    }

    @Override
    public <V> FmiBuilder.Variable<PStm, V> call(FmiBuilder.Scope<PStm> scope, FmiBuilder.RuntimeFunction functionId, Object... args) {

        if (functionId.getReturnType().isNative() &&
                functionId.getReturnType().getNativeType() == FmiBuilder.RuntimeFunction.FunctionType.Type.Void) {
            callVoid(scope, functionId, args);
            return null;
        }


        PType varType = getMablType(functionId.getReturnType());

        String name = builder.getNameGenerator().getName();
        PStm stm = MableBuilder.newVariable(name, varType, MableBuilder.call(this.getReferenceExp().clone(), functionId.getName(),
                BuilderUtil.toExp(args).stream().map(PExp::clone).collect(Collectors.toList())));
        scope.add(stm);

        if (functionId.getReturnType().isNative()) {
            switch (functionId.getReturnType().getNativeType()) {

                case Void:
                    return null;
                case Int:
                case UInt:
                    return (FmiBuilder.Variable<PStm, V>) new IntVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case Double:
                    return (FmiBuilder.Variable<PStm, V>) new DoubleVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case String:
                    return (FmiBuilder.Variable<PStm, V>) new StringVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case Boolean:
                    return (FmiBuilder.Variable<PStm, V>) new BooleanVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
            }
        }
        return new VariableFmi2Api(stm, varType.clone(), (IMablScope) scope, dynamicScope, newAIdentifierStateDesignator(name),
                newAIdentifierExp(name));
    }

    private PType getMablType(FmiBuilder.RuntimeFunction.FunctionType type) {
        if (!type.isNative()) {
            return newANameType(type.getNamedType());
        }
        switch (type.getNativeType()) {

            case Void:
                return newAVoidType();
            case Int:
                return newIntType();
            case UInt:
                return newUIntType();
            case Double:
                return newRealType();
            case String:
                return newStringType();
            case Boolean:
                return newBoleanType();
        }
        return null;
    }

    @Override
    public <V> FmiBuilder.Variable<PStm, V> call(FmiBuilder.RuntimeFunction functionId, Object... args) {
        return call(dynamicScope, functionId, args);
    }


    //    @Override
    //    public void destroy() {
    //        destroy(dynamicScope);
    //    }
    //
    //    @Override
    //    public void destroy(Fmi2Builder.Scope<PStm> scope) {
    //        if (!this.external) {
    //            scope.add(newExpressionStm(newUnloadExp(getReferenceExp())));
    //        }
    //    }
}
