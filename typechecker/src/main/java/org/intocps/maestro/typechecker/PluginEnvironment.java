package org.intocps.maestro.typechecker;

import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.AConfigStm;
import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AFunctionType;
import org.intocps.maestro.plugin.IMaestroExpansionPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.intocps.maestro.core.StringAnnotationProcessor.processStringAnnotations;

public class PluginEnvironment extends BaseEnvironment {

    final Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins;
    private final Map<String, IPluginConfiguration> pluginConfigs = new HashMap<>();

    public PluginEnvironment(Environment outer, Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins) {
        super(outer, plugins.keySet().stream()
                .flatMap(aFunctionDeclarationAFunctionTypeMap -> aFunctionDeclarationAFunctionTypeMap.getDeclaredUnfoldFunctions().stream())
                .collect(Collectors.toList()));
        this.plugins = plugins;

    }

    public Map<IMaestroExpansionPlugin, Map<AFunctionDeclaration, AFunctionType>> getTypesPlugins() {
        return plugins;
    }

    public Collection<IMaestroExpansionPlugin> getPlugins() {
        return plugins.keySet();
    }

    public IPluginConfiguration getConfiguration(IMaestroExpansionPlugin plugin, AConfigStm configStm, File specificationFolder) throws IOException {

        if (!plugin.requireConfig() || configStm == null) {
            return null;
        }

        String data = configStm.getConfig();
        data = processStringAnnotations(specificationFolder, data);
        try (InputStream is = new ByteArrayInputStream(StringEscapeUtils.unescapeJava(data).getBytes(StandardCharsets.UTF_8))) {
            IPluginConfiguration config = plugin.parseConfig(is);
            return config;
        }
    }


    public static class PluginConfigurationNotFoundException extends Exception {
        public PluginConfigurationNotFoundException() {
        }

        public PluginConfigurationNotFoundException(String message) {
            super(message);
        }

        public PluginConfigurationNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PluginConfigurationNotFoundException(Throwable cause) {
            super(cause);
        }

        public PluginConfigurationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
