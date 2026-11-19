package org.intocps.maestro.testutils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public enum TestFmu {
    FMI2_FUNCTION_TEST("fmi2--jni--fmi2functiontest.fmu"),
    SINGLE_WATER_TANK("fmi2--maestro--singlewatertank-20sim.fmu"),
    WATER_TANK_CONTROLLER("fmi2--maestro--watertankcontroller-c.fmu"),
    ROLLBACK_END("fmi2--maestro--rollback-end.fmu"),
    ROLLBACK_TEST("fmi2--maestro--rollback-test.fmu"),
    PUMP("fmi2--maestro--pump.fmu"),
    SINK("fmi2--maestro--sink.fmu"),
    MASS_SPRING_DAMPER_1("fmi2--mass-spring-damper--MassSpringDamper1.fmu"),
    MASS_SPRING_DAMPER_2("fmi2--mass-spring-damper--MassSpringDamper2.fmu"),
    THREE_WATER_TANK_1("fmi2--three-tank--threewatertank1.fmu"),
    THREE_WATER_TANK_2("fmi2--three-tank--threewatertank2.fmu"),
    THREE_WATER_TANK_CONTROLLER("fmi2--three-tank--WaterTankController_Standalone.fmu"),
    ROBOT_BODY("fmi2--line-follow-robot--Body_Block.fmu"),
    ROBOT_SENSOR("fmi2--line-follow-robot--Sensor_Block.fmu"),
    ROBOT_SENSOR_01("fmi2--line-follow-robot--Sensor_Block_01.fmu"),
    ROBOT_SENSOR_02("fmi2--line-follow-robot--Sensor_Block_02.fmu"),
    LINE_FOLLOWER_SENSOR_1("fmi2--line-follow-robot--LineFollower_Examples_SensorBlock1.fmu"),
    LINE_FOLLOWER_SENSOR_2("fmi2--line-follow-robot--LineFollower_Examples_SensorBlock2.fmu"),
    BOUNCING_BALL("fmi3--reference--BouncingBall.fmu"),
    FEEDTHROUGH("fmi3--reference--Feedthrough.fmu"),
    FMI3_FUNCTION_TEST("fmi3--jni--fmi3functiontest.fmu"),
    SIGNAL_GENERATOR("fmi3--siggen-feedthrough--SignalGenerator.fmu");

    public static final String RELEASE_URL = "https://github.com/INTO-CPS-Association/test-fmus/releases/download/Release%2F0.0.2/";

    private final String assetName;
    private final String fileName;

    TestFmu(String assetName) {
        this.assetName = assetName;
        this.fileName = assetName.substring(assetName.lastIndexOf("--") + 2);
    }

    public String fileName() {
        return fileName;
    }

    public String assetName() {
        return assetName;
    }

    public static Optional<TestFmu> fromFileName(String fileName) {
        return Arrays.stream(values()).filter(model -> model.fileName.equals(fileName)).findFirst();
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected the generated properties output path");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.write(output, Stream.concat(Stream.of("release.url=" + RELEASE_URL),
                Arrays.stream(values()).map(model -> "asset." + model.fileName() + "=" + model.assetName())).toList(), StandardCharsets.UTF_8);
    }
}