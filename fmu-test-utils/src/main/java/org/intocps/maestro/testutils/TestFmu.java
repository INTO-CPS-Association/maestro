package org.intocps.maestro.testutils;

import java.util.Arrays;
import java.util.Optional;

public enum TestFmu {
    FMI2_FUNCTION_TEST("fmi2functiontest.fmu"),
    SINGLE_WATER_TANK("singlewatertank-20sim.fmu"),
    WATER_TANK_CONTROLLER("watertankcontroller-c.fmu"),
    ROLLBACK_END("rollback-end.fmu"),
    ROLLBACK_TEST("rollback-test.fmu"),
    PUMP("pump.fmu"),
    SINK("sink.fmu"),
    MASS_SPRING_DAMPER_1("MassSpringDamper1.fmu"),
    MASS_SPRING_DAMPER_2("MassSpringDamper2.fmu"),
    THREE_WATER_TANK_1("threewatertank1.fmu"),
    THREE_WATER_TANK_2("threewatertank2.fmu"),
    THREE_WATER_TANK_CONTROLLER("WaterTankController_Standalone.fmu"),
    ROBOT_BODY("Body_Block.fmu"),
    ROBOT_SENSOR("Sensor_Block.fmu"),
    ROBOT_SENSOR_01("Sensor_Block_01.fmu"),
    ROBOT_SENSOR_02("Sensor_Block_02.fmu"),
    LINE_FOLLOWER_SENSOR_1("LineFollower_Examples_SensorBlock1.fmu"),
    LINE_FOLLOWER_SENSOR_2("LineFollower_Examples_SensorBlock2.fmu"),
    BOUNCING_BALL("BouncingBall.fmu"),
    FEEDTHROUGH("Feedthrough.fmu"),
    FMI3_FUNCTION_TEST("fmi3functiontest.fmu"),
    SIGNAL_GENERATOR("SignalGenerator.fmu");

    private final String fileName;

    TestFmu(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    public String assetName() {
        return FmuTestModelRepository.assetName(this);
    }

    public static Optional<TestFmu> fromFileName(String fileName) {
        return Arrays.stream(values()).filter(model -> model.fileName.equals(fileName)).findFirst();
    }
}