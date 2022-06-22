package ai.trading.bot.service.bitfinex;

import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.repository.PredictionRepository;
import ai.trading.bot.service.BasicLearnService;
import ai.trading.bot.service.StockMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service("bitfinexLearnService")
@ConditionalOnExpression("${predictor.learn.enabled:false}")
public class BitfinexLearnService extends BasicLearnService {

    @Autowired
    @Qualifier("bitfinexCandleRepository")
    private CandleRepository candleRepository;

    @Autowired
    @Qualifier("bitfinexPredictionRepository")
    private PredictionRepository predictionRepository;

    @Override
    protected StockMarket stockMarket() {
        return StockMarket.BitFinex;
    }

    @Override
    public CandleRepository candleRepository() {
        return candleRepository;
    }

    @Override
    public PredictionRepository predictionRepository() {
        return predictionRepository;
    }
}
