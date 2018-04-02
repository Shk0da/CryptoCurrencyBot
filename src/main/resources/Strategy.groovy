import ai.trading.bot.service.StrategyService
import org.springframework.scheduling.annotation.Scheduled

class Strategy implements StrategyService {

    @Scheduled(fixedDelay = 1000L)
    void run() {

    }
}