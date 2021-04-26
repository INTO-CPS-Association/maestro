package org.intocps.maestro.framework.fmi2.api.mabl

import org.intocps.maestro.ast.MableAstFactory
import org.intocps.maestro.ast.MableAstFactory.*
import org.intocps.maestro.ast.node.*
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder.RuntimeModule
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api

class DerivativeEstimator(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val runtimeModule: RuntimeModule<PStm>?) {
    private val moduleIdentifier: String = runtimeModule?.name ?: "derivativeEstimator"

    constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder) : this(dynamicScope, mablApiBuilder, null)

    fun createVariableStepInstanceInstance(): DerivativeEstimatorInstance {
        return DerivativeEstimatorInstance(dynamicScope, mablApiBuilder, this, runtimeModule)
    }

    fun getModuleIdentifier(): String {
        return moduleIdentifier
    }

    fun unload() {
        mablApiBuilder.getDynamicScope().add(MableAstFactory.newExpressionStm(MableAstFactory.newUnloadExp(listOf(getReferenceExp().clone()))))
    }

    private fun getReferenceExp(): PExp {
        return MableAstFactory.newAIdentifierExp(moduleIdentifier)
    }

    class DerivativeEstimatorInstance(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val derivativeEstimator: DerivativeEstimator, private val runtimeModule: RuntimeModule<PStm>?) {

        private val TYPE_DERIVATIVEESTIMATORINSTANCE = "DerivativeEstimatorInstance"
        private val derivativeEstimatorInstanceIdentifier: String = mablApiBuilder.getNameGenerator().getName("derivative_estimator_instance")
        private val indicesOfInterestIdentifier: String = mablApiBuilder.getNameGenerator().getName("indices_of_interest")
        private val derivativeOrdersIdentifier: String = mablApiBuilder.getNameGenerator().getName("derivative_orders")
        private val providedDerivativeOrderIdentifier: String = mablApiBuilder.getNameGenerator().getName("provided_derivative_order")
        private val sharedDataIdentifier: String = mablApiBuilder.getNameGenerator().getName("shared_data_for_derivatives")

        constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder, derivativeEstimator: DerivativeEstimator) : this(dynamicScope, mablApiBuilder, derivativeEstimator, null)

        fun initialize(indicesOfInterest: List<IntVariableFmi2Api>, derivativeOrders: List<IntVariableFmi2Api>, providedDerivativeOrder: List<IntVariableFmi2Api>) {
            val functionIdentifier_create = "create"

            // Generate indices of interest statement
            val indices: List<PExp> = indicesOfInterest.map { i -> i.exp }
            val indicesOfInterestStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(indicesOfInterestIdentifier),
                    newAArrayType(newAStringPrimitiveType()), indices.size,
                    newAArrayInitializer(indices)))

            // Generate orders statement
            val orders: List<PExp> = derivativeOrders.map { i -> i.exp }
            val derivativeOrdersStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(derivativeOrdersIdentifier),
                    newAArrayType(newAStringPrimitiveType()), orders.size,
                    newAArrayInitializer(orders)))

            // Generate orders statement
            val providedDerivativesOrders: List<PExp> = providedDerivativeOrder.map { i -> i.exp }
            val providedDerivativeOrderStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(providedDerivativeOrderIdentifier),
                    newAArrayType(newAStringPrimitiveType()), providedDerivativesOrders.size,
                    newAArrayInitializer(providedDerivativesOrders)))


            // Generate create function call
            val createFunctionStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(derivativeEstimatorInstanceIdentifier),
                    newANameType(TYPE_DERIVATIVEESTIMATORINSTANCE), newAExpInitializer(newACallExp(newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                    newAIdentifier(functionIdentifier_create),
                    listOf(newAIdentifierExp(indicesOfInterestIdentifier),
                            newAIdentifierExp(derivativeOrdersIdentifier), newAIdentifierExp(providedDerivativeOrderIdentifier))))))

            runtimeModule?.declaredScope?.add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)
                    ?: mablApiBuilder.getDynamicScope().add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)

        }

        fun estimate(time: DoubleVariableFmi2Api, sharedData: List<VariableFmi2Api<Any>>, sharedDataDerivatives: List<ArrayVariableFmi2Api<Any>>) {
            val functionIdentifier_estimate = "estimate"

            // Generate estimate function
            val sharedDataExp = sharedData.map { i -> i.exp }
            val sharedDataStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(sharedDataIdentifier),
                    newAArrayType(newAStringPrimitiveType()), sharedDataExp.size,
                    newAArrayInitializer(sharedDataExp)))
            this.dynamicScope.add(sharedDataStm)

            val sharedDataDerivativesExp = sharedDataDerivatives.map { i -> i.exp }
            val sharedDataDerivativesStm: ALocalVariableStm = newALocalVariableStm(newAVariableDeclarationMultiDimensionalArray(newAIdentifier(sharedDataIdentifier),
                    newARealNumericPrimitiveType(),
                    listOf(sharedDataDerivativesExp.size, sharedDataDerivatives[0].items().size)))
            this.dynamicScope.add(sharedDataDerivativesStm)



            for (outerIndex in 0..sharedDataDerivatives.size) {
                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(sharedDataIdentifier)), newAIntLiteralExp(outerIndex)).let{ innerIdentifier ->
                    for(innerIndex in 0..sharedDataDerivatives[outerIndex].items().size){
                    newAArayStateDesignator(innerIdentifier,newAIntLiteralExp(innerIndex)).let { outerIdentifier -> this.dynamicScope.add(newAAssignmentStm(outerIdentifier, sharedDataDerivatives[outerIndex].items()[innerIndex].exp)) }
                } }
            }


            val estimateDerivativesStm = newExpressionStm(newACallExp(newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                    newAIdentifier(functionIdentifier_estimate),
                    listOf(time.exp)))
        }


    }
}