package artskif.trader.microserviceokxexecutor;

import artskif.trader.microserviceokxexecutor.orders.OrderManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class MicroserviceOkxExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceOkxExecutorApplication.class, args);
    }

}
