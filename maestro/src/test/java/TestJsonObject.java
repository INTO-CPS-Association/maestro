import com.fasterxml.jackson.annotation.JsonProperty;
import org.intocps.maestro.core.API.StepAlgorithm;

public class TestJsonObject {
    @JsonProperty
    public boolean initialize = false;
    @JsonProperty
    public String stepAlgorithm = "";

    public StepAlgorithm getStepAlgorithm() {
        if (stepAlgorithm == "fixedStep") {
            return StepAlgorithm.FIXEDSTEP;
        }
        return null;
    }
}
