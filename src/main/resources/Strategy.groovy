import ai.trading.bot.domain.Order
import ai.trading.bot.service.AccountService
import ai.trading.bot.service.LearnService
import ai.trading.bot.service.StrategyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled

class Strategy implements StrategyService {

    double lot = 0.002 // in BTC :: BitFinex minimum size for BTC/USD is 0.002

    @Autowired
    @Qualifier("binanceAccountService")
    AccountService binanceAccountService

    @Autowired
    @Qualifier("binanceLearnService")
    LearnService learnService

    def isTest = true
    def lastUpdateBtcEth
    def lastPredictBtcEth = "NONE"
    def log = LoggerFactory.getLogger(Strategy.class)

    Strategy() {
        log.info String.format("Strategy loaded successful! With param: lot = %fBTC", lot)
    }

    @Scheduled(fixedDelay = 10000L)
    void run() {
        BTCETH()
    }

    @Async
    def BTCETH() {
        def symbol = "BTCETH"
        def binanceBTCETH = binanceAccountService.candleRepository().getLastCandle(symbol)
        if (binanceBTCETH == null) return

        // время пары свечей
        def candleDateTime = binanceBTCETH.dateTime
        if (candleDateTime == lastUpdateBtcEth) return else lastUpdateBtcEth = candleDateTime

        def predict = learnService.getPredict(symbol)
        log.warn "Predict $symbol: " + predict

        if (lastPredictBtcEth != predict) {
            lastPredictBtcEth = predict
            log.info "--------------------------------------------------"
            log.info "We have new predict $symbol: $predict"
            log.info "--------------------------------------------------"

            def binanceBTCBalance = binanceAccountService.balanceRepository().getBalance("BTC")
            log.warn "BinanceBTCBalance: " + binanceBTCBalance
            def binanceETHBalance = binanceAccountService.balanceRepository().getBalance("ETH")
            log.warn "BinanceETHBalance: " + binanceETHBalance

            if (predict == "DOWN") {
                log.info "BinanceBTCETH: ${binanceBTCETH.bid}"
                if (isTest) return

                def sellResult = binanceAccountService.createOrder(Order.builder()
                        .symbol("binanceBTCETH")
                        .side(Order.Side.SELL)
                        .type(Order.Type.MARKET)
                        .quantity(lot)
                        .build())

                log.info "------------- binanceBTCETH SELL RESULT ----------------"
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
            } else if (predict == "UP") {
                log.info "BinanceBTCETH: ${binanceBTCETH.ask}"
                if (isTest) return

                def buyResult = binanceAccountService.createOrder(Order.builder()
                        .symbol("binanceBTCETH")
                        .side(Order.Side.BUY)
                        .type(Order.Type.MARKET)
                        .quantity(lot)
                        .build())

                log.info "------------- binanceBTCETH BUY RESULT -----------------"
                def buyOrderId = 0
                if (buyResult == null || buyResult.isEmpty()) {
                    log.info "Something wrong! Break..."
                    log.info "--------------------------------------------------"
                    return
                } else {
                    buyResult.each { key, val ->
                        log.info "${key}: ${val}"
                        if (key == "orderId") buyOrderId = val
                    }
                }
                log.info "--------------------------------------------------"
            }
        }
    }
}
