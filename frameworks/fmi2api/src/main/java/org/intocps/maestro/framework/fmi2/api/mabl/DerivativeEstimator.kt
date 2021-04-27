package org.intocps.maestro.framework.fmi2.api.mabl

import org.intocps.maestro.ast.MableAstFactory.*
import org.intocps.maestro.ast.node.ALocalVariableStm
import org.intocps.maestro.ast.node.PExp
import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder.RuntimeModule
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api

class DerivativeEstimator(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val runtimeModule: RuntimeModule<PStm>?) {
    private val moduleIdentifier: String = runtimeModule?.name ?: "derivativeEstimator"

    constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder) : this(dynamicScope, mablApiBuilder, null)

    fun createDerivativeEstimatorInstance(): DerivativeEstimatorInstance {
        return DerivativeEstimatorInstance(dynamicScope, mablApiBuilder, this, runtimeModule)
    }

    fun getModuleIdentifier(): String {
        return moduleIdentifier
    }

    fun unload() {
        mablApiBuilder.getDynamicScope().add(newExpressionStm(newUnloadExp(listOf(getReferenceExp().clone()))))
    }

    private fun getReferenceExp(): PExp {
        return newAIdentifierExp(moduleIdentifier)
    }

    class DerivativeEstimatorInstance(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val derivativeEstimator: DerivativeEstimator, private val runtimeModule: RuntimeModule<PStm>?) {

        private val TYPE_DERIVATIVEESTIMATORINSTANCE = "DerivativeEstimatorInstance"
        private val identifier_derivativeEstimatorInstance: String = mablApiBuilder.getNameGenerator().getName("derivative_estimator_instance")
        private val identifier_indicesOfInterest: String = mablApiBuilder.getNameGenerator().getName("indices_of_interest")
        private val identifier_derivativeOrders: String = mablApiBuilder.getNameGenerator().getName("derivative_orders")
        private val identifier_providedDerivativeOrder: String = mablApiBuilder.getNameGenerator().getName("provided_derivative_order")
        private val identifier_sharedData: String = mablApiBuilder.getNameGenerator().getName("shared_data")
        private val identifier_sharedDerivatives: String = mablApiBuilder.getNameGenerator().getName("shared_derivatives")

        constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder, derivativeEstimator: DerivativeEstimator) : this(dynamicScope, mablApiBuilder, derivativeEstimator, null)

        /**
         * indicesOfInterest is related to the buffer that is to be passed in subsequent estimate calls.
         * derivativeOrders is the derivative order and related to indicesOfInterest.
         * providedDerivativeOrder is the derivatives that are externally provided and thus should be ignored. It is also related to indicesOfInterest.
         */
        fun initialize(indicesOfInterest: List<VariableFmi2Api<Int>>, derivativeOrders: List<VariableFmi2Api<Int>>, providedDerivativeOrder: List<VariableFmi2Api<Int>>) {
            val identifier_createFunction = "create"

            // Generate indices of interest statement
            val indices: List<PExp> = indicesOfInterest.map { i -> i.exp }
            val indicesOfInterestStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(identifier_indicesOfInterest),
                    newAArrayType(newAStringPrimitiveType()), indices.size,
                    newAArrayInitializer(indices)))

            // Generate orders statement
            val orders: List<PExp> = derivativeOrders.map { i -> i.exp }
            val derivativeOrdersStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(identifier_derivativeOrders),
                    newAArrayType(newAStringPrimitiveType()), orders.size,
                    newAArrayInitializer(orders)))

            // Generate orders statement
            val providedDerivativesOrders: List<PExp> = providedDerivativeOrder.map { i -> i.exp }
            val providedDerivativeOrderStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(identifier_providedDerivativeOrder),
                    newAArrayType(newAStringPrimitiveType()), providedDerivativesOrders.size,
                    newAArrayInitializer(providedDerivativesOrders)))


            // Generate create function call
            val createFunctionStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(identifier_derivativeEstimatorInstance),
                    newANameType(TYPE_DERIVATIVEESTIMATORINSTANCE), newAExpInitializer(newACallExp(newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                    newAIdentifier(identifier_createFunction),
                    listOf(newAIdentifierExp(identifier_indicesOfInterest),
                            newAIdentifierExp(identifier_derivativeOrders), newAIdentifierExp(identifier_providedDerivativeOrder))))))

            runtimeModule?.declaredScope?.add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)
                    ?: mablApiBuilder.getDynamicScope().add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)

        }

        /**
         * time is the time of data
         * sharedData is the value associated with indicesOfInterest.
            E.g.: if indicesOfInterest={1,3,4} and sharedData={0,4,3,1,56,30} then the ones used subsequently are: {4, 1, 56}.
         * sharedDataDerivatives is the buffer to contain the derivatives. It will be tampered with related to indiciesOfInterest.
            E.g.: Input function argument values: indicesOfInterest={1}, order=[2], provided=[0] and sharedDataDerivatives={{3,4},{0,0}}.
            Argument values after function execution:
            sharedDataDerivatives={{3,4},{DER(DER(variablez)),DER(DER(variableY))}}
         */
        fun estimate(time: DoubleVariableFmi2Api, sharedData: List<VariableFmi2Api<Double>>, sharedDataDerivatives: List<ArrayVariableFmi2Api<Double>>) {
            val identifier_estimateFunction = "estimate"

            // Generate estimate function
            val sharedDataExp = sharedData.map { i -> i.exp }
            val sharedDataStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(identifier_sharedData),
                    newAArrayType(newAStringPrimitiveType()), sharedDataExp.size,
                    newAArrayInitializer(sharedDataExp)))
            dynamicScope.add(sharedDataStm)

            val sharedDataDerivativesExp = sharedDataDerivatives.map { i -> i.exp }
            val sharedDataDerivativesStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclarationMultiDimensionalArray(newAIdentifier(identifier_sharedDerivatives),
                    newARealNumericPrimitiveType(),
                    listOf(sharedDataDerivativesExp.size, sharedDataDerivatives[0].items().size)))
            dynamicScope.add(sharedDataDerivativesStm)

            (0..sharedDataDerivatives.size).forEach { outerIndex ->
                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(identifier_sharedDerivatives)), newAIntLiteralExp(outerIndex)).let { innerIdentifier ->
                    (0..sharedDataDerivatives[outerIndex].items().size).forEach { innerIndex ->
                        newAArayStateDesignator(innerIdentifier, newAIntLiteralExp(innerIndex)).also { outerIdentifier ->
                            dynamicScope.add(newAAssignmentStm(outerIdentifier, sharedDataDerivatives[outerIndex].items()[innerIndex].exp))
                        }
                    }
                }
            }

            val estimateDerivativesStm = newExpressionStm(newACallExp(newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                    newAIdentifier(identifier_estimateFunction),
                    listOf(time.exp, newAIdentifierExp(identifier_sharedData), newAIdentifierExp(identifier_sharedDerivatives))))

            dynamicScope.add(estimateDerivativesStm)
        }
    }
}