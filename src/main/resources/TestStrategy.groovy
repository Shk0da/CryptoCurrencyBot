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

    double diffPercent = 0.3 // in %

    double diffBtc = 150 // in USD
    double lotBtc = 0.002 // in BTC :: BitFinex minimum size for BTC/USD is 0.0002

    double diffEth = 50 // in USD
    double lotEth = 0.01 // in ETH :: BitFinex minimum size for ETH/USD is 0.006

    @Autowired
    @Qualifier("binanceAccountService")
    AccountService binanceAccountService

    @Autowired
    @Qualifier("bitfinexAccountService")
    AccountService bitfinexAccountService

    def lastUpdateBtcUsd
    def lastUpdateEthUsd
    def log = LoggerFactory.getLogger(Strategy.class)

    def binanceComission = 0.2 // in %
    def balanceBinanceUSD = 250
    def balanceBinanceBTC = 0.0
    def balanceBinanceETH = 0.0

    def bitfinexComission = 0.3 // in %
    def balanceBitfinexUSD = 0.0
    def balanceBitfinexBTC = 0.005
    def balanceBitfinexETH = 0.0

    Strategy() {
        log.info "Strategy loaded successful!"
        log.info String.format("With param: diffBtc = %.2fUSD, lotBtc = %fBTC", diffBtc, lotBtc)
        log.info String.format("With param: diffEth = %.2fUSD, lotEth = %fETH", diffEth, lotEth)
    }

    /**
     * Этот метод выполняется раз в fixedDelay милисекунд.
     */
    @Scheduled(fixedDelay = 10000L)
    void run() {
        BTCUSDandBTCUSDT()
        ETHUSDandETHUSDT()
    }

    @Async
    def BTCUSDandBTCUSDT() {
        def BTCUSDT = binanceAccountService.candleRepository().getLastCandle("BTCUSDT")
        def BTCUSD = bitfinexAccountService.candleRepository().getLastCandle("BTCUSD")
        if (BTCUSDT == null || BTCUSD == null) return

        // время пары свечей
        def candleDateTime = BTCUSDT.dateTime.isAfter(BTCUSD.dateTime) ? BTCUSDT.dateTime : BTCUSD.dateTime
        if (candleDateTime == lastUpdateBtcUsd) return else lastUpdateBtcUsd = candleDateTime

        def realDiffBtc = (((BTCUSDT.bid + BTCUSD.ask) / 2) / 100) * diffPercent
        realDiffBtc = realDiffBtc > diffBtc ? realDiffBtc : diffBtc

        // Если {цена продажи} BTCUSDT больше {цена покупки} BTCUSD чем {diff}
        if (BTCUSDT.bid - BTCUSD.ask >= realDiffBtc) {

            // проверка покупательной способности wallet
            def binanceBTCBalance = balanceBinanceBTC
            def binanceBTCLimit = lotBtc

            if (binanceBTCBalance <= binanceBTCLimit) {
                // log.warn "Binance BTC balance: " + binanceBTCBalance
                return
            }

            def bitfinexUSDBalance = balanceBitfinexUSD
            def bitfinexUSDLimit = realDiffBtc

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                // log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSDT.bid - BTCUSD.ask) + "USD"
            log.info "--------------------------------------------------"

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotBtc) + "BTC"
            log.info "BTCUSDT:" + BTCUSDT.bid + "; BTCUSD:" + BTCUSD.ask
            log.info "--------------------------------------------------"

            balanceBinanceBTC = balanceBinanceBTC - lotBtc
            balanceBinanceUSD = balanceBinanceUSD + (lotBtc * BTCUSDT.bid)
            balanceBinanceUSD = balanceBinanceUSD - (lotBtc * BTCUSDT.bid) * binanceComission

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT SELL RESULT ----------------"

            balanceBitfinexUSD = balanceBitfinexUSD - (lotBtc * BTCUSD.ask)
            balanceBitfinexUSD = balanceBitfinexUSD - (lotBtc * BTCUSDT.ask) * bitfinexComission
            balanceBitfinexBTC = balanceBitfinexBTC + lotBtc

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD BUY RESULT -----------------"
            log.info ""
        }

        // Если {цена продажи} BTCUSD больше {цена покупки} BTCUSDT чем {diff}
        if (BTCUSD.bid - BTCUSDT.ask >= realDiffBtc) {

            // проверка покупательной способности wallet
            def bitfinexBTCBalance = balanceBitfinexBTC
            def bitfinexBTCLimit = lotBtc

            if (bitfinexBTCBalance <= bitfinexBTCLimit) {
                // log.warn "Bitfinex BTC balance: " + bitfinexBTCBalance
                return
            }

            def binanceUSDTBalance = balanceBinanceUSD
            def binanceUSDTLimit = realDiffBtc

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                // log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", BTCUSD.bid - BTCUSDT.ask) + "USD"
            log.info "--------------------------------------------------"

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotBtc) + "BTC"
            log.info "BTCUSD:" + BTCUSD.bid + "; BTCUSDT:" + BTCUSDT.ask
            log.info "--------------------------------------------------"

            balanceBitfinexBTC = balanceBitfinexBTC - lotBtc
            balanceBitfinexUSD = balanceBitfinexUSD + (lotBtc * BTCUSD.bid)
            balanceBitfinexUSD = balanceBitfinexUSD - (lotBtc * BTCUSDT.bid) * bitfinexComission

            log.info "--------------------------------------------------"
            log.info "Bitfinex BTC: " + balanceBitfinexBTC
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- BTCUSD SELL RESULT ----------------"

            balanceBinanceUSD = balanceBinanceUSD - (lotBtc * BTCUSDT.ask)
            balanceBinanceUSD = balanceBinanceUSD - (lotBtc * BTCUSDT.ask) * binanceComission
            balanceBinanceBTC = balanceBinanceBTC + lotBtc

            log.info "--------------------------------------------------"
            log.info "Binance BTC: " + balanceBinanceBTC
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- BTCUSDT BUY RESULT -----------------"
            log.info ""
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

        def realDiffEth = (((ETHUSDT.bid + ETHUSD.ask) / 2) / 100) * diffPercent
        realDiffEth = realDiffEth > diffEth ? realDiffEth : diffEth

        // Если {цена продажи} ETHUSDT больше {цена покупки} ETHUSD чем {diff}
        if (ETHUSDT.bid - ETHUSD.ask >= realDiffEth) {

            // проверка покупательной способности wallet
            def binanceETHBalance = balanceBinanceETH
            def binanceETHLimit = lotEth

            if (binanceETHBalance <= binanceETHLimit) {
                // log.warn "Binance ETH balance: " + binanceETHBalance
                return
            }

            def bitfinexUSDBalance = balanceBitfinexUSD
            def bitfinexUSDLimit = realDiffEth

            if (bitfinexUSDBalance <= bitfinexUSDLimit) {
                // log.warn "Bitfinex USD balance: " + bitfinexUSDBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", ETHUSDT.bid - ETHUSD.ask) + "USD"
            log.info "--------------------------------------------------"

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotEth) + "ETH"
            log.info "ETHUSDT:" + ETHUSDT.bid + "; ETHUSD:" + ETHUSD.ask
            log.info "--------------------------------------------------"

            balanceBinanceETH = balanceBinanceETH - lotEth
            balanceBinanceUSD = balanceBinanceUSD + (lotEth * ETHUSDT.bid)
            balanceBinanceUSD = balanceBinanceUSD - (lotEth * ETHUSDT.bid) * binanceComission

            log.info "--------------------------------------------------"
            log.info "Binance ETH: " + balanceBinanceETH
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- ETHUSDT SELL RESULT ----------------"

            balanceBitfinexUSD = balanceBitfinexUSD - (lotEth * ETHUSD.ask)
            balanceBitfinexUSD = balanceBitfinexUSD - (lotEth * ETHUSDT.ask) * bitfinexComission
            balanceBitfinexETH = balanceBitfinexETH + lotEth

            log.info "--------------------------------------------------"
            log.info "Bitfinex ETH: " + balanceBitfinexETH
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- ETHUSD BUY RESULT -----------------"
            log.info ""
        }

        // Если {цена продажи} ETHUSD больше {цена покупки} ETHUSDT чем {diff}
        if (ETHUSD.bid - ETHUSDT.ask >= realDiffEth) {

            // проверка покупательной способности wallet
            def bitfinexETHBalance = balanceBitfinexETH
            def bitfinexETHLimit = lotEth

            if (bitfinexETHBalance <= bitfinexETHLimit) {
                // log.warn "Bitfinex ETH balance: " + bitfinexETHBalance
                return
            }

            def binanceUSDTBalance = balanceBinanceUSD
            def binanceUSDTLimit = realDiffEth

            if (binanceUSDTBalance <= binanceUSDTLimit) {
                // log.warn "Binance USDT balance: " + binanceUSDTBalance
                return
            }

            log.info "--------------------------------------------------"
            log.info "We have new DIFF: " + String.format("%.2f", ETHUSD.bid - ETHUSDT.ask) + "USD"
            log.info "--------------------------------------------------"

            log.info "--------------------------------------------------"
            log.info "Do arbitrage: " + String.format("%f", lotEth) + "ETH"
            log.info "ETHUSD:" + ETHUSD.bid + "; ETHUSDT:" + ETHUSDT.ask
            log.info "--------------------------------------------------"

            balanceBitfinexETH = balanceBitfinexETH - lotEth
            balanceBitfinexUSD = balanceBitfinexUSD + (lotEth * ETHUSD.bid)
            balanceBitfinexUSD = balanceBitfinexUSD - (lotEth * ETHUSDT.bid) * bitfinexComission

            log.info "--------------------------------------------------"
            log.info "Bitfinex ETH: " + balanceBitfinexETH
            log.info "Bitfinex USD: " + balanceBitfinexUSD
            log.info "-------------- ETHUSD SELL RESULT ----------------"

            balanceBinanceUSD = balanceBinanceUSD - (lotEth * ETHUSDT.ask)
            balanceBinanceUSD = balanceBinanceUSD - (lotEth * ETHUSDT.ask) * binanceComission
            balanceBinanceETH = balanceBinanceETH + lotEth

            log.info "--------------------------------------------------"
            log.info "Binance ETH: " + balanceBinanceETH
            log.info "Binance USD: " + balanceBinanceUSD
            log.info "------------- ETHUSDT BUY RESULT -----------------"
            log.info ""
        }
    }
}
