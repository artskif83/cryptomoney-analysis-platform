package artskif.trader.indicator;

import artskif.trader.candle.CandlePeriod;
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

    protected abstract CandlePeriod getCandlePeriod();
    protected abstract void process(CandleEvent take);
    protected abstract StateRepository getStateRepository();
    protected abstract Path getPathForStateSave();

    public void init() {
        System.out.println("🔌 [" + getName() + "] Запуск процесса подсчета индикатора");

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
        if (event.period() != getCandlePeriod()) return;

        // Не блокируем продьюсера: если переполнено — логируем дроп
        // При желании можно заменить на offer(ev, timeout, unit) или политику "drop oldest".
        boolean offered = queue.offer(event);
        if (!offered) {
            System.err.println("❌ [" + getName() + "] Очередь обработки переполнена, событие отброшено: " + event);
        }
    }

    @Override
    public void run() {
        System.out.println("🔗 [" + getName() + "] Запущен поток подсчета индикатора: " + Thread.currentThread().getName());
        while (running) {
            try {
                process(queue.take());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                System.out.println("❌ [" + getName() + "] Не удалось обработать точку в потоке: " + Thread.currentThread().getName() + " ошибка - " + ignored);
            }
        }
    }

    protected void restoreState() {
        try {
            System.out.println("📥 [" + getName() + "] Восстанавливаем состояние из хранилища");
            getState().restoreObject(getStateRepository().loadStateFromFile(getPathForStateSave()));
        } catch (IOException e) {
            System.out.println("❌ [" + getName() + "] Не удалось восстановить значение состояния : ");
        }
    }

    protected void saveState() {
        try {
            System.out.println("📥 [" + getName() + "] Сохраняем состояние в хранилище");
            getStateRepository().saveStateToFile(getState(), getPathForStateSave());
        } catch (IOException e) {
            System.out.println("❌ [" + getName() + "] Не удалось сохранить значение состояния : ");
        }
    }
}
