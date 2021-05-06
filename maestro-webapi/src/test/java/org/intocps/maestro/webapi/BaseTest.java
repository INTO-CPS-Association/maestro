package org.intocps.maestro.webapi;

import org.intocps.maestro.webapi.controllers.SessionController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * If this logic is to be used ConditionalIgnoreRule needs to be reworked as Junit 5 does not have rules.
 */
//public class BaseTest {
//    //@Rule <--- Not available in Junit 5 so needs to be reworked.
//    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();
//
//    @BeforeAll
//    public static void setupClass() {
//        SessionController.test = true;
//    }
//
//    public static class DymolaLicenseWin32 implements ConditionalIgnoreRule.IgnoreCondition {
//        @Override
//        public boolean isSatisfied() {
//            return new HasDymolaLicense().isSatisfied() || new Win32Only().isSatisfied(); //&& (new Win32Only().isSatisfied() == false);
//        }
//    }
//
//    public static class NonMac extends RunOnlyOn {
//        public NonMac() {
//            super(Platform.Win32, Platform.Win64, Platform.Linux32, Platform.Linux64);
//        }
//    }
//
//    public static class MacOnly extends RunOnlyOn {
//        public MacOnly() {
//            super(Platform.Mac);
//        }
//    }
//
//    public static class Win32Only extends RunOnlyOn {
//        public Win32Only() {
//            super(Platform.Win32);
//        }
//    }
//
//    public static class Win64Only extends RunOnlyOn {
//        public Win64Only() {
//            super(Platform.Win64);
//        }
//    }
//
//    public static class HasDymolaLicense extends HasEnvironmentVariable {
//        public HasDymolaLicense() {
//            super("DYMOLA_RUNTIME_LICENSE");
//        }
//    }
//
//    public static class HasEnvironmentVariable implements ConditionalIgnoreRule.IgnoreCondition {
//        private final String environmentVariable;
//
//        public HasEnvironmentVariable(String envVar) {
//            environmentVariable = envVar;
//        }
//
//        @Override
//        public boolean isSatisfied() {
//            return System.getenv(environmentVariable) == null;
//        }
//    }
//    @ExtendWith(ConditionalIgnoreRule.IgnoreCondition.class)
//    public static class RunOnlyOn implements ConditionalIgnoreRule.IgnoreCondition {
//        private final Platform[] platforms;
//
//        public RunOnlyOn(Platform... platforms) {
//            this.platforms = platforms;
//        }
//
//        @Override
//        public boolean isSatisfied() {
//            String osName = System.getProperty("os.name");
//
//            int index = osName.indexOf(' ');
//            if (index != -1) {
//                osName = osName.substring(0, index);
//            }
//
//            String arch = System.getProperty("os.arch");
//
//            for (Platform platform : platforms) {
//                if (platform.osName.equalsIgnoreCase(osName) && platform.arch.equals(arch)) {
//                    return false;
//                }
//            }
//            return true;
//            // return System.getProperty("os.name").startsWith("Windows");
//        }
//
//        public enum Platform {
//            Mac("Mac", "x86_64"),
//
//            Win32("Windows", "x86"),
//
//            Win64("Windows", "amd64"),
//
//            Linux32("Linux", "x86"),
//
//            Linux64("Linux", "amd64");
//
//            public final String osName;
//            public final String arch;
//
//            private Platform(String osName, String arch) {
//                this.osName = osName;
//                this.arch = arch;
//            }
//        }
//    }
//}
