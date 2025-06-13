package artskif.trader.microserviceokxtelegrambot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class MicroserviceOkxTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceOkxTelegramBotApplication.class, args);
    }

}
