package artskif.trader.common;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.mapper.CandlestickMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C> {

    protected Instant lastBucket = null;

    protected abstract Path getPathForSave();

    protected abstract BufferRepository<C> getBufferRepository();


    protected void restoreBuffer() {
        try {
            System.out.println("📥 [" + getName() + "] Восстанавливаем информационные свечи из хранилища");
            getBuffer().restoreItems(getBufferRepository().loadCandlesFromFile(getPathForSave()));
        } catch (IOException e) {
            System.out.println("❌ [" + getName() + "] Не удалось восстановить значение буфера : ");
        }
    }

    protected void saveBuffer() {
        try {
            System.out.println("📥 [" + getName() + "] Сохраняем информационные свечи в хранилище");
            getBufferRepository().saveCandlesToFile(getBuffer().getSnapshot(), getPathForSave());
        } catch (IOException e) {
            System.out.println("❌ [" + getName() + "] Не удалось сохранить значение буфера : ");
        }
    }
}
