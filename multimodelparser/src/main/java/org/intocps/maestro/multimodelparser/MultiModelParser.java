package org.intocps.maestro.multimodelparser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public class MultiModelParser {
    final ObjectMapper mapper = new ObjectMapper();
    public MultiModelMessage ParseMultiModel(InputStream is){
        MultiModelMessage msg = null;
        try {
            msg = mapper.readValue(is, MultiModelMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return msg;
    }

    public static void ConstructInputToOutputMap(MultiModelMessage mmm)
    {

    }
}