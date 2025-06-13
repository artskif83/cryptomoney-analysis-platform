package artskif.trader.microserviceokxtelegrambot.kafka;

import artskif.trader.microserviceokxtelegrambot.bot.CryptoTelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessagesKafkaConsumer {

    private final CryptoTelegramBot telegramBot;

    public MessagesKafkaConsumer(CryptoTelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    @KafkaListener(topics = "dev-okx-candle-1m", groupId = "candle-group")
    public void listen(String message) {
        System.out.println("Получено сообщение: " + message);
        if (telegramBot.getLastChatId() != null) {
            telegramBot.sendToChat("Сообщение из Kafka: " + message);
        }
    }
}