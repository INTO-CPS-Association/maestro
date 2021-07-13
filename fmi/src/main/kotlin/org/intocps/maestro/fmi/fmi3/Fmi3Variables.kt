package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.ModelDescription

abstract class Fmi3Variable protected constructor(
    val name: String,
    val valueReference: UInt,
    val description: String?,
    val causality: Fmi3Causality,
    val variability: ModelDescription.Variability,
    val canHandleMultipleSetPerTimeInstant: Boolean?,
    val intermediateUpdate: Boolean?,
    val previous: UInt?,
    val clocks:  Any?, // Value references to clock variables
    val typeIdentifier: Fmi3TypeEnum // This is for easier type identification and is not part of the official spec
)

class FloatVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val initial: ModelDescription.Initial?,
    val quantity: String?,
    val unit: String?,
    val displayUnit: String?,
    val relativeQuantity: Boolean?,
    val unbounded: Boolean?,
    val min: Double?,
    val max: Double?,
    val nominal: Double?,
    val start: Collection<Double>?,
    val derivative: UInt?,
    val reinit: Boolean?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class Int64Variable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val initial: ModelDescription.Initial?,
    val quantity: String?,
    val min: Long?,
    val max: Long?,
    val start: List<Long>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class IntVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val initial: ModelDescription.Initial?,
    val quantity: String?,
    val min: Int?,
    val max: Int?,
    val start: List<Int>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class BooleanVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val initial: ModelDescription.Initial?,
    val start: List<Boolean>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class StringVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val start: List<String>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class BinaryVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val initial: ModelDescription.Initial?,
    val mimeType: String?,
    val maxSize: UInt?,
    val start: List<ByteArray>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class EnumerationVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val quantity: String?,
    val min: Long?,
    val max: Long?,
    val start: List<Long>?
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

class ClockVariable(
    name: String,
    valueReference: UInt,
    description: String?,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String?,
    val canBeDeactivated: Boolean?,
    val priority: UInt?,
    val interval: Any?, //TODO: Implement interval -> It declares the clock type
    val intervalDecimal: Float?,
    val shiftDecimal: Float? = (0).toFloat(),
    val supportsFraction: Boolean? = false,
    val resolution: ULong?,
    val intervalCounter: ULong?,
    val shiftCounter: ULong? = (0).toULong()
) : Fmi3Variable(
    name,
    valueReference,
    description,
    causality,
    variability,
    canHandleMultipleSetPerTimeInstant,
    intermediateUpdate,
    previous,
    clocks,
    typeIdentifier
)

enum class Fmi3Causality {
    StructuralParameter,
    Parameter,
    CalculatedParameter,
    Input,
    Output,
    Local,
    Independent
}
