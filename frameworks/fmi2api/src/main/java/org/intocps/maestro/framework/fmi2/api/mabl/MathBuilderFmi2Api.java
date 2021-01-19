package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;

import static org.intocps.maestro.ast.MableAstFactory.newPlusExp;

public class MathBuilderFmi2Api {

    public static Fmi2Builder.ProvidesReferenceExp add(Fmi2Builder.DoubleVariable<PStm> currentCommunicationTime,
            Fmi2Builder.DoubleVariable<PStm> stepSizeVar) {
        return new ExpFmi2Api(newPlusExp(currentCommunicationTime.getReferenceExp(), stepSizeVar.getReferenceExp()));
    }
}
