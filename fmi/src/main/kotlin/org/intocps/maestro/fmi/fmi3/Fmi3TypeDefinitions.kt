package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

data class FloatTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum,
    val quantity: String?,
    val unit: String?,
    val displayUnit: String?,
    val relativeQuantity: Boolean?,
    val unbounded: Boolean?,
    val min: Double?,
    val max: Double?,
    val nominal: Double?
) : IFmi3TypeDefinition

data class IntTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum,
    val quantity: String?,
    val min: Int?,
    val max: Int?
) : IFmi3TypeDefinition

data class Int64TypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum,
    val quantity: String?,
    val min: Long?,
    val max: Long?
) : IFmi3TypeDefinition

data class BooleanTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum = Fmi3TypeEnum.BooleanType
) : IFmi3TypeDefinition

data class StringTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum = Fmi3TypeEnum.StringType
) : IFmi3TypeDefinition

data class BinaryTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum = Fmi3TypeEnum.BinaryType,
    val mimeType: String = "application/octet-stream",
    val maxSize: UInt?
) : IFmi3TypeDefinition

data class EnumerationTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum = Fmi3TypeEnum.EnumerationType,
    val quantity: String?
) : IFmi3TypeDefinition

data class ClockTypeDefinition(
    override val name: String,
    override val description: String?,
    override var typeIdentifier: Fmi3TypeEnum = Fmi3TypeEnum.ClockType,
    val canBeDeactivated: Boolean? = false,
    val priority: UInt?,
    val interval: Fmi3ClockInterval,
    val intervalDecimal: Float?,
    val shiftDecimal: Float? = (0).toFloat(),
    val supportsFraction: Boolean? = false,
    val resolution: ULong?,
    val intervalCounter: ULong?,
    val shiftCounter: ULong? = (0).toULong()
) : IFmi3TypeDefinition

interface IFmi3TypeDefinition {
    val name: String
    val description: String?
    var typeIdentifier: Fmi3TypeEnum
}

enum class Fmi3ClockInterval {
    Constant, Fixed, Calculated, Tunable, Changing, Countdown, Triggered
}

enum class Fmi3TypeEnum {
    Float32Type, Float64Type, Int8Type, UInt8Type, Int16Type, UInt16Type, Int32Type, UInt32Type, Int64Type,
    UInt64Type, BooleanType, StringType, BinaryType, EnumerationType, ClockType;

    companion object {
        fun fromVariableTypeAsString(variableTypeAsString: String): Fmi3TypeEnum =
            valueOf(variableTypeAsString.plus("Type"))
    }
}