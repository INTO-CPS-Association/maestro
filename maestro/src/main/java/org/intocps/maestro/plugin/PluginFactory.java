package org.intocps.maestro.plugin;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.SubTypesScanner;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginFactory {

    public static Collection<IMaestroPlugin> getPlugins() {
        Reflections reflections = new Reflections(IMaestroPlugin.class.getPackage().getName(), new SubTypesScanner());

        try {

            Set<Class<? extends IMaestroPlugin>> subTypes = reflections.getSubTypesOf(IMaestroPlugin.class);

            return subTypes.stream().map(c -> {
                try {
                    return c.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        } catch (ReflectionsException e) {
            if (e.getMessage().contains("not configured")) {
                return Collections.emptySet();
            }
            throw e;
        }
    }
}
