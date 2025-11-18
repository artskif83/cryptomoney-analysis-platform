package artskif.trader.common;

import artskif.trader.buffer.BufferedPoint;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C>, Logged {

    private final AtomicBoolean saveEnabled = new AtomicBoolean(false);

    protected abstract BufferRepository<C> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    protected abstract String getSymbol();

    public abstract String getName();

    @ActivateRequestContext
    protected void initRestoreBuffer() {
        log().infof("📥 [%s] Восстанавливаем информационные свечи из хранилища", getName());
        getLiveBuffer().restoreItems(getBufferRepository().restoreFromStorage(getCandleTimeframe(), getSymbol()));
    }

    protected void initSaveBuffer() {
        if (!isSaveEnabled()) {
            log().infof("📥 [%s] Активировано сохранение по расписанию", getName());
        }
        saveEnabled.set(true);
    }

    public boolean isSaveEnabled() {
        return saveEnabled.get();
    }

    @ActivateRequestContext
    public void saveBuffer() {
        log().infof("💾 [%s] Сохраняем информационные свечи в хранилище", getName());
        getBufferRepository().saveFromMap(getLiveBuffer().getDataMap());
        saveEnabled.set(false);
    }
}
