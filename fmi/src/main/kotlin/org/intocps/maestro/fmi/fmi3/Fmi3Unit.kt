package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.ModelDescription

class Fmi3Unit private constructor(
    val name: String,
    val fmiUnit: ModelDescription.FmiUnit?,
    val displayUnits: Collection<Fmi3DisplayUnit>?
) {
    data class Builder(
        var name: String = "",
        var fmiUnit: ModelDescription.FmiUnit? = null,
        var displayUnits: Collection<Fmi3DisplayUnit>? = null
    ) {
        fun setName(name: String) = apply { this.name = name }
        fun setBaseUnit(fmiUnit: ModelDescription.FmiUnit) = apply { this.fmiUnit = fmiUnit }
        fun setDisplayUnits(displayUnits: Collection<Fmi3DisplayUnit>) = apply { this.displayUnits = displayUnits }
        fun build() = Fmi3Unit(name, fmiUnit, displayUnits)
    }

    class Fmi3DisplayUnit internal constructor(
        val inverse: Boolean?,
        name: String,
        factor: Double?,
        offset: Double?
    )
}