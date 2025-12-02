package artskif.trader.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import my.signals.v1.Signal;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaProducer {

    private static final Logger LOG = Logger.getLogger(KafkaProducer.class);

    @Inject
    @Channel("signals")
    Emitter<Signal> emitterSignal;

    public void sendSignal(Signal signal) {
        emitterSignal.send(signal);
    }
}
