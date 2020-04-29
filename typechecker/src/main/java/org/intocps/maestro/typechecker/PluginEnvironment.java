package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AFunctionType;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PluginEnvironment extends BaseEnvironment {

    final Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins;
    private final Map<String, String> rawPluginJsonConfigs;
    private final Map<String, IPluginConfiguration> pluginConfigs = new HashMap<>();

    public PluginEnvironment(Environment outer, Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins,
            Map<String, String> rawPluginJsonContext) {
        super(outer, plugins.keySet().stream()
                .map(aFunctionDeclarationAFunctionTypeMap -> aFunctionDeclarationAFunctionTypeMap.getDeclaredUnfoldFunctions().stream())
                .flatMap(Function.identity()).collect(Collectors.toList()));
        this.plugins = plugins;

        this.rawPluginJsonConfigs = rawPluginJsonContext;
    }

    public Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> getTypesPlugins() {
        return plugins;
    }

    public Collection<IMaestroPlugin> getPlugins() {
        return plugins.keySet();
    }

    public IPluginConfiguration getConfiguration(IMaestroPlugin plugin) throws PluginConfigurationNotFoundException {

        if (!plugin.requireConfig()) {
            return null;
        }

        String key = plugin.getName() + "-" + plugin.getVersion();

        if (pluginConfigs.containsKey(key)) {
            return pluginConfigs.get(key);
        }

        //TODO make version search
        if (!rawPluginJsonConfigs.containsKey(key)) {
            throw new PluginConfigurationNotFoundException("No raw JSON data available with key: " + key);
        }

        try (InputStream is = new ByteArrayInputStream(rawPluginJsonConfigs.get(key).getBytes(StandardCharsets.UTF_8))) {
            IPluginConfiguration config = plugin.parseConfig(is);
            pluginConfigs.put(key, config);
            return config;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
