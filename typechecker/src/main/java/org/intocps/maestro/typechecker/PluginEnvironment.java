package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AFunctionType;
import org.intocps.maestro.plugin.IMaestroPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PluginEnvironment extends BaseEnvironment {

    public Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> getTypesPlugins() {
        return plugins;
    }

    final Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins;

    public Collection<IMaestroPlugin> getPlugins() {
        return plugins.keySet();
    }

    public PluginEnvironment(Environment outer, Map<IMaestroPlugin, Map<AFunctionDeclaration, AFunctionType>> plugins) {
        super(outer, plugins.keySet().stream()
                .map(aFunctionDeclarationAFunctionTypeMap -> aFunctionDeclarationAFunctionTypeMap.getDeclaredUnfoldFunctions().stream())
                .flatMap(Function.identity()).collect(Collectors.toList()));
        this.plugins = plugins;


    }
}
