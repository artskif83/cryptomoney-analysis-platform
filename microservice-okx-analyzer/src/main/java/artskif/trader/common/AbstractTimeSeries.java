package artskif.trader.common;

import artskif.trader.buffer.BufferedPoint;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C>, Logged {

    private final AtomicBoolean saveLiveEnabled = new AtomicBoolean(false);
    private final AtomicBoolean saveHistoricalEnabled = new AtomicBoolean(false);

    protected abstract BufferRepository<C> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    protected abstract String getSymbol();

    public abstract String getName();

    public abstract Integer getMaxLiveBufferSize();

    public abstract Integer getMaxHistoryBufferSize();

    @ActivateRequestContext
    protected void initRestoreBuffer() {
        log().infof("📥 [%s] Восстанавливаем информационные свечи из хранилища", getName());
        getLiveBuffer().putItems(getBufferRepository().restoreFromStorage(getMaxLiveBufferSize(), getCandleTimeframe(), getSymbol()));
    }

    protected void initSaveLiveBuffer() {
        if (!isSaveLiveEnabled()) {
            log().infof("📥 [%s] Активировано сохранение активного буфера по расписанию", getName());
        }
        saveLiveEnabled.set(true);
    }


    protected void initSaveHistoricalBuffer() {
        if (!isSaveHistoricalEnabled()) {
            log().infof("📥 [%s] Активировано сохранение исторического буфера по расписанию", getName());
        }
        saveHistoricalEnabled.set(true);
    }

    public boolean isSaveLiveEnabled() {
        return saveLiveEnabled.get();
    }

    public boolean isSaveHistoricalEnabled() {
        return saveHistoricalEnabled.get();
    }

    @ActivateRequestContext
    public void saveBuffer() {
        log().infof("💾 [%s] Сохраняем информационные свечи в хранилище", getName());
        saveLiveBuffer();
        saveHistoricalBuffer();
    }

    @ActivateRequestContext
    protected void saveLiveBuffer() {
        if (isSaveLiveEnabled()) {
            log().debugf("💾 [%s] Сохраняем актуальный буфер", getName());
            getBufferRepository().saveFromMap(getLiveBuffer().getDataMap());
            saveLiveEnabled.set(false);
        }
    }

    @ActivateRequestContext
    protected void saveHistoricalBuffer() {
        if (isSaveHistoricalEnabled()) {
            log().debugf("💾 [%s] Сохраняем исторический буфер", getName());
            getBufferRepository().saveFromMap(getHistoricalBuffer().getDataMap());
            saveHistoricalEnabled.set(false);
        }
    }
}
