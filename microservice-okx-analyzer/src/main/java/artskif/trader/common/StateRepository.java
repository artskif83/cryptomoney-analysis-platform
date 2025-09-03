package artskif.trader.common;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class StateRepository {

    protected final ObjectMapper objectMapper;
    protected final JavaType mapType;

    public void saveStateToFile(PointState item, Path path) throws IOException {
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(path.toFile(), item);
    }

    public PointState loadStateFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        return objectMapper.readValue(path.toFile(), mapType);
    }
}
