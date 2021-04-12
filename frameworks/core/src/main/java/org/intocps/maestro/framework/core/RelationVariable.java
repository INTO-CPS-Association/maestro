package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.fmi.ModelDescription;

public interface RelationVariable {
    LexIdentifier getInstance();

    ModelDescription.ScalarVariable getScalarVariable();
}
