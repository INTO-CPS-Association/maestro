package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.AIdentifierExp;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.interpreter.values.Value;

import java.util.HashMap;
import java.util.Map;

class Context {

    final Context outer;


    Map<LexIdentifier, Value> values = new HashMap<>();

    Context(Context outer) {
        this.outer = outer;
    }

    public void put(AIdentifierExp identifier, Value apply) {
        values.put(identifier.getName(), apply);
    }

    public void put(LexIdentifier identifier, Value apply) {
        values.put(identifier, apply);
    }

    public Value lookup(LexIdentifier identifier) {

        Value val = this.values.get(identifier);

        if (val != null) {
            return val;
        }

        if (outer != null) {
            return outer.lookup(identifier);
        }
        return null;
    }

    public Value lookup(String name) {

        Value val = this.lookup(new LexIdentifier(name, null));

        if (val != null) {
            return val;
        }

        if (outer != null) {
            return outer.lookup(name);
        }
        return null;

    }
}
