package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.BaseModelDescription

abstract class Fmi3Variable protected constructor(
    val name: String,
    val valueReference: UInt?,
    val description: String?,
    val causality: Fmi3Causality?,
    val variability: BaseModelDescription.Variability?,
    val canHandleMultipleSetPerTimeInstant: Boolean?,
    val intermediateUpdate: Boolean?,
    val previous: UInt?,
    val clocks:  Any?, // Value references to clock variables
    val type: Fmi3TypeDefinition.Fmi3TypeIdentifiers // This is for easier type identification and is not part of the official spec
)

class FloatVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
    val declaredType: String?,
    val initial: BaseModelDescription.Initial?,
    val quantity: String?,
    val unit: String?,
    val displayUnit: String?,
    val relativeQuantity: Boolean?,
    val unbounded: Boolean?,
    val min: Double?,
    val max: Double?,
    val nominal: Double?,
    val start: Any?,
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
    type
)

class IntVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
    val declaredType: String?,
    val initial: BaseModelDescription.Initial?,
    val quantity: String?,
    val min: Double?,
    val max: Double?,
    val start: Any?
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
    type
)

class BooleanVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
    val declaredType: String?,
    val initial: BaseModelDescription.Initial?,
    val start: Any?
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
    type
)

class StringVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers
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
    type
)

class BinaryVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
    val declaredType: String?,
    val initial: BaseModelDescription.Initial?,
    val mimeType: String?,
    val maxSize: UInt?,
    val start: Any?
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
    type
)

class EnumerationVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
    val declaredType: String?,
    val quantity: String?,
    val min: Long?,
    val max: Long?,
    val start: Any?
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
    type
)

class ClockVariable(
    name: String,
    valueReference: UInt?,
    description: String?,
    causality: Fmi3Causality?,
    variability: BaseModelDescription.Variability?,
    canHandleMultipleSetPerTimeInstant: Boolean?,
    intermediateUpdate: Boolean?,
    previous: UInt?,
    clocks: Any?, // Value references to clock variables
    type: Fmi3TypeDefinition.Fmi3TypeIdentifiers,
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
    type
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
