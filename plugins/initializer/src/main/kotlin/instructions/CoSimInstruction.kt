package org.intocps.maestro.plugin.initializer.instructions

interface CoSimInstruction {
    fun perform()
    val isSimple: Boolean
}


