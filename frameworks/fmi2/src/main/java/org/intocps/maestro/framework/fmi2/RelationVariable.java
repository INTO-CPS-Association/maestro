package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.fmi.Fmi2ModelDescription;


public class RelationVariable implements org.intocps.maestro.framework.core.RelationVariable {
    public final Fmi2ModelDescription.ScalarVariable scalarVariable;
    // instance is necessary because:
    // If you look up the relations for FMU Component A,
    // and there is a dependency from FMU Component B Input as Source to FMU Component A as Target.
    // Then it is only possible to figure out that Source actually belongs to FMU Component B if instance is part of Source.
    public final LexIdentifier instance;

    public RelationVariable(Fmi2ModelDescription.ScalarVariable scalarVariable, LexIdentifier instance) {
        this.scalarVariable = scalarVariable;
        this.instance = instance;
    }

    @Override
    public LexIdentifier getInstance() {
        return this.instance;
    }

    @Override
    public Fmi2ModelDescription.ScalarVariable getScalarVariable() {
        return scalarVariable;
    }

    @Override
    public String toString() {
        return instance + "." + scalarVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RelationVariable)) {
            return false;
        }

        RelationVariable rv = (RelationVariable) o;
        return rv.toString().equals(this.toString());
    }
}
