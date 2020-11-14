package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.AImportedModuleCompilationUnit;
import org.intocps.maestro.typechecker.DeclarationList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModulesContext extends Context {
    private final Map<LexIdentifier, Map.Entry<AModuleDeclaration, DeclarationList>> modules;


    public ModulesContext(List<? extends AImportedModuleCompilationUnit> modules, Context outerContext) {
        super(outerContext);
        this.modules = modules.stream().collect(
                Collectors.toMap(m -> m.getModule().getName(), m -> Map.entry(m.getModule(), new DeclarationList(m.getModule().getFunctions()))));
    }


    @Override
    public PDeclaration findDeclaration(LexIdentifier module, LexIdentifier name) {
        if (modules.containsKey(module)) {
            PDeclaration decl = modules.get(module).getValue().findDeclaration(name);
            if (decl != null) {
                return decl;
            }
        }
        return super.findDeclaration(module, name);
    }

    @Override
    public PDeclaration findDeclaration(LexIdentifier name) {
        if (modules.containsKey(name)) {
            return modules.get(name).getKey();
        }
        return super.findDeclaration(name);
    }

}
