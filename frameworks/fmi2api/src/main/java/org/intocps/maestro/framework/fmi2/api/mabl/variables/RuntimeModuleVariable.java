package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.BuilderUtil;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;

import java.util.List;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class RuntimeModuleVariable extends VariableFmi2Api<Fmi2Builder.NamedVariable<PStm>> implements Fmi2Builder.RuntimeModule<PStm> {
    private final MablApiBuilder builder;

    public RuntimeModuleVariable(PStm declaration, PType type, IMablScope declaredScope, Fmi2Builder.DynamicActiveScope<PStm> dynamicScope,
            MablApiBuilder builder, PStateDesignator designator, PExp referenceExp) {
        super(declaration, type, declaredScope, dynamicScope, designator, referenceExp);
        this.builder = builder;
    }

    @Override
    public void initialize(List<Fmi2Builder.RuntimeFunction> declaredFuncs) {

    }

    @Override
    public void initialize(Fmi2Builder.RuntimeFunction... declaredFuncs) {

    }

    @Override
    public void callVoid(Fmi2Builder.RuntimeFunction functionId, Object... args) {
        callVoid(dynamicScope, functionId, args);
    }

    @Override
    public void callVoid(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.RuntimeFunction functionId, Object... args) {
        PStm stm = newExpressionStm(MableBuilder.call(this.getReferenceExp().clone(), functionId.getName(), BuilderUtil.toExp(args)));
        scope.add(stm);
    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> call(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.RuntimeFunction functionId, Object... args) {

        if (functionId.getReturnType().isNative() &&
                functionId.getReturnType().getNativeType() == Fmi2Builder.RuntimeFunction.FunctionType.Type.Void) {
            callVoid(scope, functionId, args);
            return null;
        }


        PType varType = getMablType(functionId.getReturnType());

        String name = builder.getNameGenerator().getName();
        PStm stm = MableBuilder
                .newVariable(name, varType, MableBuilder.call(this.getReferenceExp().clone(), functionId.getName(), BuilderUtil.toExp(args)));
        scope.add(stm);

        if (functionId.getReturnType().isNative()) {
            switch (functionId.getReturnType().getNativeType()) {

                case Void:
                    return null;
                case Int:
                case UInt:
                    return (Fmi2Builder.Variable<PStm, V>) new IntVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case Double:
                    return (Fmi2Builder.Variable<PStm, V>) new DoubleVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case String:
                    return (Fmi2Builder.Variable<PStm, V>) new StringVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
                case Boolean:
                    return (Fmi2Builder.Variable<PStm, V>) new BooleanVariableFmi2Api(stm, (IMablScope) scope, dynamicScope,
                            newAIdentifierStateDesignator(name), newAIdentifierExp(name));
            }
        }
        return new VariableFmi2Api(stm, varType.clone(), (IMablScope) scope, dynamicScope, newAIdentifierStateDesignator(name),
                newAIdentifierExp(name));
    }

    private PType getMablType(Fmi2Builder.RuntimeFunction.FunctionType type) {
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
    public <V> Fmi2Builder.Variable<PStm, V> call(Fmi2Builder.RuntimeFunction functionId, Object... args) {
        return call(dynamicScope, functionId, args);
    }


    @Override
    public void destroy() {
        destroy(dynamicScope);
    }

    @Override
    public void destroy(Fmi2Builder.Scope<PStm> scope) {
        scope.add(newExpressionStm(newUnloadExp(getReferenceExp())));
    }
}
