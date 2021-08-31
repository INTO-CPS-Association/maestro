package org.intocps.maestro.util;

public class MablModuleProvider {

    /**
     * Used for testing - not necessarily up to date!
     *
     * @return
     */
    public static String getFaultInjectMabl() {
        return "module FaultInject\n" + "import FMI2;\n" + "{\n" +
                "    FMI2Component faultInject(FMI2 creator, FMI2Component component, string constraintId);\n" +
                "    FMI2Component observe(FMI2 creator, FMI2Component component, string constraintId);\n" +
                "    FMI2Component returnFmuComponentValue(FMI2Component component);\n" + "}\n";
    }
}
