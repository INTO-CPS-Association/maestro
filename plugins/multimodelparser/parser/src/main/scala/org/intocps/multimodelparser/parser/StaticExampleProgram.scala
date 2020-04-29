package org.intocps.multimodelparser.parser

class StaticExampleProgram {

  def constructExampleAST() = {
//    new MaBLProgram(new Block(List(
//      FMI2ModuleLibrary.getFMI2Library(),
//      new VarDeclaration("FMU", ModuleType("FMI2"), Some(new Load("FMI2", new URI("path/to/FMU.fmu")))),
//      new VarDeclaration("f", ExternalType("FMI2Component"),
//        Some(new MethodCall("FMU", "instantiate", List(new StringConst("f"), new BoolConst(true))))),
//      new VarDeclaration("status", IntType(), None),
//      new Assign("status",
//        new MethodCall("FMU", "setupExperiment", List(new VarRef("f"), new BoolConst(false), new RealConst(0.0), new RealConst(0.0), new BoolConst(true), new RealConst(10.0)))),
//      new Assign("status",
//        new MethodCall("FMU", "enterInitializationMode", List(new VarRef("f")))),
//      new Assign("status",
//        new MethodCall("FMU", "exitInitializationMode", List(new VarRef("f")))),
//      new VarDeclaration("y", ArrayType(RealType()), Some(new ArrayConst(List(new RealConst(0.0))))),
//      new VarDeclaration("yref", ArrayType(UnsignedIntType()), Some(new ArrayConst(List(new UnsignedIntConst(1))))),
//      new VarDeclaration("uref", ArrayType(UnsignedIntType()), Some(new ArrayConst(List(new UnsignedIntConst(2))))),
//      new VarDeclaration("time", RealType(), Some(new RealConst(0.0))),
//      new While(new LessThan(new VarRef("time"), new RealConst(10.0)), new Block(List(
//        new Assign("status",
//          new MethodCall("FMU", "doStep", List(new VarRef("f"), new VarRef("time"), new RealConst(0.01)))),
//        new Assign("status",
//          new MethodCall("FMU", "getReal", List(new VarRef("f"), new VarRef("yref"), new UnsignedIntConst(1), new VarRef("y")))),
//        new Assign("status",
//          new MethodCall("FMU", "setReal", List(new VarRef("f"), new VarRef("uref"), new UnsignedIntConst(1), new VarRef("y")))),
//        new Assign("time", new Add(new VarRef("time"), new RealConst(0.01))),
//      ))),
//      new Assign("status",
//        new MethodCall("FMU", "terminate", List(new VarRef("f")))),
//      new MethodCall("FMU", "freeInstance", List(new VarRef("f"))),
//      new MethodCall(null, "multimodelAdpation", List.empty)
//    )))
  }


}
