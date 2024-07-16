package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.framework.fmi2.api.mabl.variables.InstanceVariableFmi3Api

abstract class FMUCoSimInstruction3(protected var FMU: InstanceVariableFmi3Api) : CoSimInstruction {
    override val isSimple: Boolean
        get() = true
}