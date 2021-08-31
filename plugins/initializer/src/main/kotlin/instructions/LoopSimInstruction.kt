package org.intocps.maestro.plugin.initializer.instructions

import org.intocps.maestro.ast.node.PStm
import org.intocps.maestro.fmi.Fmi2ModelDescription
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder
import org.intocps.maestro.framework.fmi2.api.mabl.BooleanBuilderFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.MathBuilderFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.PortFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.BooleanVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.ComponentVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.IntVariableFmi2Api
import org.intocps.maestro.framework.fmi2.api.mabl.variables.VariableFmi2Api

class LoopSimInstruction(scope: Fmi2Builder.Scope<*>, private val maxStepAcceptAttempts: Fmi2Builder.IntVariable<PStm>,
                         private val absoluteTolerance: Fmi2Builder.DoubleVariable<PStm>, private val relativeTolerance: Fmi2Builder.DoubleVariable<PStm>,
                         simulationActions: List<CoSimInstruction>,
                         private val convergencePorts: Map<ComponentVariableFmi2Api, Map<PortFmi2Api, VariableFmi2Api<Any>>>,
                         private val booleanLogic: BooleanBuilderFmi2Api,
                         private val math: MathBuilderFmi2Api) : ComplexCoSimInstruction(simulationActions, scope) {


    override fun perform() {
        val algebraicLoop = scope.store( 5) as IntVariableFmi2Api;
        val basis = scope.store(0) as IntVariableFmi2Api;

        val convergenceReached : BooleanVariableFmi2Api = scope.store("hasConverged", false) as BooleanVariableFmi2Api;
        val stabilisationScope = scope
                .enterWhile(convergenceReached.toPredicate().and(algebraicLoop.toMath().greaterThan(basis.toMath())));

        run {
            simulationActions.forEach { action: CoSimInstruction -> action.perform() }

            val convergenceVariables: List<BooleanVariableFmi2Api> = convergencePorts.entries.flatMap { (fmu, ports) ->
                ports.entries.filter { (p, _) -> p.scalarVariable.type.type == Fmi2ModelDescription.Types.Real }
                        .map { (port, v) ->
                            val oldVariable: VariableFmi2Api<Any> = port.sharedAsVariable
                            val newVariable: VariableFmi2Api<Any> = v
                            math.checkConvergence(oldVariable, newVariable, absoluteTolerance, relativeTolerance)
                        }
            }
            convergenceReached.setValue(booleanLogic.allTrue("convergence", convergenceVariables));

            val ifScope = scope.enterIf(convergenceReached.toPredicate().not()).enterThen();
            algebraicLoop.decrement()

            convergencePorts.forEach { (k, v) -> k.share(v) }
            stabilisationScope.activate();

            ifScope.activate()
        }
    }
}