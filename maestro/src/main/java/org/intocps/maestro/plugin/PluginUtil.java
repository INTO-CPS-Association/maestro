package org.intocps.maestro.plugin;

import org.apache.commons.text.StringEscapeUtils;
import org.intocps.maestro.ast.node.AConfigStm;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.intocps.maestro.core.StringAnnotationProcessor.processStringAnnotations;

public class PluginUtil {
    public static IPluginConfiguration getConfiguration(IMaestroExpansionPlugin plugin, AConfigStm configStm,
            File specificationFolder) throws IOException {

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
}
