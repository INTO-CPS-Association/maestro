package org.intocps.maestro.framework.fmi2.api

import org.intocps.maestro.ast.MableAstFactory
import org.intocps.maestro.ast.node.ALocalVariableStm
import org.intocps.maestro.ast.node.PExp
import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ArrayVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.DoubleVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api

class DerivativeEstimator(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val runtimeModule: Fmi2Builder.RuntimeModule<PStm>?) {
    private val moduleIdentifier: String = runtimeModule?.name ?: "derivativeEstimator"

    constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder) : this(dynamicScope, mablApiBuilder, null)

    fun createDerivativeEstimatorInstance(): DerivativeEstimatorInstance {
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

    class DerivativeEstimatorInstance(private val dynamicScope: DynamicActiveBuilderScope, private val mablApiBuilder: MablApiBuilder, private val derivativeEstimator: DerivativeEstimator, private val runtimeModule: Fmi2Builder.RuntimeModule<PStm>?) {

        private val TYPE_DERIVATIVEESTIMATORINSTANCE = "DerivativeEstimatorInstance"
        private val identifier_derivativeEstimatorInstance: String = mablApiBuilder.nameGenerator.getName("derivativeEstimatorInstance")
        private val identifier_indicesOfInterest: String = mablApiBuilder.nameGenerator.getName("indices_of_interest")
        private val identifier_derivativeOrders: String = mablApiBuilder.nameGenerator.getName("derivative_orders")
        private val identifier_providedDerivativeOrder: String = mablApiBuilder.nameGenerator.getName("provided_derivative_order")
        private val identifier_sharedData: String = mablApiBuilder.nameGenerator.getName("shared_data")
        private val identifier_sharedDerivatives: String = mablApiBuilder.nameGenerator.getName("shared_derivatives")
        private var isInitialized: Boolean = false

        constructor(dynamicScope: DynamicActiveBuilderScope, mablApiBuilder: MablApiBuilder, derivativeEstimator: DerivativeEstimator) : this(dynamicScope, mablApiBuilder, derivativeEstimator, null)

        /**
         * indicesOfInterest is related to the buffer that is to be passed in subsequent estimate calls.
         * derivativeOrders is the derivative order and related to indicesOfInterest.
         * providedDerivativeOrder is the derivatives that are externally provided and thus should be ignored. It is also related to indicesOfInterest.
         */
        fun initialize(indicesOfInterest: List<VariableFmi2Api<Long>>, derivativeOrders: List<VariableFmi2Api<Long>>, providedDerivativeOrder: List<VariableFmi2Api<Long>>) {
            val identifier_createFunction = "create"

            // Generate indices of interest statement
            val indices: List<PExp> = indicesOfInterest.map { i -> i.exp }
            val indicesOfInterestStm: ALocalVariableStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(identifier_indicesOfInterest),
                    MableAstFactory.newAArrayType(MableAstFactory.newAUIntNumericPrimitiveType()), indices.size,
                    MableAstFactory.newAArrayInitializer(indices)))

            // Generate orders statement
            val orders: List<PExp> = derivativeOrders.map { i -> i.exp }
            val derivativeOrdersStm: ALocalVariableStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(identifier_derivativeOrders),
                    MableAstFactory.newAArrayType(MableAstFactory.newAUIntNumericPrimitiveType()), orders.size,
                    MableAstFactory.newAArrayInitializer(orders)))

            // Generate orders statement
            val providedDerivativesOrders: List<PExp> = providedDerivativeOrder.map { i -> i.exp }
            val providedDerivativeOrderStm: ALocalVariableStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(identifier_providedDerivativeOrder),
                    MableAstFactory.newAArrayType(MableAstFactory.newAUIntNumericPrimitiveType()), providedDerivativesOrders.size,
                    MableAstFactory.newAArrayInitializer(providedDerivativesOrders)))


            // Generate create function call
            val createFunctionStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(identifier_derivativeEstimatorInstance),
                    MableAstFactory.newANameType(TYPE_DERIVATIVEESTIMATORINSTANCE), MableAstFactory.newAExpInitializer(MableAstFactory.newACallExp(MableAstFactory.newAIdentifierExp(this.derivativeEstimator.getModuleIdentifier()),
                    MableAstFactory.newAIdentifier(identifier_createFunction),
                    listOf(MableAstFactory.newAIdentifierExp(identifier_indicesOfInterest),
                            MableAstFactory.newAIdentifierExp(identifier_derivativeOrders), MableAstFactory.newAIdentifierExp(identifier_providedDerivativeOrder))))))

            runtimeModule?.declaredScope?.add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)
                    ?: mablApiBuilder.getDynamicScope().add(indicesOfInterestStm, derivativeOrdersStm, providedDerivativeOrderStm, createFunctionStm)

            isInitialized = true
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
        fun estimate(time: DoubleVariableFmi2Api, sharedData: List<VariableFmi2Api<Double>>, sharedDataDerivatives: ArrayVariableFmi2Api<Any>) {
            if(!isInitialized) return

            val identifier_estimateFunction = "estimate"

            // Generate estimate function
            val sharedDataExp = sharedData.map { i -> i.exp }
            val sharedDataStm: ALocalVariableStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(MableAstFactory.newAIdentifier(identifier_sharedData),
                    MableAstFactory.newAArrayType(MableAstFactory.newARealNumericPrimitiveType()), sharedDataExp.size,
                    MableAstFactory.newAArrayInitializer(sharedDataExp)))
            dynamicScope.add(sharedDataStm)

//            val sharedDataDerivativesExp = sharedDataDerivatives.map { i -> i.exp }
//            val sharedDataDerivativesStm: ALocalVariableStm = MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclarationMultiDimensionalArray(MableAstFactory.newAIdentifier(identifier_sharedDerivatives),
//                    MableAstFactory.newARealNumericPrimitiveType(),
//                    listOf(sharedDataDerivativesExp.size, sharedDataDerivatives[0].items().size)))
//            dynamicScope.add(sharedDataDerivativesStm)
//
//            (0..sharedDataDerivatives.size).forEach { outerIndex ->
//                MableAstFactory.newAArayStateDesignator(MableAstFactory.newAIdentifierStateDesignator(MableAstFactory.newAIdentifier(identifier_sharedDerivatives)), MableAstFactory.newAIntLiteralExp(outerIndex)).let { innerIdentifier ->
//                    (0..sharedDataDerivatives[outerIndex].items().size).forEach { innerIndex ->
//                        MableAstFactory.newAArayStateDesignator(innerIdentifier, MableAstFactory.newAIntLiteralExp(innerIndex)).also { outerIdentifier ->
//                            dynamicScope.add(MableAstFactory.newAAssignmentStm(outerIdentifier, sharedDataDerivatives[outerIndex].items()[innerIndex].exp))
//                        }
//                    }
//                }
//            }

            val estimateDerivativesStm = MableAstFactory.newExpressionStm(MableAstFactory.newACallExp(MableAstFactory.newAIdentifierExp(identifier_derivativeEstimatorInstance),
                    MableAstFactory.newAIdentifier(identifier_estimateFunction),
                    listOf(time.exp, MableAstFactory.newAIdentifierExp(identifier_sharedData), MableAstFactory.newAIdentifierExp(sharedDataDerivatives.name))))

            dynamicScope.add(estimateDerivativesStm)
        }
    }
}