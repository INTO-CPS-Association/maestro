package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

public interface RelationVariable {
    LexIdentifier getInstance();

    ModelDescription.ScalarVariable getScalarVariable();
}
