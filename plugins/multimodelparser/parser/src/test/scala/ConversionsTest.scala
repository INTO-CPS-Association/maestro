
import org.intocps.multimodelparser.data.{Connection, ConnectionScalarVariable, ConnectionType, Instance}
import org.intocps.multimodelparser.parser.Conversions
import org.scalatest.flatspec.AnyFlatSpec



class ConversionsTest extends AnyFlatSpec{
  "configVarToConnectionSV" should "Convert a connection variable to a ConnectionScalarVariable" in {
    val test = "{key}.instance.var.alsoVar"
    val expected = ConnectionScalarVariable("var.alsoVar", Instance("instance", "{key}"));
    val actual = Conversions.configVarToConnectionSV(test);

    assert(expected==actual);

  }

  "configConnectionToConnection" should "Convert a MultiModelConfiguration connection to a Connection" in {
    
    val test = ("{key}.instance.output",List("{key1}.instance1.input1", "{key2}.instance2.input2"));
    val output = ConnectionScalarVariable("output", Instance("instance", "{key}"));
    val input1 = ConnectionScalarVariable("input1", Instance("instance1", "{key1}"));
    val input2 = ConnectionScalarVariable("input2", Instance("instance2", "{key2}"));
    val expected = Connection(output,Set(input1,input2), ConnectionType.External);
    val actual = Conversions.configConnectionToConnection(test);

    assert(expected==actual);
  }

  "MMCConnectionsToMaestroConnections" should "Convert connections to Set[Connection]" in {
    val test = Map(
      "{key}.instance.output" -> List("{key1}.instance1.input1", "{key2}.instance2.input2"),
      "{keyx}.instancex.outputx" -> List("{keyx1}.instancex1.inputx1", "{keyx2}.instancex2.inputx2")
    );

    val output = ConnectionScalarVariable("output", Instance("instance", "{key}"));
    val input1 = ConnectionScalarVariable("input1", Instance("instance1", "{key1}"));
    val input2 = ConnectionScalarVariable("input2", Instance("instance2", "{key2}"));
    val connection = Connection(output,Set(input1,input2), ConnectionType.External);

    val outputx = ConnectionScalarVariable("outputx", Instance("instancex", "{keyx}"));
    val inputx1 = ConnectionScalarVariable("inputx1", Instance("instancex1", "{keyx1}"));
    val inputx2 = ConnectionScalarVariable("inputx2", Instance("instancex2", "{keyx2}"));
    val connectionx = Connection(outputx,Set(inputx1,inputx2), ConnectionType.External);

    val expected = Set(connection, connectionx);
    val actual = Conversions.MMCConnectionsToMaestroConnections(test);

    assert(expected == actual);

  }

}
