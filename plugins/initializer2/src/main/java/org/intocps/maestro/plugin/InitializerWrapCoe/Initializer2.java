package org.intocps.maestro.plugin.InitializerWrapCoe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.IMaestroPlugin;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Initializer2 implements IMaestroUnfoldPlugin {
    final AFunctionDeclaration f1 = MableAstFactory.newAFunctionDeclaration(new LexIdentifier("initialize", null), null, MableAstFactory.newAVoidType());
    SpecGen specGen;
    Config config;
    public Initializer2()
    {
        this.specGen = new SpecGen();
    }

    public Initializer2(SpecGen specGen){
        this.specGen = specGen;
    }

    @Override
    public String getName() {
        return Initializer2.class.getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(f1).collect(Collectors.toSet());
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env, IErrorReporter errorReporter) throws UnfoldException {
        if( declaredFunction == this.f1 && config instanceof Config)
        {
            this.config = (Config)config;
            try {
                PStm statement = specGen.run(this.config.configuration.toString(), this.config.start_message.toString());
                return statement;
            } catch (JsonProcessingException e) {
                throw new UnfoldException("Failed to unfold:", e);
            }
        }
        else {
            throw new UnfoldException("Bad config type");}
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        JsonNode root = new ObjectMapper().readTree(is);
        JsonNode configuration = root.get("configuration");
        JsonNode start_message = root.get("start_message");
        Config conf = new Config(configuration, start_message);
        return conf;
    }

    public static class Config implements IPluginConfiguration {

        public Config(JsonNode configuration, JsonNode start_message)
        {
            this.configuration = configuration;
            this.start_message = start_message;
        }
        private JsonNode configuration;

        private JsonNode start_message;

        public JsonNode getConfiguration() {
            return configuration;
        }

        public JsonNode getStart_message() {
            return start_message;
        }

    }

}
