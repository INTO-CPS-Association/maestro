package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.ABlockStm;
import org.intocps.maestro.ast.ALocalVariableStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;

import java.util.Optional;

public class ExpansionUtilities {
    /**
     * Looks up the tree for a given variabledeclaration
     *
     * @param name
     * @param containingBlock
     * @param maxAncestorLevel
     * @return
     */
    public static Optional<AVariableDeclaration> getVariableDeclaration(LexIdentifier name, ABlockStm containingBlock, int maxAncestorLevel) {
        return getVariableDeclaration(name, containingBlock, maxAncestorLevel, 0);
    }

    public static Optional<AVariableDeclaration> getVariableDeclaration(LexIdentifier name, ABlockStm containingBlock, int maxAncestorLevel,
            int currentAncestorLevel) {
        Optional<AVariableDeclaration> first =
                containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                        .map(ALocalVariableStm::getDeclaration)
                        .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();
        if (first.isPresent() || maxAncestorLevel == currentAncestorLevel) {
            return first;
        } else {
            return getVariableDeclaration(name, containingBlock.getAncestor(ABlockStm.class), maxAncestorLevel, currentAncestorLevel + 1);
        }
    }
}
