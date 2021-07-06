package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.BaseModelDescription

class Fmi3Unit private constructor(
    val name: String,
    val baseUnit: BaseModelDescription.BaseUnit?,
    val displayUnits: Collection<Fmi3DisplayUnit>?
) {
    data class Builder(
        var name: String = "",
        var baseUnit: BaseModelDescription.BaseUnit? = null,
        var displayUnits: Collection<Fmi3DisplayUnit>? = null
    ) {
        fun setName(name: String) = apply { this.name = name }
        fun setBaseUnit(baseUnit: BaseModelDescription.BaseUnit) = apply { this.baseUnit = baseUnit }
        fun setDisplayUnits(displayUnits: Collection<Fmi3DisplayUnit>) = apply { this.displayUnits = displayUnits }
        fun build() = Fmi3Unit(name, baseUnit, displayUnits)
    }

    class Fmi3DisplayUnit internal constructor(
        val inverse: Boolean?,
        name: String,
        factor: Double?,
        offset: Double?
    )
}