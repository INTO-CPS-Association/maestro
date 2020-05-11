package org.intocps.maestro.multimodelparser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
public class MultiModelMessage
{

    //@JsonProperty
    public Map<String, String> fmus;
    //@JsonProperty
    public Map<String, List<String>> connections;
    //@JsonProperty
    public Map<String, Object> parameters;


    @JsonIgnore public Map<String, URI> getFmuFiles() throws Exception
    {
        Map<String, URI> files = new HashMap<>();

        if (fmus != null)
        {
            for (Map.Entry<String, String> entry : fmus.entrySet())
            {
                try
                {
                    files.put(entry.getKey(), new URI(entry.getValue()));
                } catch (Exception e)
                {
                    throw new Exception(
                            entry.getKey() + "-" + entry.getValue() + ": "
                                    + e.getMessage(), e);
                }
            }
        }

        return files;
    }

}
