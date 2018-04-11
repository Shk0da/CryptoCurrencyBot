package ai.trading.bot.repository.binance;

import ai.trading.bot.repository.BasicBalanceRepository;
import ai.trading.bot.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository("binanceBalanceRepository")
public class BinanceBalanceRepository extends BasicBalanceRepository {

    public BinanceBalanceRepository(TaskScheduler scheduler,
                                    @Qualifier("binanceAccountService") AccountService accountService,
                                    @Value("#{'${binance.balance.limits}'.split(';')}") List<String> limits) {
        limits.forEach(mapLimit -> {
            String[] symbolToLimit = mapLimit.split(":");
            setLimit(symbolToLimit[0], Double.valueOf(symbolToLimit[1]));
        });

        scheduler.scheduleWithFixedDelay(() -> accountService
                .getInfo()
                .forEach(wallet -> {
                    setBalance(wallet.getName().toUpperCase(), wallet.getFree());
                    log.debug("BinanceBalance {}: {}", wallet.getName().toUpperCase(), wallet.getFree());
                }), 60_000);
    }
}
