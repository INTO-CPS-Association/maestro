package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api

class GetInstruction(fmu: ComponentVariableFmi2Api, private val port: PortFmi2Api, private val isTentative: Boolean = false) : FMUCoSimInstruction(fmu) {
    override fun perform() {
        if (isTentative)
            FMU.get<Any>(port.name)
        else
            FMU.getAndShare<Any>(port.name)
    }
}


