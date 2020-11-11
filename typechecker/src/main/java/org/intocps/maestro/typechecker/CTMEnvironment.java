package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.AFunctionType;
import org.intocps.maestro.ast.node.PType;

import java.util.HashMap;
import java.util.Map;

public class CTMEnvironment {
    private final CTMEnvironment outer;
    private final Map<LexIdentifier, PType> variables = new HashMap<>();
    private final Map<LexIdentifier, AFunctionType> functions = new HashMap<>();
    // todo: fix this
    int errorCode = -5;

    public CTMEnvironment(CTMEnvironment outer) {
        this.outer = outer;
    }


    public void addVariable(LexIdentifier name, PType nodeType) {
        if (this.variables.containsKey(name)) {
            throw new InternalException(this.errorCode, String.format("Redefinition of %s", name));
        }
        this.variables.put(name, nodeType);
    }

    public void addFunction(LexIdentifier name, AFunctionType functionType) {
        if (this.functions.containsKey(name)) {
            throw new InternalException(this.errorCode, String.format("Redefinition of %s", name));
        }
        this.functions.put(name, functionType);
    }


    public PType findFunctionByName(LexIdentifier name) {
        PType returnType = functions.get(name);
        if (returnType == null) {
            if (outer != null) {
                returnType = outer.findVariableByName(name);
            }
        }
        return returnType;
    }

    public PType findVariableByName(LexIdentifier name) {
        PType returnType = variables.get(name);
        if (returnType == null) {
            if (outer != null) {
                returnType = outer.findVariableByName(name);
            }
        }
        return returnType;
    }
}
