import ai.trading.bot.service.AccountService
import ai.trading.bot.service.StrategyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled

class Strategy implements StrategyService {

    @Autowired
    @Qualifier("binanceAccountService")
    private AccountService binanceAccountService;

    @Autowired
    @Qualifier("bitfinexAccountService")
    private AccountService bitfinexAccountService;

    @Scheduled(fixedDelay = 1000L)
    void run() {

    }
}
