package artskif.trader.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class MicroserviceTradingExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceTradingExecutorApplication.class, args);
    }

}
