import ai.trading.bot.service.StrategyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled

/**
 * Основная сратегия арбитража между крипто-биржами.
 * Поддерживает "горячую замену" в течении 5 секунд.
 */
class Strategy implements StrategyService {

    double diff = 5 // in USD
    double lot = 0.000001 // in BTC

    @Autowired
    @Qualifier("binanceAccountService")
    def binanceAccountService

    @Autowired
    @Qualifier("bitfinexAccountService")
    def bitfinexAccountService

    def lastUpdateBtcUsd
    def log = LoggerFactory.getLogger(Strategy.class)

    Strategy() {
        log.info "Strategy loaded successful! With param: diff = " + diff + "USD, lot = " + lot + "BTC"
    }

    /**
     * Этот метод выполняется раз в fixedDelay милисекунд.
     */
    @Scheduled(fixedDelay = 1000L)
    void run() {
        BTCUSDandBTCUSDT()
        LTCUSDandLTCUSDT()
        // ... and other pairs
    }

    @Async
    def BTCUSDandBTCUSDT() {
        def BTCUSDT = binanceAccountService.candleRepository().getLastCandle("BTCUSDT")
        def BTCUSD = bitfinexAccountService.candleRepository().getLastCandle("BTCUSD")
        if (BTCUSDT == null || BTCUSD == null) return

        // время пары свечей
        def candleDateTime = BTCUSDT.dateTime.isAfter(BTCUSD.dateTime) ? BTCUSDT.dateTime : BTCUSD.dateTime
        if (candleDateTime == lastUpdateBtcUsd) return else lastUpdateBtcUsd = candleDateTime

        // Если {цена продажи} BTCUSDT больше {цена покупки} BTCUSD чем {diff}
        if (BTCUSDT.bid - BTCUSD.ask >= diff) {
            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + (BTCUSDT.bid - BTCUSD.ask) + "USD"
            log.info "BTCUSDT:" + BTCUSDT.bid + "; BTCUSD:" + BTCUSD.ask
            log.info "Do arbitrage: " + lot + " BTC"
            log.info "--------------------------------------------------"
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= diff) {
            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + (BTCUSD.bid - BTCUSDT.ask) + "USD"
            log.info "BTCUSD:" + BTCUSD.bid + "; BTCUSDT:" + BTCUSDT.ask
            log.info "Do arbitrage: " + lot + " BTC"
            log.info "--------------------------------------------------"
        }
    }

    @Async
    def LTCUSDandLTCUSDT() {

    }
}
