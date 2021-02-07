package org.intocps.maestro.plugin.Initializer.instructions

interface CoSimInstruction {
    fun perform()
    val isSimple: Boolean
}


