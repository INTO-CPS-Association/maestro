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
class InitializerPluginInterfaceSpec extends AnyFlatSpec {

  def createJsonContext(f: File) = {
    val uri = f.getAbsoluteFile.toURI
    val context = "{\"multimodelfilepath\": \"%s\"}".format(uri.toString)
    context
  }

  def parseJsonContext(ctxt: String) = {
    val initializer = new Initializer()
    initializer.parseConfig(IOUtils.toInputStream(ctxt, "UTF-8"))
  }

  def createValidContext(f: File) = parseJsonContext(createJsonContext(f))

  // Test is successful if no exception is thrown.
  "The initializer plugin " should "parse a context" in {
    println("####################")
    println((new File(".")).getAbsolutePath)

    val context = createJsonContext(Resources.fileSinglewatertank20sim)
    println("Trying context: " + context)

    val initializer = new Initializer()
    initializer.parseConfig(IOUtils.toInputStream(context, "UTF-8"))

  }

  it should "fail to unfold non-reference-equal function" in {
    val initializer = new Initializer()
    assertThrows[AssertionError](initializer.unfold(FMIASTFactory.functionDeclaration("initialize"), null, createValidContext(Resources.fileSinglewatertank20sim)))
  }

   it should "unfold reference-equal function" in {
    val initializer = new Initializer()
    // functionDeclSet.iterator().next() works because it only exports 1 function
    val functionDeclSet = initializer.getDeclaredUnfoldFunctions.asScala;
    //val initializeFunction = functionDeclSet.iterator().next()
    initializer.unfold(functionDeclSet.head, null, createValidContext(Resources.fileSinglewatertank20sim))
  }
}

