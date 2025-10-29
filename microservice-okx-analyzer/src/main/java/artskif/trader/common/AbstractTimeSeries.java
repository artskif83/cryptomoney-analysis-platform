package artskif.trader.common;

import artskif.trader.buffer.BufferRepository;
import artskif.trader.buffer.BufferedPoint;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C>, Logged {


    protected Instant lastBucket = null;

    protected abstract Path getPathForSave();

    protected abstract BufferRepository<C> getBufferRepository();


    protected void restoreBuffer() {
        try {
            log().infof("📥 [%s] Восстанавливаем информационные свечи из хранилища", getName());
            getBuffer().restoreItems(getBufferRepository().loadCandlesFromFile(getPathForSave()));
        } catch (IOException e) {
            log().errorf("❌ [%s] Не удалось восстановить значение буфера : ", getName());
        }
    }

    protected void saveBuffer() {
        try {
            log().infof("📥 [%s] Сохраняем информационные свечи в хранилище", getName());
            getBufferRepository().saveCandlesToFile(getBuffer().getSnapshot(), getPathForSave());
        } catch (IOException e) {
            log().errorf(e, "❌ [%s] Не удалось сохранить значение буфера : %s", getName(), e.getMessage());
        }
    }
}
