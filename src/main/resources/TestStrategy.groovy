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

    double diffBtc = 150 // in USD
    double lotBtc = 0.0002 // in BTC :: BitFinex minimum size for BTC/USD is 0.0002

    double diffEth = 50 // in USD
    double lotEth = 0.006 // in ETH :: BitFinex minimum size for ETH/USD is 0.006

    @Autowired
    @Qualifier("binanceAccountService")
    AccountService binanceAccountService

    @Autowired
    @Qualifier("bitfinexAccountService")
    AccountService bitfinexAccountService

    def lastUpdateBtcUsd
    def lastUpdateEthUsd
    def log = LoggerFactory.getLogger(Strategy.class)

    def balanceBinanceUSD = 362.34
    def balanceBinanceBTC = 0.043
    def balanceBinanceETH = 0.5

    def balanceBitfinexUSD = 1659
    def balanceBitfinexBTC = 0.007
    def balanceBitfinexETH = 0.5

    TestStrategy() {
        log.info "Strategy loaded successful!"
        log.info String.format("With param: diffBtc = %.2fUSD, lotBtc = %fBTC", diffBtc, lotBtc)
        log.info String.format("With param: diffEth = %.2fUSD, lotEth = %fBTC", diffEth, lotEth)
    }

    /**
     * Этот метод выполняется раз в fixedDelay милисекунд.
     */
    @Scheduled(fixedDelay = 10000L)
    void run() {
        BTCUSDandBTCUSDT()
        ETHUSDandETHUSDT()
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
        if (BTCUSDT.bid - BTCUSD.ask >= diffBtc) {

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSDT.bid - BTCUSD.ask) + "USD"
            log.info "--------------------------------------------------"

            // проверка покупательной способности wallet
            def binanceBTCBalance = balanceBinanceBTC
            def binanceBTCLimit = lotBtc * 2

            if (binanceBTCBalance <= binanceBTCLimit) {
                log.warn "Binance BTC balance: " + binanceBTCBalance
                return
            }

            def bitfinexUSDBalance = balanceBitfinexUSD
            def bitfinexUSDLimit = diffBtc

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotBtc) + "BTC"
            log.info "BTCUSDT:" + BTCUSDT.bid + "; BTCUSD:" + BTCUSD.ask
            log.info "--------------------------------------------------"

            balanceBinanceBTC = balanceBinanceBTC - lotBtc
            balanceBinanceUSD = balanceBinanceUSD + (lotBtc * BTCUSDT.bid)

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT SELL RESULT ----------------"

            balanceBitfinexUSD = balanceBitfinexUSD - (lotBtc * BTCUSD.ask)
            balanceBitfinexBTC = balanceBitfinexBTC + lotBtc

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD BUY RESULT -----------------"
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= diffBtc) {

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSD.bid - BTCUSDT.ask) + "USD"
            log.info "--------------------------------------------------"

            // проверка покупательной способности wallet
            def bitfinexBTCBalance = balanceBitfinexBTC
            def bitfinexBTCLimit = lotBtc * 2

            if (bitfinexBTCBalance <= bitfinexBTCLimit) {
                log.warn "Bitfinex BTC balance: " + bitfinexBTCBalance
                return
            }

            def binanceUSDTBalance = balanceBinanceUSD
            def binanceUSDTLimit = diffBtc

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotBtc) + "BTC"
            log.info "BTCUSD:" + BTCUSD.bid + "; BTCUSDT:" + BTCUSDT.ask
            log.info "--------------------------------------------------"

            balanceBitfinexBTC = balanceBitfinexBTC - lotBtc
            balanceBitfinexUSD = balanceBitfinexUSD + (lotBtc * BTCUSD.bid)

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD SELL RESULT ----------------"

            balanceBinanceUSD = balanceBinanceUSD - (lotBtc * BTCUSDT.ask)
            balanceBinanceBTC = balanceBinanceBTC + lotBtc

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT BUY RESULT -----------------"
        }
    }

    @Async
    def ETHUSDandETHUSDT() {
        def ETHUSDT = binanceAccountService.candleRepository().getLastCandle("ETHUSDT")
        def ETHUSD = bitfinexAccountService.candleRepository().getLastCandle("ETHUSD")
        if (ETHUSDT == null || ETHUSD == null) return

        // время пары свечей
        def candleDateTime = ETHUSDT.dateTime.isAfter(ETHUSD.dateTime) ? ETHUSDT.dateTime : ETHUSD.dateTime
        if (candleDateTime == lastUpdateEthUsd) return else lastUpdateEthUsd = candleDateTime

        // Если {цена продажи} ETHUSDT больше {цена покупки} ETHUSD чем {diff}
        if (ETHUSDT.bid - ETHUSD.ask >= diffEth) {

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", ETHUSDT.bid - ETHUSD.ask) + "USD"
            log.info "--------------------------------------------------"

            // проверка покупательной способности wallet
            def binanceETHBalance = balanceBinanceETH
            def binanceETHLimit = lotEth * 2

            if (binanceETHBalance <= binanceETHLimit) {
                log.warn "Binance ETH balance: " + binanceETHBalance
                return
            }

            def bitfinexUSDBalance = balanceBitfinexUSD
            def bitfinexUSDLimit = diffEth

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotEth) + "ETH"
            log.info "ETHUSDT:" + ETHUSDT.bid + "; ETHUSD:" + ETHUSD.ask
            log.info "--------------------------------------------------"

            balanceBinanceETH = balanceBinanceETH - lotEth
            balanceBinanceUSD = balanceBinanceUSD + (lotEth * ETHUSDT.bid)

            log.info "--------------------------------------------------"
            log.info "Binance ETH: " + balanceBinanceETH
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- ETHUSDT SELL RESULT ----------------"

            balanceBitfinexUSD = balanceBitfinexUSD - (lotEth * ETHUSD.ask)
            balanceBitfinexETH = balanceBitfinexETH + lotEth

            log.info "--------------------------------------------------"
            log.info "Bitfinex ETH: " + balanceBitfinexETH
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- ETHUSD BUY RESULT -----------------"
        }

        // Если {цена продажи} ETHUSD больше {цена покупки} ETHUSDT чем {diff}
        if (ETHUSD.bid - ETHUSDT.ask >= diffEth) {

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", ETHUSD.bid - ETHUSDT.ask) + "USD"
            log.info "--------------------------------------------------"

            // проверка покупательной способности wallet
            def bitfinexETHBalance = balanceBitfinexETH
            def bitfinexETHLimit = lotEth * 2

            if (bitfinexETHBalance <= bitfinexETHLimit) {
                log.warn "Bitfinex ETH balance: " + bitfinexETHBalance
                return
            }

            def binanceUSDTBalance = balanceBinanceUSD
            def binanceUSDTLimit = diffEth

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotEth) + "ETH"
            log.info "ETHUSD:" + ETHUSD.bid + "; ETHUSDT:" + ETHUSDT.ask
            log.info "--------------------------------------------------"

            balanceBitfinexETH = balanceBitfinexETH - lotEth
            balanceBitfinexUSD = balanceBitfinexUSD + (lotEth * ETHUSD.bid)

            log.info "--------------------------------------------------"
            log.info "Bitfinex ETH: " + balanceBitfinexETH
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- ETHUSD SELL RESULT ----------------"

            balanceBinanceUSD = balanceBinanceUSD - (lotEth * ETHUSDT.ask)
            balanceBinanceETH = balanceBinanceETH + lotEth

            log.info "--------------------------------------------------"
            log.info "Binance ETH: " + balanceBinanceETH
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- ETHUSDT BUY RESULT -----------------"
        }
    }
}
