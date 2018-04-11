package ai.trading.bot.repository.bitfinex;

import ai.trading.bot.repository.BasicBalanceRepository;
import ai.trading.bot.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository("bitfinexBalanceRepository")
public class BitfinexBalanceRepository extends BasicBalanceRepository {

    public BitfinexBalanceRepository(TaskScheduler scheduler,
                                     @Qualifier("bitfinexAccountService") AccountService accountService,
                                     @Value("#{'${bitfinex.balance.limits}'.split(';')}") List<String> limits) {
        limits.forEach(mapLimit -> {
            String[] symbolToLimit = mapLimit.split(":");
            setLimit(symbolToLimit[0], Double.valueOf(symbolToLimit[1]));
        });

        scheduler.scheduleWithFixedDelay(() -> accountService
                .getInfo()
                .forEach(wallet -> {
                    setBalance(wallet.getName().toUpperCase(), wallet.getFree());
                    log.debug("BitfinexBalance {}: {}", wallet.getName().toUpperCase(), wallet.getFree());
                }), 60_000);
    }
}
