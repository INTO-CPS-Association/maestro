package org.intocps.maestro.interpreter.extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spencerwi.either.Either;
import org.intocps.maestro.interpreter.api.IValueLifecycleHandler;
import org.intocps.maestro.interpreter.values.Value;
import org.intocps.maestro.interpreter.values.csv.CsvDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.DataWriterValue;
import org.intocps.maestro.interpreter.values.datawriter.IDataListener;
import org.intocps.maestro.interpreter.values.datawriter.IntervalDataWriter;
import org.intocps.maestro.interpreter.values.datawriter.WebSocketDataWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@IValueLifecycleHandler.ValueLifecycle(name = "DataWriter")
public class DataWriterLifecycleHandler extends BaseLifecycleHandler {

    static final String DEFAULT_CSV_FILENAME = "outputs.csv";
    final String DATA_WRITER_TYPE_NAME = this.getClass().getAnnotation(IValueLifecycleHandler.ValueLifecycle.class).name();

    final List<Supplier<IDataListener>> writers = new Vector<>();

    private boolean isTextEqual(JsonNode node, String text) {
        return node.isTextual() && node.asText().equals(text);
    }

    private String getMemberText(JsonNode node, String memberName) {
        if (node.hasNonNull(memberName) && node.get(memberName).isTextual()) {
            return node.get(memberName).asText();
        }
        return null;
    }

    public DataWriterLifecycleHandler(File workingDirectory, InputStream config) throws IOException {

//        String dataWriterFileName = DEFAULT_CSV_FILENAME;

        if (config != null) {
            JsonNode configTree = new ObjectMapper().readTree(config);

            if (configTree.has(DATA_WRITER_TYPE_NAME)) {
                JsonNode dwConfig = configTree.get(DATA_WRITER_TYPE_NAME);

                for (JsonNode val : dwConfig) {
                    if (val.hasNonNull("type")) {
                        JsonNode typeNode = val.get("type");

                        if (isTextEqual(typeNode, "CSV")) {
                            //we have CSV writer instance
                            String filename = getMemberText(dwConfig, "filename");
                            final String dataWriterFileName = filename == null ? DEFAULT_CSV_FILENAME : filename;

                            final List<String> filter = getFilter(val);

                            writers.add(() -> new CsvDataWriter(
                                    workingDirectory == null ? new File(dataWriterFileName) : new File(workingDirectory, dataWriterFileName),
                                    filter));
                        } else if (isTextEqual(typeNode, "CSV_LiveStream")) {
                            //we have CSV livestream  writer instance
                            String filename = getMemberText(dwConfig, "filename");
                            final String dataWriterFileName = filename == null ? "livestream.csv" : filename;

                            final List<String> filter = getFilter(val);
                            double interval = 0.1;
                            if (val.hasNonNull("interval") && val.get("interval").isNumber()) {
                                interval = val.get("interval").asDouble();
                            }

                            final double finalInterval = interval;
                            writers.add(() ->new IntervalDataWriter(finalInterval, new CsvDataWriter(
                                    workingDirectory == null ? new File(dataWriterFileName) : new File(workingDirectory, dataWriterFileName),
                                    filter)));
                        }else if (isTextEqual(typeNode, "WebSocket")) {

                            int port = val.get("port").asInt(8082);

                            final List<String> filter = getFilter(val);
                            double interval = 0.1;
                            if (val.hasNonNull("interval") && val.get("interval").isNumber()) {
                                interval = val.get("interval").asDouble();
                            }

                            final double finalInterval = interval;
                            writers.add(() ->new IntervalDataWriter(finalInterval, new WebSocketDataWriter(port,filter)));
                        }
                    }
                }
            }
        }
    }

    private static List<String> getFilter(JsonNode val) {
        if (val.hasNonNull("filter")) {
            return
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(val.get("filter").iterator(), Spliterator.ORDERED),
                            false).map(JsonNode::asText).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public Either<Exception, Value> instantiate(List<Value> args) {
        return Either.right(new DataWriterValue(writers.stream().map(Supplier::get).collect(Collectors.toList())));
    }
}