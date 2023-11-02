package org.intocps.maestro.plugin.verificationsuite.prologverifier;

import com.ugos.jiprolog.engine.*;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.RelationVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InitializationPrologQuery {
    final static Logger logger = LoggerFactory.getLogger(InitializationPrologQuery.class);
    private final PrologGenerator prologGenerator;

    public InitializationPrologQuery(PrologGenerator prologGenerator) {
        this.prologGenerator = prologGenerator;
    }

    public InitializationPrologQuery() {
        this.prologGenerator = new PrologGenerator();
    }

    public boolean initializationOrderIsValid(List<RelationVariable> instantiationOrder, Set<Fmi2SimulationEnvironment.Relation> relations) {
        // New instance of prolog engine
        JIPEngine jip = new JIPEngine();
        JIPTerm queryTerm = null;
        Boolean isCorrectInitializationOrder = false;
        // files are searched in the search path
        Path path = getPathToProlog();
        // parse query
        try {
            // consult file
            jip.consultFile(path + "/initialization.pl");

            var init = prologGenerator.createInitOperationOrder(instantiationOrder);
            var connections = prologGenerator.createConnections(
                    relations.stream().filter(o -> o.getOrigin() == Fmi2SimulationEnvironment.Relation.InternalOrExternal.External)
                            .collect(Collectors.toList()));
            var fmus = prologGenerator.createFMUs(relations);

            queryTerm = jip.getTermParser().parseTerm(String.format("?- isInitSchedule(%s,%s, %s).", init, fmus, connections));

            //queryTerm = jip.getTermParser().parseTerm("?- father(X, Y).");
        } catch (JIPSyntaxErrorException ex) {
            ex.printStackTrace();
        }

        // open Query
        JIPQuery jipQuery = jip.openSynchronousQuery(queryTerm);

        try {
            //If the solution is false the result will be null
            isCorrectInitializationOrder = (jipQuery.nextSolution() != null);
        } catch (JIPRuntimeException ex) {
            logger.error("No solution", ex);
        }

        jip.reset();

        return isCorrectInitializationOrder;
    }

    private Path getPathToProlog() {
        var currentPath = Paths.get("").toAbsolutePath().getParent().normalize().toString();
        var pluginString = "plugins";
        if (currentPath.contains("plugins")) {
            pluginString = "";
        }
        return Paths.get(currentPath, pluginString, "verificationsuite", "src", "main", "resources", "prologCode");
    }

}

