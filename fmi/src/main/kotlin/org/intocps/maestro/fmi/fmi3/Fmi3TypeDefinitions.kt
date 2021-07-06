package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

data class Fmi3TypeDefinition(val typeIdentifier: Fmi3TypeIdentifiers, val type: Fmi3Type) {

    data class FloatType(
        override val name: String,
        override val description: String?,
        val quantity: String?,
        val unit: String?,
        val displayUnit: String?,
        val relativeQuantity: Boolean?,
        val unbounded: Boolean?,
        val min: Double?,
        val max: Double?,
        val nominal: Double?
    ) : Fmi3Type

    data class IntType(
        override val name: String,
        override val description: String?,
        val quantity: String?,
        val min: Double?,
        val max: Double?
    ) : Fmi3Type

    data class BooleanType(
        override val name: String,
        override val description: String?,
    ) : Fmi3Type

    data class StringType(
        override val name: String,
        override val description: String?,
    ) : Fmi3Type

    data class BinaryType(
        override val name: String,
        override val description: String?,
        val mimeType: String?,
        val maxSize: UInt?
    ) : Fmi3Type

    data class EnumerationType(
        override val name: String,
        override val description: String?,
        val quantity: String?
    ) : Fmi3Type

    data class ClockType(
        override val name: String,
        override val description: String?,
        val canBeDeactivated: Boolean? = false,
        val priority: UInt?,
        val interval: Any?, //TODO: Implement interval -> It declares the clock type
        val intervalDecimal: Float?,
        val shiftDecimal: Float? = (0).toFloat(),
        val supportsFraction: Boolean? = false,
        val resolution: ULong?,
        val intervalCounter: ULong?,
        val shiftCounter: ULong? = (0).toULong()
    ) : Fmi3Type

    interface Fmi3Type {
        val name: String
        val description: String?
    }

    enum class Fmi3TypeIdentifiers {
        Float32Type, Float64Type, Int8Type, UInt8Type, Int16Type, UInt16Type, Int32Type, UInt32Type, Int64Type,
        UInt64Type, BooleanType, StringType, BinaryType, EnumerationType, ClockType;

        companion object {
            fun fromVariableTypeAsString(variableTypeIdentifier: String): Fmi3TypeIdentifiers = valueOf(variableTypeIdentifier.plus("Type"))
        }
    }
}