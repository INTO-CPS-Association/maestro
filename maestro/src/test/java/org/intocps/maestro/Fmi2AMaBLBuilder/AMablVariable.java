package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.node.AIdentifierStateDesignator;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

public class AMablVariable<T> implements Fmi2Builder.Variable<T> {
    private final AMaBLScope scope;
    private final String lexName;
    PType type;
    AMaBLVariableLocation.IValuePosition position;
    T value;

    public AMablVariable(String name, PType type, AMaBLScope scope, AMaBLVariableLocation.IValuePosition position) {
        this.lexName = name;
        this.position = position;
        this.scope = scope;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.lexName;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;

    }

    @Override
    public AMaBLScope getScope() {
        return this.scope;
    }

    public LexIdentifier getIdentifier() {
        return MableAstFactory.newAIdentifier(this.lexName);
    }

    public AIdentifierStateDesignator getStateDesignator() {
        return new MableAstFactory().newAIdentifierStateDesignator(this.getIdentifier());
    }

    public AIdentifierStateDesignator getNestedStateDesignator() {
        return null;
    }
}