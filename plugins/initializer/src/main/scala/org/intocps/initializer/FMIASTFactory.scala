package org.intocps.initializer

import java.net.URI
import java.util

import org.antlr.v4.runtime.TokenFactory
import org.intocps.fmi.IFmiComponent
import org.intocps.initializer.FMIASTFactory.ValueArrayVariables
import org.intocps.initializer.SetOrGet.SetOrGet
import org.intocps.maestro.ast.{AArrayInitializer, AArrayType, AAssigmentStm, ABoolLiteralExp, ACallExp, ADotExp, AExpInitializer, AFunctionDeclaration, AIdentifierExp, AIdentifierStateDesignator, ALoadExp, ALocalVariableStm, AStringLiteralExp, AUIntLiteralExp, AVariableDeclaration, LexIdentifier, MableAstFactory, Node, PExp, PInitializer, PStm, PType, SLiteralExpBase}
import org.intocps.orchestration.coe.modeldefinition.ModelDescription
import org.intocps.orchestration.coe.modeldefinition.ModelDescription.ScalarVariable

import scala.jdk.CollectionConverters._

// Define a new enumeration with a type alias and work with the full set of enumerated values
object SetOrGet extends Enumeration {
  type SetOrGet = Value
  val Set,Get = Value

  override def toString(): String = Value match {
    case Set => "set"
    case Get =>" get"
  }
}

object FMIASTFactory {


  def fmi2FunctionPrefix = "fmi2"
  def FMIVersionStr = "FMI2"
  def FMIComponentStr = FMIVersionStr + "Component"
  def FMIInstantiateFunctionName = "fmi2Instantiate";
  def fmi2SetupExperimentFunctionName = "SetupExperiment"
  def fmi2EnterInitializationMode = "EnterInitializationMode"


  implicit def CONVAIdentifierStateDesignator2LexIdentifier(aIdentifierStateDesignator: AIdentifierStateDesignator) : LexIdentifier = aIdentifierStateDesignator.getName
  def CONVAIdentifierStateDesignator2AIdentifierExp(aIdentifierStateDesignator: AIdentifierStateDesignator) : AIdentifierExp = MableAstFactory.newAIdentifierExp(aIdentifierStateDesignator.getName)
  implicit def CONVSingleValueToArray(AUIntLiteralExp: AUIntLiteralExp) : AArrayInitializer = MableAstFactory.newAArrayInitializer(Vector(AUIntLiteralExp).asJava)
  def CONVALocalVariableStm2AIdentifierStateDesignator(stm : ALocalVariableStm) : AIdentifierStateDesignator = MableAstFactory.newAIdentifierStateDesignator(stm.getName)
  implicit def CONVLexIdentifier2AIdentifierStateDesignator(id : LexIdentifier) : AIdentifierStateDesignator = MableAstFactory.newAIdentifierStateDesignator(id)
  implicit def str2LexIdentifier(x: String) = new LexIdentifier(x, null)
  implicit def exp2ExpInitializer(x: PExp) : AExpInitializer = MableAstFactory.newAExpInitializer(x)
  implicit def str2IdentifierExp(x: String): AIdentifierExp = MableAstFactory.newAIdentifierExp(x)
  implicit def lexIdentifier2IdentifierExp(x:LexIdentifier) : AIdentifierExp = MableAstFactory.newAIdentifierExp(x)
  implicit def scalaList2JavaList[A](x: List[A]): util.List[A] = x.asJava
  implicit def variableDeclaration2Statement(x: AVariableDeclaration): ALocalVariableStm = MableAstFactory.newALocalVariableStm(x)
  implicit def aLocalVariableStmToAIdentifierExp(x: ALocalVariableStm) : AIdentifierExp = x.getDeclaration.getName
  implicit def long2AUIntLiteralExp(x: Long) : AUIntLiteralExp = MableAstFactory.newAUIntLiteralExp(x)
  implicit def svType2FmiType(x: ModelDescription.Type ) : PType =     x match {
    case booleanType: ModelDescription.BooleanType =>  MableAstFactory.newABoleanPrimitiveType()
    case integerType: ModelDescription.IntegerType => integerType match {
      case realType: ModelDescription.RealType => MableAstFactory.newARealNumericPrimitiveType()
      case x => MableAstFactory.newAIntNumericPrimitiveType()
    }
    case stringType: ModelDescription.StringType => MableAstFactory.newAStringPrimitiveType()
    case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
  }

  def createVariable(name: String, varType: PType, initializer: Option[PInitializer]=None): ALocalVariableStm = {
    MableAstFactory.newALocalVariableStm(MableAstFactory.newAVariableDeclaration(name, varType, initializer.getOrElse(null)))
  }


  def FMUType() = MableAstFactory.newANameType(FMIVersionStr);

  def createFmi2StatusVariable(str: String): ALocalVariableStm = {
    val statusType = MableAstFactory.newABoleanPrimitiveType()
    val exp = MableAstFactory.newABoolLiteralExp(false)
    val varDecl = MableAstFactory.newAVariableDeclaration(str, statusType, exp)
    MableAstFactory.newALocalVariableStm(varDecl)
  }


  def findFmiFunction(parent: String, functionWithoutPrefix: String ) : ADotExp = {
   MableAstFactory.newADotExp(parent, fmi2FunctionPrefix + functionWithoutPrefix)
  }

  def findGetSetFMIFunction(parent: String, scalarVariable: ModelDescription.ScalarVariable,setOrGet: SetOrGet) : ADotExp = {
    val typeName = getFMIType(scalarVariable)
    val preTypeName = setOrGet.toString
    findFmiFunction(parent, preTypeName + typeName)
  }

  def findGetSetFMIFunction(parent: String, scalarVariableType: ModelDescription.Type,setOrGet: SetOrGet) : ADotExp = {
    val typeName = getFMIType(scalarVariableType)
    val preTypeName = setOrGet.toString
    findFmiFunction(parent, preTypeName + typeName)
  }

  def getFMIType(scalarVariableType: ModelDescription.Type): String = {
    scalarVariableType match {
      case booleanType: ModelDescription.BooleanType =>  "Boolean"
      case integerType: ModelDescription.IntegerType => integerType match {
        case realType: ModelDescription.RealType => "Real"
        case x => "Integer"
      }
      case stringType: ModelDescription.StringType => ("String")
      case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
    }
  }

  def getFMIType(scalarVariable: ModelDescription.ScalarVariable): String = {
    scalarVariable.`type` match {
      case booleanType: ModelDescription.BooleanType =>  "Boolean"
      case integerType: ModelDescription.IntegerType => integerType match {
        case realType: ModelDescription.RealType => "Real"
        case x => "Integer"
      }
      case stringType: ModelDescription.StringType => ("String")
      case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
    }
  }

  def getValueArgument(scalarVariable: ModelDescription.ScalarVariable, value: Any): SLiteralExpBase = {
    scalarVariable.`type` match {
      case booleanType: ModelDescription.BooleanType =>   MableAstFactory.newABoolLiteralExp(value.asInstanceOf[Boolean])
      case integerType: ModelDescription.IntegerType => integerType match {
        case realType: ModelDescription.RealType => MableAstFactory.newARealLiteralExp(value.asInstanceOf[Double])
        case x => MableAstFactory.newAIntLiteralExp(value.asInstanceOf[Int])
      }
      case stringType: ModelDescription.StringType => MableAstFactory.newAStringLiteralExp(value.asInstanceOf[String])
      case unknown => throw new Exception("Unknown scalar: " + unknown.toString)
    }
  }



  def setupExperiment(name: String, statusVariable: AIdentifierStateDesignator): AAssigmentStm = {
    val functionIdentifier = findFmiFunction(name, fmi2SetupExperimentFunctionName)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, null)
    MableAstFactory.newAAssignmentStm(statusVariable, callExp)
  }

  def enterInitializationMode(name: String, statusVariable: AIdentifierStateDesignator): AAssigmentStm = {
    val functionIdentifier = findFmiFunction(name, fmi2EnterInitializationMode)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, null)
    MableAstFactory.newAAssignmentStm(statusVariable, callExp)
  }

  def LoadFMU(name: String, uri: URI) : AVariableDeclaration = {
    MableAstFactory.newAVariableDeclaration(name, FMUType(), MableAstFactory.newALoadExp(uri))
  }

  def FMUComponentType(): PType = {
    MableAstFactory.newANameType(FMIVersionStr+"Component");
  }

  def instantiate(fmu: AVariableDeclaration, instanceName: String) : AVariableDeclaration = {
    val initializerArgs: List[AStringLiteralExp] = List(MableAstFactory.newAStringLiteralExp(instanceName))
    val functionIdentifier = MableAstFactory.newADotExp(fmu.getName, FMIInstantiateFunctionName)
    val initializer : PInitializer = MableAstFactory.newACallExp(functionIdentifier, initializerArgs);

    MableAstFactory.newAVariableDeclaration(instanceName, FMUComponentType(), initializer);
  }

  def functionDeclaration(name: String) : AFunctionDeclaration = MableAstFactory.newAFunctionDeclaration(name)



  def setScalarVariable(fmiComponent: String,
                        scalarVariable: ModelDescription.ScalarVariable,
                        value: ValuesToSet,
                        valueReferenceArrayVariable : AIdentifierStateDesignator,
                        valueArrayVariables: ValueArrayVariables,
                        statusVariable: AIdentifierStateDesignator) = {

case class ValueStatementAndArgument(stm: Option[PStm], value: PExp)
    // Update the valueReferenceArrayVariable  with the correct valueReference and create the relevant arguments
    val stmValueReferenceAssignment = MableAstFactory.newAAssignmentStm(MableAstFactory.newAArayStateDesignator(valueReferenceArrayVariable, 0L),scalarVariable.valueReference)
    val valueReferenceArgument : AIdentifierExp = CONVAIdentifierStateDesignator2LexIdentifier(valueReferenceArrayVariable)
    val sizeArgument : AUIntLiteralExp = (1 : Long)

    // Update the valueArrayVariable with the correct value and create the relevant arguments IF not already carried out!
    val valueIdentifier = valueArrayVariables.getCorrectIdentifier(scalarVariable.`type`)
    val stmValueAssignment = value match {
      case a@rawSingleValue(value, valueType) =>
        val stmValueAssignment = MableAstFactory.newAAssignmentStm(MableAstFactory.newAArayStateDesignator(valueIdentifier,0L), a.getExp())
        Some(stmValueAssignment)
      case ArrayVariable(variables, valueType) => None
    }

    val valueArgument : AIdentifierExp = CONVAIdentifierStateDesignator2LexIdentifier(valueIdentifier)

    // Create the arguments to the call
    val callArguments = Vector(valueReferenceArgument, sizeArgument, valueArgument).asJava

    // Find the function to call
    val functionIdentifier = findGetSetFMIFunction(fmiComponent, scalarVariable, SetOrGet.Set)

    // Call the function
    val callExp = MableAstFactory.newACallExp(functionIdentifier, callArguments)

    // Assign to status variable
    val stmFmiCallAssignment = MableAstFactory.newAAssignmentStm(statusVariable, callExp)

    val statements = (stmValueAssignment match {
      case None => Vector(stmValueReferenceAssignment)
      case Some(value) => Vector(stmValueReferenceAssignment) :+ value
    }) :+ stmFmiCallAssignment
    statements
  }

  private def getSetScalarVariable(fmiComponent: String, valueReference: String, size: Long, valueVariable: String, statusVariable: String, scalarVariableType: ModelDescription.Type, setOrGet: SetOrGet) : PStm = {
    val functionIdentifier = findGetSetFMIFunction(fmiComponent, scalarVariableType, setOrGet)
    val valueReferenceArgument = MableAstFactory.newAIdentifierExp(valueReference)
    val sizeArgument = MableAstFactory.newAUIntLiteralExp(size)
    val valueArgument = MableAstFactory.newAIdentifierExp(valueVariable)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, Vector(valueReferenceArgument, sizeArgument, valueArgument).asJava)
    MableAstFactory.newAAssignmentStm(MableAstFactory.newAIdentifierStateDesignator(statusVariable), callExp)
  }

  def getScalarVariable(fmiComponent: String, valueReference: String, size: Long, valueVariable: String, statusVariable: String, scalarVariableType: ModelDescription.Type): PStm =
  {
    return getSetScalarVariable(fmiComponent, valueReference, size, valueVariable, statusVariable, scalarVariableType, SetOrGet.Get)
  }

  def setScalarVariable(fmiComponent: String, valueReference: String, size: Long, valueVariable: String, statusVariable: String, scalarVariableType: ModelDescription.Type): PStm = {
    return getSetScalarVariable(fmiComponent, valueReference, size, valueVariable, statusVariable, scalarVariableType, SetOrGet.Set)
  }

  def getScalarVariable(fmiComponent: String,
                        scalarVariable: ModelDescription.ScalarVariable,
                        valueReferenceArrayVariable : AIdentifierStateDesignator,
                        valueArrayVariables: ValueArrayVariables,
                        statusVariable: AIdentifierStateDesignator) : Seq[PStm] = {
    // Update the valueReferenceArrayVariable  with the correct valueReference and create the relevant arguments
    val stmValueReferenceAssignment = MableAstFactory.newAAssignmentStm(MableAstFactory.newAArayStateDesignator(valueReferenceArrayVariable, 0L),scalarVariable.valueReference)
    val valueReferenceArgument : AIdentifierExp = CONVAIdentifierStateDesignator2LexIdentifier(valueReferenceArrayVariable)
    val sizeArgument : AUIntLiteralExp = (1 : Long)

    // Find the correct valueArrayVariable to store the result in
    val valueArgument : AIdentifierExp = CONVAIdentifierStateDesignator2LexIdentifier(valueArrayVariables.getCorrectIdentifier(scalarVariable.`type`))

    // Create the arguments to the call
    val callArguments= Vector(valueReferenceArgument, sizeArgument, valueArgument).asJava

    // Find the function to call
    val functionIdentifier = findGetSetFMIFunction(fmiComponent, scalarVariable, SetOrGet.Get)

    // Create the Call expression
    val callExp = MableAstFactory.newACallExp(functionIdentifier, callArguments)

    // Assign to status variable
    val stmFmiCallAssignment = MableAstFactory.newAAssignmentStm(statusVariable, callExp)

    Vector(stmValueReferenceAssignment, stmFmiCallAssignment)
  }

  case class ValueArrayVariables(real: AIdentifierStateDesignator, int: AIdentifierStateDesignator, string: AIdentifierStateDesignator, bool : AIdentifierStateDesignator) {
    def getCorrectIdentifier(fmiType: ModelDescription.Type) = fmiType match {
      case booleanType: ModelDescription.BooleanType => bool
      case integerType: ModelDescription.IntegerType => integerType match {
        case enumerationType: ModelDescription.EnumerationType => throw new Exception("Enum type not supported.")
        case realType: ModelDescription.RealType => real
        case _ => int
      }
      case stringType: ModelDescription.StringType => string
      case _ => throw new Exception("Unknown type not supported.")
    }
  }


}

sealed trait ValuesToSet {
  def getExp(): PExp
}

case class rawSingleValue(value: Any, valueType: ModelDescription.Type )  extends ValuesToSet {
  def computeASTType = {
    valueType match {
      case booleanType: ModelDescription.BooleanType =>  MableAstFactory.newABoleanPrimitiveType()
      case integerType: ModelDescription.IntegerType => integerType match {
        case realType: ModelDescription.RealType => MableAstFactory.newARealNumericPrimitiveType()
        case x => MableAstFactory.newAIntNumericPrimitiveType()
      }
      case stringType: ModelDescription.StringType => MableAstFactory.newAStringPrimitiveType()
      case unknown => throw new Exception("Unknown type for variable: " + valueType)
  }}
    def getExp = {
      valueType match {
        case booleanType: ModelDescription.BooleanType =>   MableAstFactory.newABoolLiteralExp(value.asInstanceOf[Boolean])
        case integerType: ModelDescription.IntegerType => integerType match {
          case realType: ModelDescription.RealType => MableAstFactory.newARealLiteralExp(value.asInstanceOf[Double])
          case x => MableAstFactory.newAIntLiteralExp(value.asInstanceOf[Int])
        }
        case stringType: ModelDescription.StringType => MableAstFactory.newAStringLiteralExp(value.asInstanceOf[String])
        case unknown => throw new Exception("Unknown type for variable: " + valueType)
      }
  }
}

case class ArrayVariable(variables : ValueArrayVariables, valueType: ModelDescription.Type) extends ValuesToSet {
  def getExp() : PExp =
  {
    FMIASTFactory.CONVAIdentifierStateDesignator2AIdentifierExp(variables.getCorrectIdentifier(valueType))
  }
}

