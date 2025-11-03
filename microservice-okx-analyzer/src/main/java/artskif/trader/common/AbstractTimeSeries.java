package artskif.trader.common;

import artskif.trader.buffer.BufferedPoint;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.repository.BufferRepository;
import jakarta.enterprise.context.control.ActivateRequestContext;

public abstract class AbstractTimeSeries<C> implements BufferedPoint<C>, Logged {

    protected abstract BufferRepository<C> getBufferRepository();

    protected abstract CandleTimeframe getCandleTimeframe();

    protected abstract String getSymbol();

    public abstract String getName();

    @ActivateRequestContext
    protected void initRestoreBuffer() {
        log().infof("📥 [%s] Восстанавливаем информационные свечи из хранилища", getName());
        getBuffer().restoreItems(getBufferRepository().restoreFromStorage(getCandleTimeframe(), getSymbol()));
    }

    @ActivateRequestContext
    protected void initSaveBuffer() {
        log().infof("📥 [%s] Сохраняем информационные свечи в хранилище", getName());
        getBufferRepository().saveFromMap(getBuffer().getSnapshot());
    }
}
