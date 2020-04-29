import java.io.File

import org.intocps.multimodelparser.data.IntegerVal
import org.intocps.multimodelparser.parser.{ConfigurationHandler, MultiModelConfiguration}
import org.scalatest.flatspec.AnyFlatSpec

import scala.jdk.CollectionConverters._
class ConfigurationHandlerTest extends AnyFlatSpec {
"loadMMCFromFile" should "Load a json file into a case class" in {
  val f = new File("src/test/resources/single-watertank/single-watertank.json");

  val expected = MultiModelConfiguration(
    Map(
      "{control}" -> "src/test/resources/single-watertank/watertankcontroller-c.fmu",
      "{tank}" -> "src/test/resources/single-watertank/singlewatertank-20sim.fmu"),

    Map(
      "{control}.c.valve" -> List("{tank}.t.valvecontrol"),
      "{tank}.t.level" -> List("{control}.c.level")),

    Map("{control}.c.maxlevel" -> IntegerVal(2),
      "{control}.c.minlevel" -> IntegerVal(1)))

  val actual = ConfigurationHandler.loadMMCFromFile(f);
  actual match {
    case Left(value) => assert(false, "Could not loadMMCFromFile")
    case Right(actualValue) =>
      // Check that MMs has actually been parsed by checking that it has scalar variables
      assert(actualValue.fmus.keySet.map(x => x.modelDescription.getScalarVariables.asScala.size > 0 ).contains(false) == false)
      assert(expected == actualValue.multiModelConfiguration)
  }
}

}
