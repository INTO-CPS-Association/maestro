package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.framework.fmi2.api.FmiBuilder

abstract class ComplexCoSimInstruction(
    protected var simulationActions: List<CoSimInstruction>,
    protected var scope: FmiBuilder.Scope<*>
) : CoSimInstruction {
    override val isSimple: Boolean
        get() = false
}