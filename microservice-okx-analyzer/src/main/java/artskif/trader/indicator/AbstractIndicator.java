package artskif.trader.indicator;

import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.common.Stateable;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractIndicator<C> extends AbstractTimeSeries<C> implements CandleEventListener, Runnable, Stateable, IndicatorPoint {

    protected static final String DEFAULT_SYMBOL = "BTC-USDT";

    protected final CandleEventBus bus;
    private final BlockingQueue<CandleEvent> queue = new ArrayBlockingQueue<>(4096, true);

    private Thread worker;
    private volatile boolean running = false;

    public AbstractIndicator(CandleEventBus bus) {
        this.bus = bus;
    }

    @Override
    protected String getSymbol() {
        return DEFAULT_SYMBOL;
    }


    protected abstract void process(CandleEvent take);

    @PostConstruct
    public void init() {
        log().infof("🔌 [%s] Запуск процесса подсчета индикатора", getName());

        initRestoreBuffer();
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
}
