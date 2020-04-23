package org.intocps.maestro.plugin;

public interface IContext {

    <T> T getPluginData(Class<? extends IMaestroPlugin> clz);
}
