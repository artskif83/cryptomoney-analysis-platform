package artskif.trader.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;

import java.util.List;

@Getter
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandlestickMessage {
    private Arg arg;
    private List<List<String>> data;

    @Getter
    public static class Arg {
        private String channel;
        private String instId;
    }
}
