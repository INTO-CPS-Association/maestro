package org.intocps.maestro.webapi.maestro2.interpreter;

import com.spencerwi.either.Either;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.interpreter.DefaultExternalValueFactory;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.csv.CsvDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WebApiInterpreterFactory extends DefaultExternalValueFactory {


    public WebApiInterpreterFactory(File workingDirectory, WebSocketSession ws, double interval, List<String> webSocketFilter, File csvOutputFile,
            List<String> csvFilter) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        super(workingDirectory, null);

        this.lifecycleHandlers.put(DataWriterLifecycleHandler.class.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                new WebDataWriterAndWebsocketLifecycleHandler(csvFilter, csvOutputFile, ws, webSocketFilter, interval));
    }


    public WebApiInterpreterFactory(File workingDirectory, File csvOutputFile,
            List<String> csvFilter) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        super(workingDirectory, null);
        this.lifecycleHandlers.put(DataWriterLifecycleHandler.class.getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name(),
                new WebDataWriterLifecycleHandler(csvFilter, csvOutputFile));
    }

    static class WebDataWriterLifecycleHandler extends BaseLifecycleHandler {


        final private List<String> csvFilter;
        final private File csvOutputFile;

        WebDataWriterLifecycleHandler(List<String> csvFilter, File csvOutputFile) {
            this.csvFilter = csvFilter;
            this.csvOutputFile = csvOutputFile;
        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            return Either.right(new DataWriterValue(Collections.singletonList(new CsvDataWriter(csvOutputFile, csvFilter))));
        }
    }

    static class WebDataWriterAndWebsocketLifecycleHandler extends BaseLifecycleHandler {


        final private List<String> csvFilter;
        final private File csvOutputFile;
        private final WebSocketSession ws;
        private final List<String> webSocketFilter;
        private final double interval;

        WebDataWriterAndWebsocketLifecycleHandler(List<String> csvFilter, File csvOutputFile, WebSocketSession ws, List<String> webSocketFilter,
                double interval) {
            this.csvFilter = csvFilter;
            this.csvOutputFile = csvOutputFile;
            this.ws = ws;
            this.webSocketFilter = webSocketFilter;
            this.interval = interval;
        }

        @Override
        public Either<Exception, Value> instantiate(List<Value> args) {
            if (ws == null) {
                return Either.left(new AnalysisException("No websocket present"));
            }

            return Either.right(new DataWriterValue(
                    Arrays.asList(new CsvDataWriter(csvOutputFile, csvFilter), new WebsocketDataWriter(ws, webSocketFilter, interval))));
        }
    }
}
