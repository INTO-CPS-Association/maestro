package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.ast.node.PType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeCheckInfo {

    private final Map<LexIdentifier, ModuleEnvironment> modules = new HashMap<>();
    private CTMEnvironment ctmEnvironment;

    public void addEnvironment(CTMEnvironment ctmEnvironment) {
        this.ctmEnvironment = ctmEnvironment;
    }

    public CTMEnvironment getCtmEnvironment() {
        return ctmEnvironment;
    }

    public PDeclaration findModuleFunction(LexIdentifier module, LexIdentifier name) {
        return this.modules.get(module).findName(name);
    }


    public PType findModule(LexIdentifier name) {
        Optional<LexIdentifier> first = modules.keySet().stream().filter(x -> x.equals(name)).findFirst();
        if (first.isPresent()) {
            return MableAstFactory.newAModuleType(name);
        } else {
            return null;
        }

    }

    public PType findName(LexIdentifier name) {
        return this.ctmEnvironment.findVariableByName(name);
    }

    public void setEnvironment(CTMEnvironment newEnv) {
        this.ctmEnvironment = newEnv;
    }

    public void addModule(LexIdentifier moduleType, ModuleEnvironment env) {
        this.modules.put(moduleType, env);
    }

    public Map<LexIdentifier, ModuleEnvironment> getModules() {
        return modules;
    }
}
