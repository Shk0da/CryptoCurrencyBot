package ai.trading.bot.service.binance;

import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.repository.PredictionRepository;
import ai.trading.bot.service.BasicLearnService;
import ai.trading.bot.service.StockMarket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("binanceLearnService")
public class BinanceLearnService extends BasicLearnService {

    @Autowired
    @Qualifier("binanceCandleRepository")
    private CandleRepository candleRepository;

    @Autowired
    @Qualifier("binancePredictionRepository")
    private PredictionRepository predictionRepository;

    @Override
    protected StockMarket stockMarket() {
        return StockMarket.Binance;
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
