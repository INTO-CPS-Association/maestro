package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.framework.fmi2.api.Fmi2Builder

abstract class ComplexCoSimInstruction(protected var simulationActions: List<CoSimInstruction>,
                                       protected var scope: Fmi2Builder.Scope<*>) : CoSimInstruction {
    override val isSimple: Boolean
        get() = false
}