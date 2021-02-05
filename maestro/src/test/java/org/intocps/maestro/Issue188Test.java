package org.intocps.maestro;

import org.junit.Assert;
import org.junit.Test;

public class Issue188Test {
    @Test
    public void InterpretSpec() throws Exception {
        Assert.assertTrue(Main.argumentHandler(new String[]{"-i", Issue188Test.class.getClassLoader().getResource("188/spec.mabl").getPath()}));
    }

    @Test
    public void CreateAndInterpretNewSpec() throws Exception {
        String initializeJson = Issue188Test.class.getClassLoader().getResource("188/initialize.json").getPath();
        String simulateJson = Issue188Test.class.getClassLoader().getResource("188/simulate.json").getPath();
        String dumpPath = "target/test-classes/188/dump";
        Assert.assertTrue(Main.argumentHandler(new String[]{"-sg1", initializeJson, simulateJson, "-d", dumpPath}));
    }

}
