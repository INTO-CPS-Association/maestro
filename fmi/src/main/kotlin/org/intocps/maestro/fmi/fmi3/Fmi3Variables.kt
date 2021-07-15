package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.fmi.ModelDescription

abstract class Fmi3Variable protected constructor(
    val name: String,
    val valueReference: UInt,
    val description: String? = null,
    val causality: Fmi3Causality,
    val variability: ModelDescription.Variability,
    val canHandleMultipleSetPerTimeInstant: Boolean? = null,
    val intermediateUpdate: Boolean? = false,
    val previous: UInt? = null,
    val clocks: List<UInt>? = null,
    val typeIdentifier: Fmi3TypeEnum // This is for easier type identification and is not part of the official spec
)

data class Dimension(val valueReference: UInt?, val start: List<Long>?)

class FloatVariable(
    name: String,
    valueReference: UInt,
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val initial: ModelDescription.Initial? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val displayUnit: String? = null,
    val relativeQuantity: Boolean? = false,
    val unbounded: Boolean? = false,
    val min: Double? = null,
    val max: Double? = null,
    val nominal: Double? = null,
    val start: Collection<Double>? = null,
    val derivative: UInt? = null,
    val reinit: Boolean? = false,
    val dimensions: List<Dimension>?  = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val initial: ModelDescription.Initial? = null,
    val quantity: String? = null,
    val min: Long? = null,
    val max: Long? = null,
    val start: List<Long>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val initial: ModelDescription.Initial? = null,
    val quantity: String? = null,
    val min: Int? = null,
    val max: Int? = null,
    val start: List<Int>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val initial: ModelDescription.Initial? = null,
    val start: List<Boolean>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val start: List<String>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val initial: ModelDescription.Initial? = null,
    val mimeType: String? = null,
    val maxSize: UInt? = null,
    val start: List<ByteArray>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val quantity: String? = null,
    val min: Long? = null,
    val max: Long? = null,
    val start: List<Long>? = null,
    val dimensions: List<Dimension>? = null
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
    description: String? = null,
    causality: Fmi3Causality,
    variability: ModelDescription.Variability,
    canHandleMultipleSetPerTimeInstant: Boolean? = null,
    intermediateUpdate: Boolean? = false,
    previous: UInt? = null,
    clocks: List<UInt>? = null,
    typeIdentifier: Fmi3TypeEnum,
    val declaredType: String? = null,
    val canBeDeactivated: Boolean? = false,
    val priority: UInt? = null,
    val interval: Fmi3ClockInterval,
    val intervalDecimal: Float? = null,
    val shiftDecimal: Float? = (0).toFloat(),
    val supportsFraction: Boolean? = false,
    val resolution: ULong? = null,
    val intervalCounter: ULong? = null,
    val shiftCounter: ULong? = (0).toULong(),
    val dimensions: List<Dimension>? = null
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
