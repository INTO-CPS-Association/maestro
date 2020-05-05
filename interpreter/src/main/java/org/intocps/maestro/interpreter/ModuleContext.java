package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.interpreter.values.ModuleValue;
import org.intocps.maestro.interpreter.values.Value;

public class ModuleContext extends Context {

    final ModuleValue module;

    public ModuleContext(ModuleValue module, Context outer) {
        super(outer);
        this.module = module;
    }

    @Override
    public Value lookup(LexIdentifier identifier) {


        Value val = module.lookup(identifier.getText());

        if (val != null) {
            return val;
        }

        return super.lookup(identifier);
    }

    @Override
    public Value lookup(String name) {

        Value val = module.lookup(name);

        if (val != null) {
            return val;
        }

        return super.lookup(name);
    }
}
