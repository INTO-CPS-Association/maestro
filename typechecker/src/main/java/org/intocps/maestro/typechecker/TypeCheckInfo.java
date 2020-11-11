package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.AModuleType;
import org.intocps.maestro.ast.node.PType;

import java.util.Map;
import java.util.Optional;

public class TypeCheckInfo {

    private Map<AModuleType, ModuleEnvironment> modules = null;
    private CTMEnvironment ctmEnvironment;

    public void addModules(Map<AModuleType, ModuleEnvironment> modules) {
        this.modules = modules;
    }

    public void addEnvironment(CTMEnvironment ctmEnvironment) {
        this.ctmEnvironment = ctmEnvironment;
    }

    public CTMEnvironment getCtmEnvironment() {
        return ctmEnvironment;
    }

    public PDeclaration findModuleFunction(AModuleType module, LexIdentifier name) {
        return this.modules.get(module).findName(name);
    }


    public PType findModule(LexIdentifier name) {
        Optional<AModuleType> first = modules.keySet().stream().filter(x -> x.getName().getName().equals(name)).findFirst();
        return first.orElse(null);
    }

    public PType findName(LexIdentifier name) {
        return this.ctmEnvironment.findVariableByName(name);
    }
}
