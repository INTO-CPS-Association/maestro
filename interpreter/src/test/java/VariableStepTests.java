import org.intocps.maestro.interpreter.Fmi2Interpreter;
import org.intocps.maestro.interpreter.values.*;
import org.intocps.maestro.interpreter.values.fmi.FmuValue;
import org.intocps.maestro.interpreter.values.variablestep.VariableStepValue;
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
import java.util.List;

public class VariableStepTests {
//    @Test
//    public void testStepSize() throws IOException, URISyntaxException, ParserConfigurationException, SAXException, XPathExpressionException, InvocationTargetException, IllegalAccessException {
//
//        //Arrange
//        String configUri = VariableStepTests.class.getClassLoader().getResource("variable_step/coe.json").toURI().getPath();
//        String WorkingDirectory = VariableStepTests.class.getClassLoader().getResource("variable_step").getPath();
//        String tankPath = VariableStepTests.class.getClassLoader().getResource("variable_step/singlewatertank-20sim.fmu").getPath();
//        String controllerPath = VariableStepTests.class.getClassLoader().getResource("variable_step/watertankcontroller-c.fmu").getPath();
//        Fmi2Interpreter interpreter = new Fmi2Interpreter(new File(WorkingDirectory));
//        List<StringValue> FMUInstanceNames = new ArrayList<>();
//        List<FmuValue> FMUs = new ArrayList<>();
//        ArrayValue<RealValue> portValues1 = new ArrayValue<>(new ArrayList<>());
//        ArrayValue<RealValue> portValues2 = new ArrayValue<>(new ArrayList<>());
//        ArrayValue<StringValue> portNames = new ArrayValue<>(new ArrayList<>());
//        double endTime = 10.0;
//        double dataPointTime1 = 0.0;
//        double dataPointTime2 = 5.0;
//
//        // create controller
//        FmuValue fmuValueController = (FmuValue) interpreter.createFmiValue(controllerPath, "{afb6bcd7-7160-4881-a8ec-9ba5add8b9d0}");
//        ModelDescription fmuModelDescriptionController = new ModelDescription(fmuValueController.getModule().getModelDescription());
//        String fmuInstanceNameController = "c";
//        FMUInstanceNames.add(new StringValue(fmuInstanceNameController));
//        FMUs.add(fmuValueController);
//        for (int l = 0; l < fmuModelDescriptionController.getScalarVariables().size(); l++) {
//            if (fmuModelDescriptionController.getScalarVariables().get(l).type.type == ModelDescription.Types.Real) {
//                portValues1.getValues().add(new RealValue(0.0));
//                portValues2.getValues().add(new RealValue(10.0));
//                portNames.getValues().add(new StringValue("{controller}.c." + fmuModelDescriptionController.getScalarVariables().get(l).name));
//            }
//        }
//
//        // create tank
//        FmuValue fmuValueTank = (FmuValue) interpreter.createFmiValue(tankPath, "{afb6bcd7-7160-4881-a8ec-9ba5add8b9d1}");
//        ModelDescription fmuModelDescriptionTank = new ModelDescription(fmuValueTank.getModule().getModelDescription());
//        String fmuInstanceNameTank = "t";
//        FMUInstanceNames.add(new StringValue(fmuInstanceNameTank));
//        FMUs.add(fmuValueTank);
//        for (int l = 0; l < fmuModelDescriptionTank.getScalarVariables().size(); l++) {
//            if (fmuModelDescriptionTank.getScalarVariables().get(l).type.type == ModelDescription.Types.Real) {
//                portValues1.getValues().add(new RealValue(0.0));
//                portValues2.getValues().add(new RealValue(10.0));
//                portNames.getValues().add(new StringValue("{tank}.t." + fmuModelDescriptionTank.getScalarVariables().get(l).name));
//            }
//        }
//
//        VariableStepValue variableStepValue = new VariableStepValue(configUri);
//
//        FunctionValue.ExternalFunctionValue addDataPoint = (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("addDataPoint");
//        FunctionValue.ExternalFunctionValue getStepSize = (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("getStepSize");
//        FunctionValue.ExternalFunctionValue setFMUs = (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("setFMUs");
//        FunctionValue.ExternalFunctionValue setEndTime = (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("setEndTime");
//        FunctionValue.ExternalFunctionValue initializePortNames =
//                (FunctionValue.ExternalFunctionValue) variableStepValue.lookup("initializePortNames");
//
//        Value configValMabl = setFMUs.evaluate(new ArrayValue<>(FMUInstanceNames), new ArrayValue<>(FMUs));
//        initializePortNames.evaluate(configValMabl, portNames);
//        setEndTime.evaluate(configValMabl, new RealValue(endTime));
//
//        //Act
//        addDataPoint.evaluate(configValMabl, new RealValue(dataPointTime1), portValues1);
//        Double stepSize1 = ((RealValue) getStepSize.evaluate(configValMabl)).getValue();
//        addDataPoint.evaluate(configValMabl, new RealValue(dataPointTime2), portValues2);
//        Double stepSize2 = ((RealValue) getStepSize.evaluate(configValMabl)).getValue();
//
//        //Assert
//
//        Assert.assertNotNull(stepSize1);
//    }
}
