package org.intocps.maestro.interpreter.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.csv.CsvDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@IValueLifecycleHandler.ValueLifecycle(name = "DataWriter")
public class DataWriterLifecycleHandler extends BaseLifecycleHandler {

    static final String DEFAULT_CSV_FILENAME = "outputs.csv";
    final String DATA_WRITER_TYPE_NAME;
    final String dataWriterFileNameFinal;
    final List<String> dataWriterFilterFinal;
    final private File workingDirectory;

    public DataWriterLifecycleHandler(File workingDirectory, InputStream config) throws IOException {
        this.workingDirectory = workingDirectory;

        DATA_WRITER_TYPE_NAME = this.getClass().getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name();
        String dataWriterFileName = DEFAULT_CSV_FILENAME;
        List<String> dataWriterFilter = null;

        if (config != null) {
            JsonNode configTree = new ObjectMapper().readTree(config);

            if (configTree.has(DATA_WRITER_TYPE_NAME)) {
                JsonNode dwConfig = configTree.get(DATA_WRITER_TYPE_NAME);

                for (JsonNode val : dwConfig) {
                    if (val.has("type") && val.get("type").isTextual() && val.get("type").asText().equals("CSV")) {
                        if (val.has("filename") && val.get("filename").isTextual()) {
                            dataWriterFileName = val.get("filename").asText();
                        }
                        if (val.has("filter")) {
                            dataWriterFilter =
                                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(val.get("filter").iterator(), Spliterator.ORDERED),
                                            false).map(v -> v.asText()).collect(Collectors.toList());
                        }

                    }
                }
            }
        }

        dataWriterFileNameFinal = dataWriterFileName;
        dataWriterFilterFinal = dataWriterFilter;
    }

    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new DataWriterValue(Collections.singletonList(new CsvDataWriter(
                workingDirectory == null ? new File(dataWriterFileNameFinal) : new File(workingDirectory, dataWriterFileNameFinal),
                dataWriterFilterFinal))));
    }
}