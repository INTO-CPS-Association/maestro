package org.intocps.maestro.fmi.fmi3

import org.intocps.maestro.fmi.ModelDescription
import org.intocps.maestro.fmi.org.intocps.maestro.fmi.fmi3.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import javax.xml.transform.stream.StreamSource

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
    @Order(0)
    fun validateVALIDModelDescriptionTest(name: String, modelDescription: File) {
        assertDoesNotThrow("All reference model descriptions should be able to be validated") {
            val mdAsStream = modelDescription.inputStream()
            val mdXsdAsStream = Paths.get(resourcePath.toString(), "fmi3ModelDescription.xsd").toFile().inputStream()

            ModelDescription.validateAgainstXSD(StreamSource(mdAsStream), StreamSource(mdXsdAsStream))
        }
    }

    @Test
    @Order(1)
    fun validateINVALIDModelDescriptionTest() {
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
    fun getScalarVariablesFromModelDescriptionTest(name: String, modelDescription: File) {
        // ARRANGE
        val mdAsStream = ByteArrayInputStream(modelDescription.readBytes())

        // ACT
        val scalarVariables = Fmi3ModelDescription(mdAsStream).getScalarVariables()

        // ASSERT
        Assertions.assertTrue(scalarVariables.isNotEmpty())
    }

    @Test
    fun getUnitDefinitionsTest() {
        // ARRANGE
        val expectedListLength = 3
        val mdAsStream = ByteArrayInputStream(
            Paths.get(
                resourcePath.toString(),
                "Fmi3ReferenceFmuModelDescriptions",
                "BouncingBallMD.xml"
            ).toFile().readBytes()
        )
        val modelDescription = Fmi3ModelDescription(mdAsStream)

        // ACT
        val unitDefinitions = modelDescription.getUnitDefinitions()

        // ASSERT
        Assertions.assertEquals(expectedListLength, unitDefinitions.size)
    }

    @Test
    fun getLogCategoriesTest() {
        // ARRANGE
        val mdAsStream = ByteArrayInputStream(
            Paths.get(
                resourcePath.toString(),
                "Fmi3ReferenceFmuModelDescriptions",
                "BouncingBallMD.xml"
            ).toFile().readBytes()
        )

        // ACT
        val logCategories = Fmi3ModelDescription(mdAsStream).getLogCategories()

        // ASSERT
        Assertions.assertTrue(logCategories.isNotEmpty())
    }

    @ParameterizedTest(name = "{index} \"{0}\"")
    @MethodSource("modelDescriptionProvider")
    fun getDefaultExperimentTest(name: String, modelDescription: File) {
        // ARRANGE
        val mdAsStream = ByteArrayInputStream(modelDescription.readBytes())

        // ACT
        val defaultExperiment = Fmi3ModelDescription(mdAsStream).getDefaultExperiment()

        // ASSERT
        Assertions.assertTrue(defaultExperiment != null)
    }

    @Test
    fun getModelIdentifierTest() {
        // ARRANGE
        val mdAsStream = ByteArrayInputStream(
            Paths.get(
                resourcePath.toString(),
                "Fmi3ReferenceFmuModelDescriptions",
                "BouncingBallMD.xml"
            ).toFile().readBytes()
        )

        // ACT
        val modelIdentifier = Fmi3ModelDescription(mdAsStream).getModelIdentifier()

        // ASSERT
        Assertions.assertTrue(modelIdentifier.isNotEmpty())
    }

    @Test
    fun scalarVariablesAsExpectedTest() {
        // ARRANGE
        val mdAsStream = ByteArrayInputStream(
            Paths.get(
                resourcePath.toString(),
                "Fmi3ReferenceFmuModelDescriptions",
                "BouncingBallMD.xml"
            ).toFile().readBytes()
        )
        val expectedVariable = FloatVariable(
            name = "der(h)",
            valueReference = (2).toUInt(),
            description = "Derivative of h",
            causality = Fmi3Causality.Local,
            variability = ModelDescription.Variability.Continuous,
            derivative = (1).toUInt(),
            unit = "m/s",
            quantity = "Velocity",
            declaredType = "Velocity",
            typeIdentifier = Fmi3TypeEnum.Float64Type
        )
        val expectedDependencyKind = Fmi3DependencyKind.Constant
        val expectedDependencyValueRef = (3).toUInt()

        // ACT
        val scalarVariables = Fmi3ModelDescription(mdAsStream).getScalarVariables()
        val actualVariable = scalarVariables[2].variable as FloatVariable
        val initialUnknownDependency = scalarVariables[2].initialUnknownsDependencies.entries.iterator().next()
        val variablesAreEqual =
            actualVariable.name == expectedVariable.name &&
                    actualVariable.valueReference == expectedVariable.valueReference &&
                    actualVariable.description == expectedVariable.description &&
                    actualVariable.causality == expectedVariable.causality &&
                    actualVariable.variability == expectedVariable.variability &&
                    actualVariable.derivative == expectedVariable.derivative &&
                    actualVariable.unit == expectedVariable.unit &&
                    actualVariable.quantity == expectedVariable.quantity &&
                    actualVariable.declaredType == expectedVariable.declaredType &&
                    actualVariable.typeIdentifier == expectedVariable.typeIdentifier

        // ASSERT
        Assertions.assertTrue(
            variablesAreEqual && initialUnknownDependency.value == expectedDependencyKind &&
                    initialUnknownDependency.key.variable.valueReference == expectedDependencyValueRef
        )
    }
}