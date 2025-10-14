package artskif.trader.indicator;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.common.StateRepository;
import artskif.trader.common.Stateable;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractIndicator<C> extends AbstractTimeSeries<C> implements CandleEventListener, Runnable, Stateable, IndicatorPoint {

    protected final CandleEventBus bus;

    public AbstractIndicator(CandleEventBus bus) {
        this.bus = bus;
    }

    private final BlockingQueue<CandleEvent> queue = new ArrayBlockingQueue<>(4096, true);

    private Thread worker;
    private volatile boolean running = false;

    protected abstract void process(CandleEvent take);
    protected abstract StateRepository getStateRepository();
    protected abstract Path getPathForStateSave();

    public void init() {
        log().infof("🔌 [%s] Запуск процесса подсчета индикатора", getName());

        restoreBuffer();
        if (isStateful()) restoreState();
        // подписка на события и старт фонового потока
        bus.subscribe(this);
        running = true;
        worker = new Thread(this, getName() + "-worker");
        worker.start();
    }

    public void shutdown() {
        bus.unsubscribe(this);
        running = false;
        if (worker != null) worker.interrupt();
    }

    @Override
    public void onCandle(CandleEvent event) {
        if (event.period() != getCandleTimeframe()) return;

        // Не блокируем продьюсера: если переполнено — логируем дроп
        // При желании можно заменить на offer(ev, timeout, unit) или политику "drop oldest".
        boolean offered = queue.offer(event);
        if (!offered) {
            System.err.println("❌ [" + getName() + "] Очередь обработки переполнена, событие отброшено: " + event);
        }
    }

    @Override
    public void run() {
        log().infof("🔗 [%s] Запущен поток подсчета индикатора", getName());
        while (running) {
            try {
                process(queue.take());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                log().errorf(ignored, "❌ [%s] Не удалось обработать точку в потоке", getName());
            }
        }
    }

    protected void restoreState() {
        try {
            log().infof("📥 [%s] Восстанавливаем состояние из хранилища", getName());
            getState().restoreObject(getStateRepository().loadStateFromFile(getPathForStateSave()));
        } catch (IOException e) {
            log().errorf(e,"❌ [%s] Не удалось восстановить значение состояния", getName());
        }
    }

    protected void saveState() {
        try {
            log().infof("📥 [%s] Сохраняем состояние в хранилище", getName());
            getStateRepository().saveStateToFile(getState(), getPathForStateSave());
        } catch (IOException e) {
            log().errorf(e,"❌ [%s] Не удалось сохранить значение состояния", getName());
        }
    }
}
