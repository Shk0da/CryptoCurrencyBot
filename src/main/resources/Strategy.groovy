import ai.trading.bot.domain.Order
import ai.trading.bot.service.AccountService
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

    double diff = 1 // in USD
    double lot = 0.000001 // in BTC

    @Autowired
    @Qualifier("binanceAccountService")
    AccountService binanceAccountService

    @Autowired
    @Qualifier("bitfinexAccountService")
    AccountService bitfinexAccountService

    def lastUpdateBtcUsd
    def log = LoggerFactory.getLogger(Strategy.class)

    Strategy() {
        log.info String.format("Strategy loaded successful! With param: diff = %.2fUSD, lot = %fBTC", diff, lot)
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
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSDT.bid - BTCUSD.ask) + "USD"
            log.info "BTCUSDT:" + BTCUSDT.bid + "; BTCUSD:" + BTCUSD.ask
            log.info "Do arbitrage: " + String.format("%f", lot) + "BTC"
            log.info "--------------------------------------------------"

            def sellResult = binanceAccountService.createOrder(Order.builder()
                    .symbol("BTCUSDT")
                    .side(Order.Side.SELL)
                    .type(Order.Type.MARKET)
                    .quantity(lot)
                    .build())

            log.info "------------- BTCUSDT SELL RESULT ----------------"
            if (sellResult == null || sellResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                sellResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"

            def buyResult = bitfinexAccountService.createOrder(Order.builder()
                    .symbol("BTCUSD")
                    .side(Order.Side.BUY)
                    .type(Order.Type.MARKET)
                    .quantity(lot)
                    .build())

            log.info "-------------- BTCUSD BUY RESULT -----------------"
            if (buyResult == null || buyResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                buyResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= diff) {
            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSD.bid - BTCUSDT.ask) + "USD"
            log.info "BTCUSD:" + BTCUSD.bid + "; BTCUSDT:" + BTCUSDT.ask
            log.info "Do arbitrage: " + String.format("%f", lot) + "BTC"
            log.info "--------------------------------------------------"

            def sellResult = bitfinexAccountService.createOrder(Order.builder()
                    .symbol("BTCUSD")
                    .side(Order.Side.SELL)
                    .type(Order.Type.MARKET)
                    .quantity(lot)
                    .build())

            log.info "-------------- BTCUSD SELL RESULT ----------------"
            if (sellResult == null || sellResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                sellResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"

            def buyResult = binanceAccountService.createOrder(Order.builder()
                    .symbol("BTCUSDT")
                    .side(Order.Side.BUY)
                    .type(Order.Type.MARKET)
                    .quantity(lot)
                    .build())

            log.info "------------- BTCUSDT BUY RESULT -----------------"
            if (buyResult == null || buyResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                buyResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"
        }
    }

    @Async
    def LTCUSDandLTCUSDT() {

    }
}
