package org.intocps.maestro.plugin.InitializerWrapCoe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.IMaestroUnfoldPlugin;
import org.intocps.maestro.plugin.IPluginConfiguration;
import org.intocps.maestro.plugin.InitializerWrapCoe.Spec.StatementContainer;
import org.intocps.maestro.plugin.SimulationFramework;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class InitializerUsingCOE implements IMaestroUnfoldPlugin {
    final AFunctionDeclaration fun = MableAstFactory.newAFunctionDeclaration(new LexIdentifier("initialize", null),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("startTime")),
                    newAFormalParameter(newAIntNumericPrimitiveType(), newAIdentifier("endTime"))), MableAstFactory.newAVoidType());
    SpecGen specGen;
    Config config;

    public InitializerUsingCOE() {
        this.specGen = new SpecGen();
    }

    public InitializerUsingCOE(SpecGen specGen) {
        this.specGen = specGen;
    }

    @Override
    public String getName() {
        return InitializerUsingCOE.class.getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(fun).collect(Collectors.toSet());
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException {

        // Reset statement container
        StatementContainer.reset();

        if (declaredFunction != this.fun) {
            throw new UnfoldException("Invalid function");
        }

        if (formalArguments == null || formalArguments.size() != fun.getFormals().size()) {
            throw new UnfoldException("Invalid args");
        }

        if (env == null) {
            throw new UnfoldException("Simulation environment must not be null");
        }

        List<LexIdentifier> knownComponentNames = null;

        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (!compDecl.isPresent()) {
                throw new UnfoldException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        PExp startTime = formalArguments.get(1).clone();
        PExp endTime = formalArguments.get(2).clone();

        if (config instanceof Config) {
            this.config = (Config) config;
            try {
                PStm statement =
                        specGen.run(knownComponentNames, this.config.configuration.toString(), this.config.start_message.toString(), startTime,
                                endTime);
                return statement;
            } catch (IOException | NanoHTTPD.ResponseException e) {
                throw new UnfoldException("Failed to unfold:", e);
            }
        } else {
            throw new UnfoldException("Bad config type");
        }
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

        private final JsonNode configuration;
        private final JsonNode start_message;

        public Config(JsonNode configuration, JsonNode start_message) {
            this.configuration = configuration;
            this.start_message = start_message;
        }

        public JsonNode getConfiguration() {
            return configuration;
        }

        public JsonNode getStart_message() {
            return start_message;
        }

    }

}
