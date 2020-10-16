package org.intocps.maestro.webapi.maestro2;

import org.intocps.maestro.ErrorReporter;
import org.intocps.maestro.MaBLTemplateGenerator.MaBLTemplateConfiguration;
import org.intocps.maestro.Mabl;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.MableInterpreter;
import org.intocps.maestro.webapi.maestro2.interpreter.WebApiInterpreterFactory;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Maestro2Broker {

    final Mabl mabl;
    final File workingDirectory;
    final ErrorReporter reporter;

    public Maestro2Broker(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        this.mabl = new Mabl(workingDirectory, null);
        this.reporter = new ErrorReporter();
        mabl.setReporter(reporter);
        mabl.getSettings().dumpIntermediateSpecs = false;
        mabl.getSettings().inlineFrameworkConfig = true;
    }

    public void generateSpecification(MaBLTemplateConfiguration config) throws Exception {
        mabl.generateSpec(config);
        mabl.expand();
        mabl.dump(workingDirectory);
    }

    public void executeInterpreter(WebSocketSession webSocket, List<String> csvFilter, List<String> webSocketFilter,
            double interval) throws IOException, AnalysisException {
        WebApiInterpreterFactory factory;
        if (webSocket != null) {
            factory = new WebApiInterpreterFactory(webSocket, interval, webSocketFilter, new File(workingDirectory, "outputs.csv"), csvFilter);
        } else {
            factory = new WebApiInterpreterFactory(new File(workingDirectory, "outputs.csv"), csvFilter);
        }
        new MableInterpreter(factory).execute(mabl.getMainSimulationUnit());
    }
}
