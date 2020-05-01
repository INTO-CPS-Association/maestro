import java.net.URI

import org.intocps.initializer.FMIASTFactory
import org.intocps.multimodelparser.data.{Connection, ConnectionScalarVariable, FMUWithMD, Instance}
import org.intocps.multimodelparser.parser.MultiModelConfiguration
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.{Causality, Initial, Type, Types, Variability}
import org.intocps.maestro.ast._

import scala.jdk.CollectionConverters._


object MaBLSpec {



  ?

  def createMaBLSpec(fmus: Map[FMUWithMD, Set[Instance]], topSortedSvs: List[ConnectionScalarVariable], mmc: MultiModelConfiguration, extConnections: Set[Connection]): ABlockStm = {
    val statusVariable: ALocalVariableStm = FMIASTFactory.createFmi2StatusVariable("status")
    val statusVariableAsExp = FMIASTFactory.aLocalVariableStmToAIdentifierExp(statusVariable)
    val initializerStatements = fmus.foldLeft(Vector() : Seq[PStm]) { case (acc, (fmumd, insts)) => {
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
        val setInitialScalarVariableNodes: Seq[AAssigmentStm] = initialScalarVariables.map(sv => {
          val valueToSet = sv.`type`.start
          FMIASTFactory.setScalarVariable(instance.name, sv, valueToSet, statusVariableAsExp)
          })

        // Setup experiment
        val setupExperimentStm : AAssigmentStm = FMIASTFactory.setupExperiment(instance.name, statusVariableAsExp)

        //EnterInitializationMode
        val enterInitializationModeStm : AAssigmentStm = FMIASTFactory.enterInitializationMode(instance.name, statusVariableAsExp)

        // Set the independent scalar variables.
        // The value for the independent scalar variables might comes from the modelDescriptino file OR multimodel
        val setIndependentScalarVariableStatements: Seq[AAssigmentStm] = independentScalarVariables.map(sv => {
          val valueToSet = if(sv.causality == Causality.Parameter ) {
            mmc.parameters.get("{%s}.%s.%s".format(fmumd.key, instance.name, sv.name)).map(_.getValue()).getOrElse(sv.`type`.start)
          }
          else {
            sv.`type`.start
          }

          FMIASTFactory.setScalarVariable(instance.name, sv, valueToSet, statusVariableAsExp)
        })

        FMIASTFactory.variableDeclaration2Statement(instantiateInstance) +: setupExperimentStm +: setInitialScalarVariableNodes ++: enterInitializationModeStm +: setIndependentScalarVariableStatements
      }
      )
      val fmuRelatedStatements: Seq[PStm] = FMIASTFactory.variableDeclaration2Statement(fmuLoadStatement) +: instanceStatements
      acc ++ fmuRelatedStatements
    }
    }

    // At this stage all the independent FMU and instance statements have been created.
    // The next step is getting and setting inputs and outputs according to the topological sorting

    topSortedSvs.map(connSv => {
      val enrichedConnectionScalarVariable: Option[EnrichedConnectionScalarVariable] = findFMUAndSv(connSv, fmus)
      enrichedConnectionScalarVariable.map(enrichedConnSV =>{

        //If the variable is an output, then it is necessary to store the result in a variable and build up a map as we go.
        if(enrichedConnSV.scalarVariable.causality == Causality.Output) {
          FMIASTFactory.getScalarVariable(enrichedConnSV.instance, enrichedConnSV.scalarVariable, statusVariable.getDeclaration)
        }
          // The variable is an input.
        else {
          //Find the corresponding output to locate the variable name.
          val relatedOutput: Option[Connection] = extConnections.find(x => x.to.exists(conn => conn.vName == enrichedConnSV.scalarVariable.name && conn.vInstance.name == enrichedConnSV.instance))
          relatedOutput.map(output => {
            val variableName = output.from.vInstance.name+output.from.vName
            FMIASTFactory.setScalarVariable(enrichedConnSV.instance, enrichedConnSV.scalarVariable, variableName, statusVariable)
          }

          )

        }

      }
      )
    })

    MableAstFactory.newABlockStm((statusVariable +: initializerStatements).asJava)
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
  def getInitialScalarVariables(scalars: Seq[ModelDescription.ScalarVariable]) =
  {
    scalars.filter(s =>
      s.variability != Variability.Constant
        && ((s.initial == Initial.Exact || s.initial == Initial.Approx) || (s.initial == null && s.causality == Causality.Parameter))
        && (s.`type`.`type` != Types.Enumeration))
  }

  case class EnrichedConnectionScalarVariable(connSv: ConnectionScalarVariable, fmu: FMUWithMD, instance: String, scalarVariable: ModelDescription.ScalarVariable)

  def findFMUAndSv(connectionScalarVariable: ConnectionScalarVariable, fmus: Map[FMUWithMD, Set[Instance]]) : Option[EnrichedConnectionScalarVariable] = {
    for {
      fmu <- fmus.keySet.find(x => x.key == connectionScalarVariable.vInstance.fmu)
      instances <- fmus.get(fmu)
      instance <- instances.find(x => x.name == connectionScalarVariable.vInstance.name)
      scalarVariable <- fmu.modelDescription.getScalarVariables.asScala.find(x => x.name == connectionScalarVariable.vName)
    }
    yield EnrichedConnectionScalarVariable(connectionScalarVariable, fmu, instance.name, scalarVariable )
  }

  def connSvToSv(connSv: ConnectionScalarVariable, fmus: Map[FMUWithMD, Set[Instance]]): Option[ModelDescription.ScalarVariable] = {
   fmus.keySet.find(x => x.key == connSv.vInstance.fmu).flatMap(x => x.modelDescription.getScalarVariables.asScala.find(x => x.name == connSv.vName))
  }
}

//  private def setScalar(scalarVariable: ModelDescription.ScalarVariable, fmu: String, instance : String, value: Any): Assign = {
//    val convertedValue = scalarVariable.`type` match {
//      case booleanType: ModelDescription.BooleanType =>  ("setBoolean",new BoolConst(value.asInstanceOf[Boolean]))
//      case integerType: ModelDescription.IntegerType => integerType match {
//        case realType: ModelDescription.RealType => ("setReal",new RealConst(value.asInstanceOf[Double]))
//        case x => ("setInteger",new IntConst(value.asInstanceOf[Int]))
//      }
//      case stringType: ModelDescription.StringType => ("setString",new StringConst(value.asInstanceOf[String]))
//      case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
//    }
//
//    new Assign("status",
//      new MethodCall(fmu, convertedValue._1, List(new VarRef(instance), new ArrayConst(List(new UnsignedIntConst(scalarVariable.valueReference))), new UnsignedIntConst(1), new ArrayConst(List(convertedValue._2)))))
//  }
//
//  private def mdSVTypeToArrayASTType(scalarVariable: ModelDescription.ScalarVariable): Type = {
//    val basicType = scalarVariable.`type` match {
//      case booleanType: ModelDescription.BooleanType =>  BooleanType()
//      case integerType: ModelDescription.IntegerType => integerType match {
//        case realType: ModelDescription.RealType => RealType()
//        case x => IntType()
//      }
//      case stringType: ModelDescription.StringType => StringType()
//      case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
//    }
//
//    ArrayType(basicType)
//  }
//
//  private def getScalar(scalarVariable: ModelDescription.ScalarVariable, variableName: String) = {
//    //Create variable to store output
//    new VarDeclaration(variableName, mdSVTypeToArrayASTType(scalarVariable), None )
//  }

//  def createMaBLSpec(fmus: Map[FMUWithMD, Set[Instance]], topSortedSvs: List[ConnectionScalarVariable], mmc: MultiModelConfiguration, extConnections: Set[Connection]): MaBLProgram = {
//
//    val statementsTillTopSortStatements: List[Statement] =
//      fmus.foldLeft(List(): List[Statement]) { case (acc, (fmumd, insts)) => {
//        val fmuLoad = List(new VarDeclaration(fmumd.keyNoBrackets, ModuleType("FMI2"), Some(new Load("FMI2", new URI(mmc.fmus.getOrElse(fmumd
//          .key, "FIXME"))))))
//
//        val allScalars = fmumd.modelDescription.getScalarVariables.asScala.toSeq;
//        val iniScalars = allScalars.filter(s =>
//          s.variability != Variability.Constant
//            && ((s.initial == Initial.Exact || s.initial == Initial.Approx) || (s.initial == null && s.causality == Causality.Parameter))
//            && (s.`type`.`type` != Types.Enumeration))
//
//
//        val independantScalars = allScalars.diff(iniScalars).filter(s =>
//          (s.causality == Causality.Output && (s.initial != Initial.Calculated))
//            || (s.causality == Causality.Parameter && s.initial != Initial.Calculated))
//
//        val instanceRelatedStms = insts.toList.map { x =>
//          val setupExp = List(
//            new VarDeclaration(x.name, ExternalType("FMI2Component"),
//              Some(new MethodCall(fmumd.keyNoBrackets, "instantiate", List(new StringConst(x.name), new BoolConst(true))))),
//
//            new Assign("status", new MethodCall(fmumd.keyNoBrackets, "setupExperiment", List(new VarRef(x.name), new BoolConst(false), new RealConst(0.0), new RealConst(0.0), new BoolConst(true), new RealConst(10.0)))))
//
//          val setIniScalarsStms = iniScalars.map(s  => setScalar(s, fmumd.keyNoBrackets, x.name, s.`type`.start))
//
//          val enterInitializationMode = List(new Assign("status",
//            new MethodCall(fmumd.keyNoBrackets, "enterInitializationMode", List(new VarRef(x.name)))))
//
//          //Set all independent scalars
//          val setIndependantScalars = independantScalars.map(s => {
//            val valueToSet = {
//              val defaultValue = s.`type`.start
//              val bla : Option[Any] = if (s.causality == Causality.Parameter){
//                mmc.parameters.get("{%s}.%s.%s".format(fmumd.keyNoBrackets, x.name, s.name)).map { case BooleanVal(v) => v
//                case RealVal(v) => v
//                case StringVal(v) => v
//                case IntegerVal(v) => v
//                }
//              }
//              else None
//              bla match {
//                case Some(value) => value
//                case None => s.`type`.start
//              }
//            }
//            s.`type` match {
//            case booleanType: ModelDescription.BooleanType => new Assign("status",
//              new MethodCall(fmumd.keyNoBrackets, "setBoolean", List(new VarRef(x.name), new ArrayConst(List(new UnsignedIntConst(s.valueReference))), new UnsignedIntConst(1), new ArrayConst(List(new BoolConst(valueToSet.asInstanceOf[Boolean]))))))
//            case integerType: ModelDescription.IntegerType =>
//              integerType match {
//                case realType: ModelDescription.RealType => new Assign("status",
//                  new MethodCall(fmumd.keyNoBrackets, "setReal", List(new VarRef(x.name), new ArrayConst(List(new UnsignedIntConst(s.valueReference))), new UnsignedIntConst(1), new ArrayConst(List(new RealConst(valueToSet.asInstanceOf[Double]))))))
//                case _ => new Assign("status",
//                  new MethodCall(fmumd.keyNoBrackets, "SetInteger", List(new VarRef(x.name), new ArrayConst(List(new UnsignedIntConst(s.valueReference))), new UnsignedIntConst(1), new ArrayConst(List(new IntConst(valueToSet.asInstanceOf[Integer]))))))
//              }
//            case stringType: ModelDescription.StringType => new Assign("status",
//              new MethodCall(fmumd.keyNoBrackets, "SetString", List(new VarRef(x.name), new ArrayConst(List(new UnsignedIntConst(s.valueReference))), new UnsignedIntConst(1), new ArrayConst(List(new StringConst(valueToSet.asInstanceOf[String]))))))
//          }})
//
//
//
//
//          val result = setupExp.appendedAll(setIniScalarsStms).appendedAll(enterInitializationMode).appendedAll(setIndependantScalars)
//          result
//        }
//
//        acc.appendedAll(fmuLoad.appendedAll(instanceRelatedStms.flatten))
//      }
//      }
//
//
//
//    // Set/Get all related to topological sort
//   val getsAndSets: Seq[VarDeclaration] =  topSortedSvs.flatMap(connectionSV => {
//      fmus.keys.find(fmu => fmu.key == connectionSV.vInstance.fmu).flatMap(fmu => {
//        fmu.modelDescription.getScalarVariables.asScala.toSeq.find(x => x.name == connectionSV.vName).map(sv => {
//          getScalar(sv, fmu.keyNoBrackets + connectionSV.vName + connectionSV.vInstance.name)
//        })
//        })
//    })
//
//
//    val program = new MaBLProgram(new Block(List(FMI2ModuleLibrary.getFMI2Library()).appendedAll(statementsTillTopSortStatements).appendedAll(getsAndSets)))
//    program
//  }

//}
