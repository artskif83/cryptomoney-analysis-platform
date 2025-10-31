package artskif.trader.common;

import artskif.trader.buffer.BufferFileRepository;
import artskif.trader.buffer.BufferedPoint;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;

import java.nio.file.Path;
import java.time.Instant;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C>, Logged {

    protected Instant lastBucket = null;


    protected abstract Path getPathForSave();

    protected abstract BufferFileRepository<C> getBufferFileRepository();

    protected abstract BufferRepository<C> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    protected abstract String getSymbol();

    public abstract String getName();

    @ActivateRequestContext
    protected void restoreBuffer() {
        log().infof("📥 [%s] Восстанавливаем информационные свечи из хранилища", getName());
        getBuffer().restoreItems(getBufferRepository().restoreFromStorage(getCandleTimeframe(), getSymbol()));
    }

    @ActivateRequestContext
    protected void saveBuffer() {
        log().infof("📥 [%s] Сохраняем информационные свечи в хранилище", getName());
        getBufferRepository().saveFromMap(getBuffer().getSnapshot());
    }
}
