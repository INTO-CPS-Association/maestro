package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.interpreter.values.ModuleValue;
import org.intocps.maestro.interpreter.values.Value;

public class ModuleContext extends Context {

    final ModuleValue module;

    public ModuleContext(ModuleValue module) {
        this.module = module;
    }

    @Override
    public Value lookup(LexIdentifier identifier) {
        return module.lookup(identifier.getText());
    }

    @Override
    public Value lookup(String name) {
        return module.lookup(name);
    }
}
