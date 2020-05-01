package org.intocps.initializer

import java.net.URI
import java.util

import org.antlr.v4.runtime.TokenFactory
import org.intocps.fmi.IFmiComponent
import org.intocps.initializer.SetOrGet.SetOrGet
import org.intocps.maestro.ast.{AArrayInitializer, AAssigmentStm, ABoolLiteralExp, ACallExp, ADotExp, AExpInitializer, AFunctionDeclaration, AIdentifierExp, ALoadExp, ALocalVariableStm, AStringLiteralExp, AUIntLiteralExp, AVariableDeclaration, LexIdentifier, MableAstFactory, PExp, PInitializer, PStm, PType, SLiteralExpBase}
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



  implicit def singleValueToArray(AUIntLiteralExp: AUIntLiteralExp) : AArrayInitializer = MableAstFactory.newAArrayInitializer(Vector(AUIntLiteralExp).asJava)
  implicit def str2LexIdentifier(x: String) = new LexIdentifier(x, null)
  implicit def exp2ExpInitializer(x: PExp) : AExpInitializer = MableAstFactory.newAExpInitializer(x)
  implicit def str2IdentifierExp(x: String): AIdentifierExp = MableAstFactory.newAIdentifierExp(x)
  implicit def lexIdentifier2IdentifierExp(x:LexIdentifier) : AIdentifierExp = x.getText
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

  def setScalarVariable(fmiComponent: String, scalarVariable: ModelDescription.ScalarVariable, value: Any, identifier: AIdentifierExp) = {
    val functionIdentifier = findGetSetFMIFunction(fmiComponent, scalarVariable, SetOrGet.Set)
    val sizeArgument : AUIntLiteralExp = (1 : Long)
    // TODO: Fix this with correct arrayExp
    val valueReferenceArgument : AArrayInitializer  = long2AUIntLiteralExp(scalarVariable.valueReference)
    // TODO: Fix this with correct arrayExp
    val valueArgument : SLiteralExpBase = getValueArgument(scalarVariable, value)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, Vector(valueReferenceArgument, sizeArgument, valueArgument).asJava)
    MableAstFactory.newAAssignmentStm(identifier, callExp)
  }

  def setupExperiment(name: String, statusVariableAsExp: AIdentifierExp): AAssigmentStm = {
    val functionIdentifier = findFmiFunction(name, fmi2SetupExperimentFunctionName)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, null)
    MableAstFactory.newAAssignmentStm(statusVariableAsExp, callExp)
  }

  def enterInitializationMode(name: String, statusVariableAsExp: AIdentifierExp): AAssigmentStm = {
    val functionIdentifier = findFmiFunction(name, fmi2EnterInitializationMode)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, null)
    MableAstFactory.newAAssignmentStm(statusVariableAsExp, callExp)
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

  def getScalarVariable(fmiComponent: String, scalarVariable: ModelDescription.ScalarVariable, identifier: AIdentifierExp) : Seq[PStm] = {
    // Create variable to contain result
    val arrayType = MableAstFactory.newAArrayType(scalarVariable.`type`, 1)
    val variableDeclStm : PStm = MableAstFactory.newAVariableDeclaration(fmiComponent+scalarVariable.name, arrayType, null)
    val functionIdentifier = findGetSetFMIFunction(fmiComponent, scalarVariable, SetOrGet.Set)
    // TODO: Fix this with correct arrayExp
    val valueReferenceArgument : AArrayInitializer  = long2AUIntLiteralExp(scalarVariable.valueReference)
    val callExp = MableAstFactory.newACallExp(functionIdentifier, Vector(Vector(scalarVariable.valueReference, ))

  }
}
