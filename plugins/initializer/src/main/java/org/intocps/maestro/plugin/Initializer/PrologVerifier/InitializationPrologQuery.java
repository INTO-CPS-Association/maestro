package org.intocps.maestro.plugin.Initializer.PrologVerifier;


import com.ugos.jiprolog.engine.*;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.plugin.UnfoldException;
import org.intocps.maestro.plugin.env.UnitRelationship;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InitializationPrologQuery {
    private final EnumMap<ModelDescription.Causality, String> causalitytoMethod =
            new EnumMap<ModelDescription.Causality, String>(ModelDescription.Causality.class){
        {
            put(ModelDescription.Causality.Output,"getOut");
            put(ModelDescription.Causality.Input,"setIn");
        }
    };

    private final PrologGenerator prologGenerator;

    public InitializationPrologQuery(PrologGenerator prologGenerator){
        this.prologGenerator = prologGenerator;
    }

    public InitializationPrologQuery(){
        this.prologGenerator = new PrologGenerator();
    }

    public boolean initializationOrderIsValid(List<UnitRelationship.Variable> instantiationOrder, Set<UnitRelationship.Relation> relations) {
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

            var init = prologGenerator.CreateInitOperationOrder(instantiationOrder);
            var connections = prologGenerator.createConnections(relations.stream().filter(o -> o.getOrigin() == UnitRelationship.Relation.InternalOrExternal.External)
                    .collect(Collectors.toList()));
            var fmus = prologGenerator.createFMUs(relations);

            queryTerm =
                    jip.getTermParser().parseTerm(String.format("?- isInitSchedule(%s,%s, %s).", init, fmus, connections));

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
            System.out.println(ex.getMessage());
        }

        jip.reset();

        return isCorrectInitializationOrder;
    }

    private Path getPathToProlog() {
        var current = Paths.get("").toAbsolutePath().getParent().normalize().toString();
        return Paths.get(current, "plugins", "initializer", "src", "main", "resources", "prologCode");
    }


}
