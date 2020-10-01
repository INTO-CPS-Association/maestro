import com.fasterxml.jackson.annotation.JsonProperty;

public class TestJsonObject {
    @JsonProperty
    public boolean initialize = false;
    @JsonProperty
    public boolean simulate = false;
    @JsonProperty("use_local_spec")
    public boolean useLocalSpec = false;
    @JsonProperty("use_log_levels")
    public boolean useLogLevels = false;
}
