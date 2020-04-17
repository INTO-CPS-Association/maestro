package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.AIdentifierExp;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.interpreter.values.Value;

import java.util.HashMap;
import java.util.Map;

class Context {

    Map<LexIdentifier, Value> values = new HashMap<>();

    public void put(AIdentifierExp identifier, Value apply) {
        values.put(identifier.getName(), apply);
    }

    public void put(LexIdentifier identifier, Value apply) {
        values.put(identifier, apply);
    }

    public Value lookup(LexIdentifier identifier) {
        return this.values.get(identifier);
    }

    public Value lookup(String name) {
        return this.lookup(new LexIdentifier(name, null));
    }
}
