package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi3Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.InstanceVariableFmi3Api

class GetInstruction3(fmu: InstanceVariableFmi3Api, private val port: PortFmi3Api, private val isTentative: Boolean = false) : FMUCoSimInstruction3(fmu) {
    override fun perform() {
        if (isTentative)
            FMU.get<Any>(port.name)
        else
            FMU.getAndShare<Any>(port.name)
    }
}


