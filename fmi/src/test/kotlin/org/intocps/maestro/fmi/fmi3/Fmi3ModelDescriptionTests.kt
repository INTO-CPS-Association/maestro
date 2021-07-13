package org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription
import org.junit.jupiter.api.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import javax.xml.transform.stream.StreamSource
import kotlin.Exception

class Fmi3ModelDescriptionTests {

    private val resourcePath = Paths.get("src", "test", "resources")

    companion object {
        @JvmStatic
        fun modelDescriptionProvider(): Stream<Arguments> {
            //TODO: reference ClocksMD.xml does not conform to xsd so it is filtered until fixed
            return Arrays.stream(
                Objects.requireNonNull(
                    Paths.get("src", "test", "resources", "Fmi3ReferenceFmuModelDescriptions").toFile().listFiles()
                )
            ).filter { f -> f.name != "ClocksMD.xml" }.map { f -> Arguments.arguments(f.name, f) }
        }
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("modelDescriptionProvider")
    fun validateVALIDModelDescriptionTest(name: String, modelDescription: File) {
        assertDoesNotThrow("All reference model descriptions should be able to be validated") {
            val mdAsStream = modelDescription.inputStream()
            val mdXsdAsStream = Paths.get(resourcePath.toString(), "fmi3ModelDescription.xsd").toFile().inputStream()

            ModelDescription.validateAgainstXSD(StreamSource(mdAsStream), StreamSource(mdXsdAsStream))
        }
    }

    @Test
    fun validateUNVALIDModelDescriptionTest() {
        assertThrows<Exception>("Model description with error should not be able to be validated") {
            val mdAsStream =
                Paths.get(resourcePath.toString(), "Fmi3ModelDescriptionWithError", "InvalidMD.xml").toFile()
                    .inputStream()
            val mdXsdAsStream = Paths.get(resourcePath.toString(), "fmi3ModelDescription.xsd").toFile().inputStream()

            ModelDescription.validateAgainstXSD(StreamSource(mdAsStream), StreamSource(mdXsdAsStream))
        }
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("modelDescriptionProvider")
    fun parseModelDescriptionTest(name: String, modelDescription: File) {
        assertDoesNotThrow{
            val mdAsStream = ByteArrayInputStream(modelDescription.readBytes())
            Fmi3ModelDescription(mdAsStream)
        }
    }

    @Test
    fun getScalarVariablesFromModelDescriptionTest() {
        // ARRANGE
        val expectedListLength = 7
        val mdAsStream = ByteArrayInputStream(Paths.get(resourcePath.toString(),"Fmi3ReferenceFmuModelDescriptions", "BouncingBallMD.xml").toFile().readBytes())
        val modelDescription = Fmi3ModelDescription(mdAsStream)

        // ACT
        val scalarVariables = modelDescription.getScalarVariables()

        // ASSERT
        Assertions.assertEquals(expectedListLength, scalarVariables.size)
    }

    @Test
    fun getUnitDefinitionsTest() {
        // ARRANGE
        val expectedListLength = 3
        val mdAsStream = ByteArrayInputStream(Paths.get(resourcePath.toString(),"Fmi3ReferenceFmuModelDescriptions", "BouncingBallMD.xml").toFile().readBytes())
        val modelDescription = Fmi3ModelDescription(mdAsStream)

        // ACT
        val unitDefinitions = modelDescription.getUnitDefinitions()

        // ASSERT
        Assertions.assertEquals(expectedListLength, unitDefinitions.size)
    }
}