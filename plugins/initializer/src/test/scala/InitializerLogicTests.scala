import java.io.File
import java.net.URI

import org.apache.commons.io.IOUtils
import org.intocps.initializer.FMIASTFactory
import org.intocps.maestro.ast.PStm
import org.scalatest.flatspec.AnyFlatSpec
import scala.jdk.CollectionConverters._


/*
Tests related to the interface of the plugin
 */
class InitializerLogicSpec extends AnyFlatSpec {

  "the initializer object" should "create a spec that matches expected" in {
    val spec = Initializer.calculateInitialize(Resources.fileSinglewatertank20sim);
    spec match {
      case Left(value: String) => assert(false, "Failed to calculateInitialize: " + value)
      case Right(value: PStm) => assertResult("{\nFMI2 tank = load(\"src/test/resources/single-watertank/singlewatertank-20sim.fmu\");\nFMI2Component t = tank.fmi2Instantiate(\"t\");;\nFMI2 control = load(\"src/test/resources/single-watertank/watertankcontroller-c.fmu\");\nFMI2Component c = control.fmi2Instantiate(\"c\");;\n}")(value.toString)
    }
  }
}