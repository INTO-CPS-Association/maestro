package org.intocps.maestro.ast.display;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.node.AArrayType;

public class PrintVariableDeclaration {
    public static String VariableDeclarationToString(AVariableDeclaration variableDeclaration) {
        StringBuilder sb = new StringBuilder();
        if (variableDeclaration.getType() instanceof AArrayType) {
            AArrayType type = (AArrayType) variableDeclaration.getType();
            return String.format("%s %s[%s]", type.getType().toString(), variableDeclaration.getName().toString(), type.getSize());
        } else {
            return variableDeclaration.toString();
        }

    }
}
