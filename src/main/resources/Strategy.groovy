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

    double diff = 40 // in USD
    double lot = 0.002 // in BTC :: BitFinex minimum size for BTC/USD is 0.002

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
    @Scheduled(fixedDelay = 10000L)
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

            // проверка покупательной способности wallet
            def binanceBTCBalance = binanceAccountService.balanceRepository().getBalance("BTC")
            def binanceBTCLimit = binanceAccountService.balanceRepository().getLimit("BTC")

            if (binanceBTCBalance <= binanceBTCLimit) {
                log.warn "Binance BTC balance: " + binanceBTCBalance
                return
            }

            def bitfinexUSDBalance = bitfinexAccountService.balanceRepository().getBalance("USD")
            def bitfinexUSDLimit = bitfinexAccountService.balanceRepository().getLimit("USD")

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

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
            def sellOrderId = 0
            if (sellResult == null || sellResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                sellResult.each { key, val ->
                    log.info "${key}: ${val}"
                    if (key == "orderId") sellOrderId = val
                }
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
                // пробуем отменить предыдущий ордер, если он еще не реализован
                if (sellOrderId != 0) {
                    binanceAccountService.cancelOrder("BTCUSDT", sellOrderId)
                }
                return
            } else {
                buyResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= diff) {

            // проверка покупательной способности wallet
            def bitfinexBTCBalance = bitfinexAccountService.balanceRepository().getBalance("BTC")
            def bitfinexBTCLimit = bitfinexAccountService.balanceRepository().getLimit("BTC")

            if (bitfinexBTCBalance <= bitfinexBTCLimit) {
                log.warn "Bitfinex BTC balance: " + bitfinexBTCBalance
                return
            }

            def binanceUSDTBalance = binanceAccountService.balanceRepository().getBalance("USDT")
            def binanceUSDTLimit = binanceAccountService.balanceRepository().getLimit("USDT")

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

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
            def sellOrderId = 0
            if (sellResult == null || sellResult.isEmpty()) {
                log.info "Something wrong! Break..."
                log.info "--------------------------------------------------"
                return
            } else {
                sellResult.each { key, val ->
                    log.info "${key}: ${val}"
                    if (key == "id") sellOrderId = val
                }
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
                // пробуем отменить предыдущий ордер, если он еще не реализован
                if (sellOrderId != 0) {
                    bitfinexAccountService.cancelOrder("BTCUSD", sellOrderId)
                }
                return
            } else {
                buyResult.each { key, val -> log.info "${key}: ${val}" }
            }
            log.info "--------------------------------------------------"
        }
    }

    @Async
    def LTCUSDandLTCUSDT() {
        // method body
    }
}
