package org.intocps.initializer

import java.net.URI
import java.util

import org.antlr.v4.runtime.TokenFactory
import org.intocps.maestro.ast.{AExpInitializer, AFunctionDeclaration, AIdentifierExp, ALoadExp, ALocalVariableStm, AStringLiteralExp, AVariableDeclaration, LexIdentifier, MableAstFactory, PExp, PInitializer, PType}

import scala.jdk.CollectionConverters._
object FMIASTFactory {

  implicit def str2LexIdentifier(x: String) = new LexIdentifier(x, null)
  implicit def exp2ExpInitializer(x: PExp) : AExpInitializer = MableAstFactory.newAExpInitializer(x)
  implicit def str2IdentifierExp(x: String): AIdentifierExp = MableAstFactory.newAIdentifierExp(x)
  implicit def lexIdentifier2IdentifierExp(x:LexIdentifier) : AIdentifierExp = x.getText
  implicit def scalaList2JavaList[A](x: List[A]): util.List[A] = x.asJava
  implicit def variableDeclaration2Statement(x: AVariableDeclaration): ALocalVariableStm = MableAstFactory.newALocalVariableStm(x)

  def FMIVersionStr = "FMI2"
  def FMIComponentStr = FMIVersionStr + "Component"
  def FMIInstantiateFunctionName = "fmi2Instantiate";
  def FMUType() = MableAstFactory.newANameType(FMIVersionStr);

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


}
