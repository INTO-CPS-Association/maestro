package org.intocps.maestro.plugin.Initializer.instructions

import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api

abstract class FMUCoSimInstruction(protected var FMU: ComponentVariableFmi2Api) : CoSimInstruction {
    override val isSimple: Boolean
        get() = true
}