import org.intocps.fmi.FmuInvocationException;
import org.intocps.fmi.IFmu;
import org.intocps.maestro.interpreter.Fmi2Interpreter;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.interpreter.values.variablestep.VariableStepValue;
import org.intocps.orchestration.coe.FmuFactory;
import org.intocps.orchestration.coe.config.ModelConnection;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VariableStepTests {
    @Test
    public void stepSize() throws IOException, URISyntaxException, ParserConfigurationException, SAXException, XPathExpressionException, InvocationTargetException, IllegalAccessException {

        //Arrange
        URI configUri = VariableStepTests.class.getClassLoader().getResource("VariableStep/coe.json").toURI();
        String WorkingDirectory = VariableStepTests.class.getClassLoader().getResource("VariableStep").getPath();
        String[] fmuPaths = new String[]{VariableStepTests.class.getClassLoader().getResource("VariableStep/GATestController.fmu").getPath(),
                VariableStepTests.class.getClassLoader().getResource("VariableStep/GATestController2.fmu").getPath()};
        Fmi2Interpreter interpreter = new Fmi2Interpreter(new File(WorkingDirectory));
        List<StringValue> FMUInstanceNames = new ArrayList<>();
        List<FmuValue> FMUs = new ArrayList<>();
        ArrayValue<RealValue> portValues = new ArrayValue<>(new ArrayList<>());
        ArrayValue<StringValue> portNames = new ArrayValue<>(new ArrayList<>());
        Double endTime = 10.0;
        Double dataPointTime = 0.1;

        for(int i = 0; i < fmuPaths.length; i++){
            FmuValue fmuValue = (FmuValue) interpreter.createFmiValue(fmuPaths[i], "{afb6bcd7-7160-4881-a8ec-9ba5add8b9d" + i + "}");
            ModelDescription fmuModelDescription = new ModelDescription(fmuValue.getModule().getModelDescription());
            String fmuInstanceName = fmuModelDescription.getModelId() + i;
            FMUInstanceNames.add(new StringValue(fmuInstanceName));
            FMUs.add(fmuValue);
            for(int l = 0; l < fmuModelDescription.getScalarVariables().size(); l++){
                if(fmuModelDescription.getScalarVariables().get(l).type.type == ModelDescription.Types.Real){
                    portValues.getValues().add(new RealValue(1.0));
                    portNames.getValues().add(new StringValue("{}."+fmuInstanceName+"."+fmuModelDescription.getScalarVariables().get(l).name));
                }
            }
        }

        String configuration = new String(Files.readAllBytes(Paths.get(configUri)));

        VariableStepValue variableStepValue = new VariableStepValue(configuration);

        //Act
        FunctionValue.ExternalFunctionValue setFMUs =
                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("setFMUs");
        Value configValMabl = setFMUs.evaluate(new ArrayValue<>(FMUInstanceNames), new ArrayValue<>(FMUs));

        FunctionValue.ExternalFunctionValue initializePortNames =
                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("initializePortNames");
        initializePortNames.evaluate(configValMabl, portNames);

        FunctionValue.ExternalFunctionValue addDataPoint =
                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("addDataPoint");
        addDataPoint.evaluate(configValMabl, new RealValue(dataPointTime), portValues);

        FunctionValue.ExternalFunctionValue setEndTime =
                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("setEndTime");
        setEndTime.evaluate(configValMabl, new RealValue(endTime));

        FunctionValue.ExternalFunctionValue getStepSize =
                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("getStepSize");
        Value realValue = getStepSize.evaluate(configValMabl);
        Double value = ((RealValue)realValue).getValue();

        //Assert

        //Double expectedValue = 2; ??
        Assert.assertTrue(value != 0);

    }
}
