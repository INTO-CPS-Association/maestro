package org.intocps.maestro.verifiers;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.NodeCollector;
import org.intocps.maestro.ast.node.ALoadExp;
import org.intocps.maestro.ast.node.ARootDocument;
import org.intocps.maestro.ast.node.AStringLiteralExp;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.fmi2.Fmi2FmuValidator;
import org.intocps.maestro.plugin.IMaestroVerifier;
import org.intocps.maestro.plugin.SimulationFramework;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

@SimulationFramework(framework = Framework.FMI2)
public class Fmi2ModelDescriptionVerifier implements IMaestroVerifier {
    @Override
    public boolean verify(ARootDocument doc, IErrorReporter reporter) {
        List<ALoadExp> fmi2LoadStatements = NodeCollector.collect(doc, ALoadExp.class).orElse(new Vector<>()).stream()
                .filter(l -> !l.getArgs().isEmpty() && l.getArgs().get(0) instanceof AStringLiteralExp &&
                        ((AStringLiteralExp) l.getArgs().get(0)).getValue().equals("FMI2")).collect(Collectors.toList());

        Fmi2FmuValidator validator = new Fmi2FmuValidator();
        boolean verified = true;
        for (ALoadExp load : fmi2LoadStatements) {

            AVariableDeclaration decl = load.getAncestor(AVariableDeclaration.class);
            String name = decl == null ? "?" : decl.getName().getText();

            PExp pathExp = load.getArgs().get(2);
            if (pathExp instanceof AStringLiteralExp) {
                String path = ((AStringLiteralExp) pathExp).getValue();
                verified = verified && validator.validate(name, new File(path).toURI(), reporter);
            }
        }


        return verified;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }
}