package artskif.trader.common;

import artskif.trader.dto.CandlestickDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BufferRepository<C> {

    protected abstract ObjectMapper getObjectMapper();

    protected abstract JavaType getMapType();

    public void saveCandlesToFile(Map<Instant, C> items, Path path) throws IOException {
        getObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), items);
    }

    public Map<Instant, C> loadCandlesFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        return getObjectMapper().readValue(path.toFile(), getMapType());
    }

}
