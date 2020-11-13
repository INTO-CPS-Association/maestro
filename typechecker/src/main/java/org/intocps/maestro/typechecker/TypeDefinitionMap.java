package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.PType;

import java.util.HashMap;
import java.util.List;

public class TypeDefinitionMap {
    HashMap<LexIdentifier, PDeclaration> nameToDeclaration = new HashMap<>();
    HashMap<LexIdentifier, PType> nameToType = new HashMap<>();

    public TypeDefinitionMap(List<PDeclaration> declarationList, HashMap<LexIdentifier, PType> resolvedDeclarationTypes) {
        for (PDeclaration decl : declarationList) {
            this.add(decl, resolvedDeclarationTypes.get(decl.getName()));
        }
    }

    public TypeDefinitionMap() {
    }

    public void add(PDeclaration declaration, PType type) {
        nameToDeclaration.put(declaration.getName(), declaration);
        nameToType.put(declaration.getName(), type);
    }

    public PDeclaration getDeclaration(LexIdentifier name) {
        return nameToDeclaration.get(name);
    }

    public PType getType(LexIdentifier name) {
        return this.nameToType.get(name);
    }
}
