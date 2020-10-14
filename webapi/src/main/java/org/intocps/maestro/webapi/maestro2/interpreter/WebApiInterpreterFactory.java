package org.intocps.maestro.webapi.maestro2.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.values.csv.CsvDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.util.Arrays;

public class WebApiInterpreterFactory extends DefaultExternalValueFactory {

    public WebApiInterpreterFactory(WebSocketSession ws, File csvOutputFile) {
        super();
        this.instantiators.put(this.DATA_WRITER_TYPE_NAME, args -> {
            if (ws == null) {
                return Either.left(new AnalysisException("No websocket present"));
            } else {
                return Either.right(new DataWriterValue(Arrays.asList(new CsvDataWriter(csvOutputFile, environment), new WebsocketDataWriter(ws))));
            }


        });
    }
}
