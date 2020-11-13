package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;

import java.util.Map;

public class ModulesContext extends ATypeCheckerContext {
    private final Map<LexIdentifier, TypeDefinitionMap> modules;


    public ModulesContext(Map<LexIdentifier, TypeDefinitionMap> modules, ATypeCheckerContext outerContext) {
        super(outerContext);
        this.modules = modules;
    }

    @Override
    public TypeDefinitionMap findModuleDeclarations(LexIdentifier module) {

        if (modules != null && modules.containsKey(module)) {
            return modules.get(module);
        } else {
            return super.findModuleDeclarations(module);
        }
    }
}
