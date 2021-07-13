package org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3

data class Fmi3ModelStructureElement(val elementType: Fmi3ModelStructureElementEnum, val valueReference: UInt, val dependencies: List<UInt>?, val dependenciesKind: List<Fmi3DependencyKind>?)

enum class Fmi3ModelStructureElementEnum {
    Output, ContinuousStateDerivative, ClockedState, InitialUnknown, EventIndicator
}

enum class Fmi3DependencyKind {
    Dependent, Constant, Fixed, Tunable, Discrete
}