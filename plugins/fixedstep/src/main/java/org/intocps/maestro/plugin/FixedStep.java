package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.UnitRelationship;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SimulationFramework(framework = Framework.FMI2)
public class FixedStep implements IMaestroUnfoldPlugin {
    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return null;
    }

    @Override
    public PStm unfold(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config, ISimulationEnvironment env,
            IErrorReporter errorReporter) throws UnfoldException {

        if (formalArguments == null || formalArguments.isEmpty()) {
            throw new UnfoldException("Invalid args");
        }

        List<LexIdentifier> componentNames = formalArguments.stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                .map(AIdentifierExp::getName).collect(Collectors.toList());

        Set<UnitRelationship.Relation> relations = env.getRelations(componentNames);

        //relations.stream().filter(r -> r.getDirection() == In)

        return null;
    }

    @Override
    public boolean requireConfig() {
        return true;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return new FixedstepConfig(new ObjectMapper().readValue(is, Integer.class));
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
