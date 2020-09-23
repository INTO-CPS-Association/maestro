package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.PStm;

import java.util.List;

public interface GeneratorComponent {
    public List<PStm> deallocate();
}
