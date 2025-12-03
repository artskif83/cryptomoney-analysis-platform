package artskif.trader.executor.kafka;

import artskif.trader.executor.bot.CryptoTelegramBot;
import my.signals.v1.Signal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessagesKafkaConsumer {

    private final CryptoTelegramBot telegramBot;

    public MessagesKafkaConsumer(CryptoTelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @KafkaListener(
            topics = "${app.signals-topic}",
            groupId = "${app.consumer-group:executor-bot}"
    )
    public void listen(Signal message) {
        System.out.println("Получено сообщение: " + message);
//        if (telegramBot.getLastChatId() != null) {
//            telegramBot.sendToChat("Сообщение из Kafka: " + message);
//        }
    }
}