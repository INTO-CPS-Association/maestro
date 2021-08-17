package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

public interface RelationVariable {
    LexIdentifier getInstance();

    Fmi2ModelDescription.ScalarVariable getScalarVariable();
}
