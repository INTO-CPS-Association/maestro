package org.intocps.maestro.framework.fmi2.api

import org.intocps.maestro.ast.MableAstFactory
import org.intocps.maestro.ast.node.PExp
import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api

class DerivativeEstimator(
    private val dynamicScope: DynamicActiveBuilderScope,
    private val mablApiBuilder: MablApiBuilder,
    private val runtimeModule: Fmi2Builder.RuntimeModule<PStm>?
) {
    private val moduleIdentifier: String = runtimeModule?.name ?: "derivativeEstimator"

    constructor(mablApiBuilder: MablApiBuilder) : this(
        mablApiBuilder.dynamicScope,
        mablApiBuilder,
        null
    )

    fun createDerivativeEstimatorInstance(): DerivativeEstimatorInstance {
        return DerivativeEstimatorInstance(dynamicScope, mablApiBuilder, this, runtimeModule)
    }

    fun getModuleIdentifier(): String {
        return moduleIdentifier
    }

    fun unload() {
        mablApiBuilder.dynamicScope
            .add(MableAstFactory.newExpressionStm(MableAstFactory.newUnloadExp(listOf(getReferenceExp().clone()))))
    }

    private fun getReferenceExp(): PExp {
        return MableAstFactory.newAIdentifierExp(moduleIdentifier)
    }

    class DerivativeEstimatorInstance(
        private val dynamicScope: DynamicActiveBuilderScope,
        private val mablApiBuilder: MablApiBuilder,
        private val derivativeEstimator: DerivativeEstimator,
        private val runtimeModule: Fmi2Builder.RuntimeModule<PStm>?
    ) {

        private val TYPE_DERIVATIVEESTIMATORINSTANCE = "DerivativeEstimatorInstance"
        private val identifier_derivativeEstimatorInstance: String =
            mablApiBuilder.nameGenerator.getName("derivativeEstimatorInstance")
        private var isInitialized: Boolean = false

        constructor(
            dynamicScope: DynamicActiveBuilderScope,
            mablApiBuilder: MablApiBuilder,
            derivativeEstimator: DerivativeEstimator
        ) : this(dynamicScope, mablApiBuilder, derivativeEstimator, null)

        /**
         * [indicesOfInterest] is related to the buffer that is to be passed in subsequent estimate calls.
         * [derivativeOrders] is the derivative order and related to indicesOfInterest.
         * [providedDerivativeOrder] is the derivatives that are externally provided and thus should be ignored. It is also related to indicesOfInterest.
         */
        fun initialize(
            indicesOfInterest: ArrayVariableFmi2Api<Int>,
            derivativeOrders: ArrayVariableFmi2Api<Int>,
            providedDerivativeOrder: ArrayVariableFmi2Api<Int>
        ) {
            val createFunctionIdentifier = "create"

            // Generate create function call
            val createFunctionStm = MableAstFactory.newALocalVariableStm(
                MableAstFactory.newAVariableDeclaration(
                    MableAstFactory.newAIdentifier(identifier_derivativeEstimatorInstance),
                    MableAstFactory.newANameType(TYPE_DERIVATIVEESTIMATORINSTANCE),
                    MableAstFactory.newAExpInitializer(
                        MableAstFactory.newACallExp(
                            MableAstFactory.newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                            MableAstFactory.newAIdentifier(createFunctionIdentifier),
                            listOf(
                                MableAstFactory.newAIdentifierExp(indicesOfInterest.name),
                                MableAstFactory.newAIdentifierExp(derivativeOrders.name),
                                MableAstFactory.newAIdentifierExp(providedDerivativeOrder.name)
                            )
                        )
                    )
                )
            )

            mablApiBuilder.dynamicScope.add(createFunctionStm)

            isInitialized = true
        }

        /**
         *
         * [time] is the n-th time of the data.
         * [sharedData] is the value associated with indicesOfInterest. E.g.: if indicesOfInterest={1,3,4} and sharedData={0,4,3,1,56,30} then the ones used subsequently are: {4, 1, 56}.
         * [sharedDataDerivatives] is the buffer to contain the derivatives.
         * It will be tampered with related to indicesOfInterest. E.g.: Input function argument values: indicesOfInterest={1}, order={2}, provided={0} and sharedDataDerivatives={{3,4},{0,0}}.
         * Argument values after function execution: sharedDataDerivatives={{3,4},{DER(DER(variableX)),DER(DER(variableY))}}
         */
        fun estimate(
            time: DoubleVariableFmi2Api,
            sharedData: ArrayVariableFmi2Api<Double>,
            sharedDataDerivatives: ArrayVariableFmi2Api<Array<Double>>
        ) {
            if (!isInitialized) return

            val estimateFunctionIdentifier = "estimate"

            val estimateDerivativesStm = MableAstFactory.newExpressionStm(
                MableAstFactory.newACallExp(
                    MableAstFactory.newAIdentifierExp(identifier_derivativeEstimatorInstance),
                    MableAstFactory.newAIdentifier(estimateFunctionIdentifier),
                    listOf(
                        time.exp,
                        MableAstFactory.newAIdentifierExp(sharedData.name),
                        MableAstFactory.newAIdentifierExp(sharedDataDerivatives.name)
                    )
                )
            )

            dynamicScope.add(estimateDerivativesStm)
        }

        /**
         * [sharedDataDerivatives] should contain the derivatives to be rolled back.
         */
        fun rollback(sharedDataDerivatives: ArrayVariableFmi2Api<Array<Double>>) {
            if (!isInitialized) return

            val rollbackFunctionIdentifier = "rollback"

            val estimateDerivativesStm = MableAstFactory.newExpressionStm(
                MableAstFactory.newACallExp(
                    MableAstFactory.newAIdentifierExp(identifier_derivativeEstimatorInstance),
                    MableAstFactory.newAIdentifier(rollbackFunctionIdentifier),
                    listOf(MableAstFactory.newAIdentifierExp(sharedDataDerivatives.name))
                )
            )

            dynamicScope.add(estimateDerivativesStm)
        }
    }
}