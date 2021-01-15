package org.intocps.maestro.framework.fmi2.api.mabl.scoping;

import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.AMaBLVariableCreator;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.values.AMablValue;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablDoubleVariable;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.AMablVariable;

import java.util.Arrays;
import java.util.Collection;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class AMaBLScope implements IMablScope {
    final IMablScope parent;
    private final MablApiBuilder builder;
    private final ABlockStm block;
    AMaBLVariableCreator variableCreator;

    public AMaBLScope(MablApiBuilder builder) {
        this.builder = builder;
        this.parent = null;
        this.block = new ABlockStm();
        this.variableCreator = new AMaBLVariableCreator(this, builder);

    }

    public AMaBLScope(MablApiBuilder builder, IMablScope parent, ABlockStm block) {
        this.builder = builder;
        this.parent = parent;
        this.block = block;
        this.variableCreator = new AMaBLVariableCreator(this, builder);
    }

    public ABlockStm getBlock() {
        return block;
    }

    @Override
    public AMaBLVariableCreator getVariableCreator() {
        return variableCreator;
    }

    @Override
    public Fmi2Builder.WhileScope<PStm> enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
        return null;
    }

    @Override
    public Fmi2Builder.IfScope<PStm> enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
        //todo
        ABlockStm thenStm = newABlockStm();
        ABlockStm elseStm = newABlockStm();

        AIfStm ifStm = newIf(newABoolLiteralExp(true), thenStm, elseStm);
        add(ifStm);
        AMaBLScope thenScope = new AMaBLScope(builder, this, thenStm);
        AMaBLScope elseScope = new AMaBLScope(builder, this, elseStm);
        return new IfMaBlScope(builder, ifStm, this, thenScope, elseScope);
    }

    @Override
    public Fmi2Builder.Scope<PStm> leave() {
        return null;
    }

    @Override
    public Fmi2Builder.LiteralCreator literalCreator() {
        return null;
    }


    @Override
    public void add(PStm... commands) {
        addAll(Arrays.asList(commands));
    }

    @Override
    public void addAll(Collection<PStm> commands) {
        block.getBody().addAll(commands);
    }

    @Override
    public void addBefore(PStm item, PStm... commands) {

        int index = block.getBody().indexOf(item);

        if (index == -1) {
            add(commands);
        } else {
            int insertAt = index - 1;
            if (insertAt < 0) {
                block.getBody().addAll(0, Arrays.asList(commands));
            } else {
                block.getBody().addAll(insertAt, Arrays.asList(commands));
            }
        }

    }

    @Override
    public void addAfter(PStm item, PStm... commands) {
        int index = block.getBody().indexOf(item);
        int insertAt = index + 1;
        if (index == -1 || insertAt > block.getBody().size()) {
            add(commands);
        } else {
            block.getBody().addAll(insertAt, Arrays.asList(commands));
        }
    }


    @Override
    public Fmi2Builder.Scope<PStm> activate() {
        return builder.getDynamicScope().activate(this);
    }

    @Override
    public AMablDoubleVariable store(double value) {
        String name = builder.getNameGenerator().getName();
        ARealLiteralExp initial = newARealLiteralExp(value);
        PStm var = newVariable(name, newARealNumericPrimitiveType(), initial);
        add(var);
        return new AMablDoubleVariable(var, this, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(name)),
                newAIdentifierExp(name));

    }

    @Override
    public <V> Fmi2Builder.Variable<PStm, V> store(Fmi2Builder.Value<V> tag) {

        if (!(tag instanceof AMablValue)) {
            throw new IllegalArgumentException();
        }

        AMablValue<V> v = (AMablValue<V>) tag;

        String name = builder.getNameGenerator().getName();

        PExp initial = null;
        Fmi2Builder.Variable<PStm, V> variable;

        if (v.getType() instanceof ARealNumericPrimitiveType) {
            if (v.get() != null) {
                initial = newARealLiteralExp((Double) v.get());
            }
            //TODO once changed to not use generic values we can return the actual variable type here

        } else if (v.getType() instanceof AIntNumericPrimitiveType) {
            if (v.get() != null) {
                initial = newAIntLiteralExp((Integer) v.get());
            }

        } else if (v.getType() instanceof ABooleanPrimitiveType) {
            if (v.get() != null) {
                initial = newABoolLiteralExp((Boolean) v.get());
            }
        } else if (v.getType() instanceof AStringPrimitiveType) {
            if (v.get() != null) {
                initial = newAStringLiteralExp((String) v.get());
            }
        }

        PStm var = newVariable(name, v.getType(), initial);
        add(var);
        variable = new AMablVariable<>(var, v.getType(), this, builder.getDynamicScope(),
                newAIdentifierStateDesignator(MableAstFactory.newAIdentifier(name)), MableAstFactory.newAIdentifierExp(name));

        return variable;
    }

    @Override
    public Fmi2Builder.DoubleVariable<PStm> doubleFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

    @Override
    public Fmi2Builder.IntVariable<PStm> intFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

    @Override
    public Fmi2Builder.MBoolean booleanFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
        return null;
    }

}
