import java.net.URI

import org.intocps.initializer.FMIASTFactory.ValueArrayVariables
import org.intocps.initializer.{ArrayVariable, FMIASTFactory, rawSingleValue}
import org.intocps.multimodelparser.data.{Connection, ConnectionScalarVariable, FMUWithMD, Instance}
import org.intocps.multimodelparser.parser.MultiModelConfiguration
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.{Causality, Initial, Type, Types, Variability}
import org.intocps.maestro.ast._

import scala.annotation.tailrec
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.jdk.CollectionConverters._


object MaBLSpec {

  def createMaBLSpec(fmus: Map[FMUWithMD, Set[Instance]], topSortedSvs: List[ConnectionScalarVariable], mmc: MultiModelConfiguration, extConnections: Set[Connection]): ABlockStm = {
    val stmStatusVariable: ALocalVariableStm = FMIASTFactory.createFmi2StatusVariable("status")
    val statusVariableTarget = FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmStatusVariable)

    // Create array for single valueReference
    val stmValueReferenceVariable: ALocalVariableStm = FMIASTFactory.createVariable(name = "initializer_ValueReference", varType = MableAstFactory.newAArrayType(MableAstFactory.newAUIntNumericPrimitiveType(), 1))
    val valueReferenceTarget = FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmValueReferenceVariable)

    // Create array for single real|int|boolean|string value
    val stmValueArrayInt: ALocalVariableStm = FMIASTFactory.createVariable(name = "initializer_int", varType = MableAstFactory.newAArrayType(MableAstFactory.newAIntNumericPrimitiveType(), 1))
    val stmValueArrayReal: ALocalVariableStm = FMIASTFactory.createVariable(name = "initializer_real", varType = MableAstFactory.newAArrayType(MableAstFactory.newARealNumericPrimitiveType(), 1))
    val stmValueArrayBool: ALocalVariableStm = FMIASTFactory.createVariable(name = "initializer_bool", varType = MableAstFactory.newAArrayType(MableAstFactory.newABoleanPrimitiveType(), 1))
    val stmValueArrayString: ALocalVariableStm = FMIASTFactory.createVariable(name = "initializer_string", varType = MableAstFactory.newAArrayType(MableAstFactory.newAStringPrimitiveType(), 1))
    val valueArrayVariables = ValueArrayVariables(
      FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmValueArrayReal),
      FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmValueArrayInt),
      FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmValueArrayString),
      FMIASTFactory.CONVALocalVariableStm2AIdentifierStateDesignator(stmValueArrayBool))


    val initializerStatements = fmus.foldLeft(Vector(): Seq[PStm]) { case (acc, (fmumd, insts)) => {
      // Load the FMU
      val fmuLoadStatement: AVariableDeclaration = FMIASTFactory.LoadFMU(fmumd.keyNoBrackets, new URI(mmc.fmus.getOrElse(fmumd.key, "FIXME")))

      // The scalar variables are calculated on FMU level instead of instance level as it applies to all instances of the given FMU.
      val scalarVariablesForFMU: Seq[ModelDescription.ScalarVariable] = fmumd.modelDescription.getScalarVariables.asScala.toSeq;
      val initialScalarVariables = getInitialScalarVariables(scalarVariablesForFMU)
      val independentScalarVariables = getIndependentScalarVariables(scalarVariablesForFMU.diff(initialScalarVariables))

      val instanceStatements: Seq[PStm] = insts.toVector.flatMap(instance => {
        // Instantiate the instances
        val instantiateInstance: AVariableDeclaration = FMIASTFactory.instantiate(fmuLoadStatement, instance.name)

        // Set the initial scalar variables.
        // The value for initial scalar variables is always the start value from the modelDescription file
        val setInitialScalarVariableNodes: Seq[PStm] = initialScalarVariables.flatMap(sv => {
          val valueToSet = sv.`type`.start
          FMIASTFactory.setScalarVariable(instance.name, sv, rawSingleValue(valueToSet, sv.`type`), valueReferenceTarget, valueArrayVariables, statusVariableTarget)
        })

        // Setup experiment
        val setupExperimentStm: AAssigmentStm = FMIASTFactory.setupExperiment(instance.name, statusVariableTarget)

        //EnterInitializationMode
        val enterInitializationModeStm: AAssigmentStm = FMIASTFactory.enterInitializationMode(instance.name, statusVariableTarget)

        // Set the independent scalar variables.
        // The value for the independent scalar variables might comes from the modelDescriptino file OR multimodel
        val setIndependentScalarVariableStatements: Seq[PStm] = independentScalarVariables.flatMap(sv => {
          val valueToSet = if (sv.causality == Causality.Parameter) {
            mmc.parameters.get("{%s}.%s.%s".format(fmumd.key, instance.name, sv.name)).map(_.getValue()).getOrElse(sv.`type`.start)
          }
          else {
            sv.`type`.start
          }

          FMIASTFactory.setScalarVariable(instance.name, sv, rawSingleValue(valueToSet, sv.`type`), valueReferenceTarget, valueArrayVariables, statusVariableTarget)
        })

        FMIASTFactory.variableDeclaration2Statement(instantiateInstance) +: setupExperimentStm +: setInitialScalarVariableNodes ++: enterInitializationModeStm +: setIndependentScalarVariableStatements
      }
      )
      val fmuRelatedStatements: Seq[PStm] =
        FMIASTFactory.variableDeclaration2Statement(fmuLoadStatement) +: instanceStatements
      acc ++ fmuRelatedStatements
    }
    }

    //     At this stage all the independent FMU and instance statements have been created.
    //     The next step is getting and setting inputs and outputs according to the topological sorting

    def sequence[A](a: Seq[Option[A]]) : Option[Seq[A]] = {
      @tailrec
      def iterate(seq: Seq[Option[A]], acc: Seq[A]) : Option[Seq[A]] = seq match {
        case Nil => Some(acc.reverse)
        case x :: xs => x match {
          case None => None
          case Some(value: A) => iterate(xs, value +: acc)
        }
      }
      iterate(a, Seq.empty)
    }

    val stmDependentVariables: Seq[Option[Seq[PStm]]] = topSortedSvs.map(connSv => {
      val enrichedConnectionScalarVariable: Option[EnrichedConnectionScalarVariable] = findFMUAndSv(connSv, fmus)
      enrichedConnectionScalarVariable.map(enrichedConnSV => {
        // Output Variables
        if (enrichedConnSV.scalarVariable.causality == Causality.Output) {
          FMIASTFactory.getScalarVariable(enrichedConnSV.instance, enrichedConnSV.scalarVariable, valueReferenceTarget, valueArrayVariables, statusVariableTarget)
        }
        // Input variables
        else {
          FMIASTFactory.setScalarVariable(enrichedConnSV.instance,
            enrichedConnSV.scalarVariable,
            ArrayVariable(valueArrayVariables, enrichedConnSV.scalarVariable.`type`),
            valueReferenceTarget,
            valueArrayVariables,
            statusVariableTarget)
        } })
    })
    sequence(stmDependentVariables)

    val statementsWithoutDependants: Seq[PStm] = (stmStatusVariable +: stmValueArrayInt +: stmValueArrayBool +: stmValueArrayReal +: stmValueArrayString +: initializerStatements)
    val finalStatements = sequence(stmDependentVariables) match {
      case None => statementsWithoutDependants
      case Some(value) => statementsWithoutDependants ++: value.flatten
    }

    MableAstFactory.newABlockStm(finalStatements.asJava)
  }

  // Independent Scalar Variables are scalar variables to set:
  // after invoking EnterInitializaionMode AND
  // that does not depend on other scalar variables to be set
  def getIndependentScalarVariables(scalars: Seq[ModelDescription.ScalarVariable]) = {
    scalars.filter(s =>
      (s.causality == Causality.Output && (s.initial != Initial.Calculated))
        || (s.causality == Causality.Parameter && s.initial != Initial.Calculated))
  }

  // Initial scalar variables are scalar variables to set prior to invoking EnterInitializationMode
  def getInitialScalarVariables(scalars: Seq[ModelDescription.ScalarVariable]) = {
    scalars.filter(s =>
      s.variability != Variability.Constant
        && ((s.initial == Initial.Exact || s.initial == Initial.Approx) || (s.initial == null && s.causality == Causality.Parameter))
        && (s.`type`.`type` != Types.Enumeration))
  }

  case class EnrichedConnectionScalarVariable(connSv: ConnectionScalarVariable, fmu: FMUWithMD, instance: String, scalarVariable: ModelDescription.ScalarVariable)

  def findFMUAndSv(connectionScalarVariable: ConnectionScalarVariable, fmus: Map[FMUWithMD, Set[Instance]]): Option[EnrichedConnectionScalarVariable] = {
    for {
      fmu <- fmus.keySet.find(x => x.key == connectionScalarVariable.vInstance.fmu)
      instances <- fmus.get(fmu)
      instance <- instances.find(x => x.name == connectionScalarVariable.vInstance.name)
      scalarVariable <- fmu.modelDescription.getScalarVariables.asScala.find(x => x.name == connectionScalarVariable.vName)
    }
      yield EnrichedConnectionScalarVariable(connectionScalarVariable, fmu, instance.name, scalarVariable)
  }

  def connSvToSv(connSv: ConnectionScalarVariable, fmus: Map[FMUWithMD, Set[Instance]]): Option[ModelDescription.ScalarVariable] = {
    fmus.keySet.find(x => x.key == connSv.vInstance.fmu).flatMap(x => x.modelDescription.getScalarVariables.asScala.find(x => x.name == connSv.vName))
  }
}
