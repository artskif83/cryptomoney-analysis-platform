package artskif.trader.indicator;

import artskif.trader.candle.CandleType;
import artskif.trader.common.AbstractTimeSeries;
import artskif.trader.events.CandleEvent;
import artskif.trader.events.CandleEventBus;
import artskif.trader.events.CandleEventListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.NoArgsConstructor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@NoArgsConstructor(force = true)
public abstract class AbstractIndicator<C> extends AbstractTimeSeries<C> implements CandleEventListener, Runnable {

    protected final CandleEventBus bus;

    public AbstractIndicator(CandleEventBus bus) {
        this.bus = bus;
    }

    private final BlockingQueue<CandleEvent> queue = new ArrayBlockingQueue<>(4096, true);

    private Thread worker;
    private volatile boolean running = false;

    protected abstract CandleType getCandleType();
    protected abstract void process(CandleEvent take);

    @PostConstruct
    void initAbstractIndicator() {
        System.out.println("🔌 [" + getName() + "] Запуск процесса подсчета индикатора");

        restoreBuffer();
        // подписка на события и старт фонового потока
        bus.subscribe(this);
        running = true;
        worker = new Thread(this, getName() + "-worker");
        worker.start();
    }

    @PreDestroy
    void shutdown() {
        bus.unsubscribe(this);
        running = false;
        if (worker != null) worker.interrupt();
    }

    @Override
    public void onCandle(CandleEvent event) {
        if (event.type() != getCandleType()) return;

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
}
