package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PExp;
import org.intocps.maestro.ast.PStateDesignator;
import org.intocps.maestro.ast.PStm;
import org.intocps.maestro.framework.fmi2.FmiSimulationEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.*;

public class StateHandler {
    final static Logger logger = LoggerFactory.getLogger(StateHandler.class);
    final boolean supportsGetSetState;
    final List<LexIdentifier> componentNames;
    final String fix_comp_states = "fix_comp_states";
    Function<LexIdentifier, PStateDesignator> getCompStatusDesignator;
    BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus;
    Function<LexIdentifier, PExp> getCompStateDesignator;

    public StateHandler(List<LexIdentifier> componentNames, FmiSimulationEnvironment env,
            Function<LexIdentifier, PStateDesignator> getCompStatusDesignator,
            BiConsumer<Map.Entry<Boolean, String>, Map.Entry<LexIdentifier, List<PStm>>> checkStatus) {
        this.componentNames = componentNames;
        this.getCompStatusDesignator = getCompStatusDesignator;
        this.checkStatus = checkStatus;
        supportsGetSetState =
                env.getInstances().stream().filter(f -> componentNames.stream().anyMatch(m -> m.getText().equals(f.getKey()))).allMatch(pair -> {
                    try {
                        return pair.getValue().modelDescription.getCanGetAndSetFmustate();
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                        return false;
                    }
                });

        this.getCompStateDesignator = comp -> arrayGet(fix_comp_states, componentNames.indexOf(comp));
        logger.debug("Expand with get/set state: {}", supportsGetSetState);
    }

    public List<PStm> allocate() {
        if (!supportsGetSetState) {
            return Collections.emptyList();
        }

        return Collections.singletonList(newVariable(fix_comp_states, newANameType("FmuState"), componentNames.size()));
    }

    public List<PStm> getAllStates() {
        if (!supportsGetSetState) {
            return Collections.emptyList();
        }
        //get states
        Consumer<List<PStm>> getAllStates = (list) -> componentNames.forEach(comp -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    call(newAIdentifierExp((LexIdentifier) comp.clone()), "getState", getCompStateDesignator.apply(comp))));
            checkStatus.accept(Map.entry(true, "get state failed"), Map.entry(comp, list));
        });

        List<PStm> statements = new Vector<>();
        getAllStates.accept(statements);
        return statements;
    }

    public List<PStm> freeAllStates() {
        if (!supportsGetSetState) {
            return Collections.emptyList();
        }

        //free states
        Consumer<List<PStm>> freeAllStates = (list) -> componentNames.forEach(comp -> {
            list.add(newAAssignmentStm(getCompStatusDesignator.apply(comp),
                    call(newAIdentifierExp((LexIdentifier) comp.clone()), "freeState", getCompStateDesignator.apply(comp))));
            checkStatus.accept(Map.entry(true, "free state failed"), Map.entry(comp, list));
        });

        List<PStm> statements = new Vector<>();
        freeAllStates.accept(statements);
        return statements;
    }
}
