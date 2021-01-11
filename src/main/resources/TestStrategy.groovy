import ai.trading.bot.service.AccountService
import ai.trading.bot.service.StrategyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled

/**
 * Test Example
 * Тестовая сратегия арбитража между крипто-биржами.
 * Поддерживает "горячую замену" в течении 5 секунд.
 */
class TestStrategy implements StrategyService {

    double diff = 70 // in USD
    double lot = 0.0002 // in BTC :: BitFinex minimum size for BTC/USD is 0.0002

    @Autowired
    @Qualifier("binanceAccountService")
    AccountService binanceAccountService

    @Autowired
    @Qualifier("bitfinexAccountService")
    AccountService bitfinexAccountService

    def lastUpdateBtcUsd
    def log = LoggerFactory.getLogger(Strategy.class)

    def balanceBinanceUSD = 850
    def balanceBinanceBTC = 0.025

    def balanceBitfinexUSD = 850
    def balanceBitfinexBTC = 0.025

    TestStrategy() {
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
            def binanceBTCBalance = balanceBinanceBTC
            def binanceBTCLimit = binanceAccountService.balanceRepository().getLimit("BTC")

            if (binanceBTCBalance <= binanceBTCLimit) {
                log.warn "Binance BTC balance: " + binanceBTCBalance
                return
            }

            def bitfinexUSDBalance = balanceBitfinexUSD
            def bitfinexUSDLimit = bitfinexAccountService.balanceRepository().getLimit("USD")

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lot) + "BTC"
            log.info "BTCUSDT:" + BTCUSDT.bid + "; BTCUSD:" + BTCUSD.ask
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSDT.bid - BTCUSD.ask) + "USD"
            log.info "--------------------------------------------------"

            balanceBinanceBTC = balanceBinanceBTC - lot
            balanceBinanceUSD = balanceBinanceUSD + (lot * BTCUSDT.bid)

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT SELL RESULT ----------------"

            balanceBitfinexUSD = balanceBitfinexUSD - (lot * BTCUSD.ask)
            balanceBitfinexBTC = balanceBitfinexBTC + lot

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD BUY RESULT -----------------"
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= diff) {

            // проверка покупательной способности wallet
            def bitfinexBTCBalance = balanceBitfinexBTC
            def bitfinexBTCLimit = bitfinexAccountService.balanceRepository().getLimit("BTC")

            if (bitfinexBTCBalance <= bitfinexBTCLimit) {
                log.warn "Bitfinex BTC balance: " + bitfinexBTCBalance
                return
            }

            def binanceUSDTBalance = balanceBinanceUSD
            def binanceUSDTLimit = binanceAccountService.balanceRepository().getLimit("USDT")

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lot) + "BTC"
            log.info "BTCUSD:" + BTCUSD.bid + "; BTCUSDT:" + BTCUSDT.ask
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSD.bid - BTCUSDT.ask) + "USD"
            log.info "--------------------------------------------------"

            balanceBitfinexBTC = balanceBitfinexBTC - lot
            balanceBitfinexUSD = balanceBitfinexUSD + (lot * BTCUSD.bid)

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD SELL RESULT ----------------"

            balanceBinanceUSD = balanceBinanceUSD - (lot * BTCUSDT.ask)
            balanceBinanceBTC = balanceBinanceBTC + lot

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT BUY RESULT -----------------"
        }
    }

    @Async
    def LTCUSDandLTCUSDT() {
        // method body
    }
}
