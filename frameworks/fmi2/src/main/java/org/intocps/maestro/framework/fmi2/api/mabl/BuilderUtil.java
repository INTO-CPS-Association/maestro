package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStateDesignator;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;

import static org.intocps.maestro.ast.MableAstFactory.newAAssignmentStm;

public class BuilderUtil {
    final static Logger logger = LoggerFactory.getLogger(BuilderUtil.class);

    public static List<PStm> createTypeConvertingAssignment(MablApiBuilder builder, Fmi2Builder.Scope<PStm> scope, PStateDesignator designator,
            PExp value, PType valueType, PType targetType) {

        List<PStm> statements = new Vector<>();
        // if (new TypeComparator().compatible(targetType, valueType)) {
        statements.add(newAAssignmentStm(designator, value));
        //} else {
        //FIXME type conversion missing
        logger.error("No implementation for type conversion");

        // }

         /* we need this but cannot add it <dependency>
            <groupId>org.into-cps.maestro</groupId>
            <artifactId>typechecker</artifactId>
            <version>2.0.4-SNAPSHOT</version>
        </dependency>

        the API needs to be moved to a separate module

        */
        return statements;
    }
}
