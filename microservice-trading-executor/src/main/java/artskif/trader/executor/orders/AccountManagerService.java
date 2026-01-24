package artskif.trader.executor.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public final class AccountManagerService {

    private static final Logger log = LoggerFactory.getLogger(AccountManagerService.class);

    private final AccountClient accountClient;

    public AccountManagerService(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    /**
     * Получает доступный баланс USDT на торговом аккаунте
     * @return Баланс USDT или null в случае ошибки
     */
    public BigDecimal getUsdtBalance() {
        log.debug("💰 Запрос баланса USDT");
        BigDecimal balance = accountClient.getUsdtBalance();
        if (balance != null) {
            log.info("💰 Текущий баланс USDT: {}", balance);
        } else {
            log.error("❌ Не удалось получить баланс USDT");
        }
        return balance;
    }
}

