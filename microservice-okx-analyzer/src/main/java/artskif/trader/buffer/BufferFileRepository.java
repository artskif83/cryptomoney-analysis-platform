package artskif.trader.buffer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class BufferFileRepository<C> {

    protected final ObjectMapper objectMapper;
    protected final JavaType mapType;

    public void saveCandlesToFile(Map<Instant, C> items, Path path) throws IOException {
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), items);
    }

    public Map<Instant, C> loadCandlesFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        return objectMapper.readValue(path.toFile(), mapType);
    }

}
