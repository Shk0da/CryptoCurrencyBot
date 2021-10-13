package ai.trading.bot.service;

import ai.trading.bot.domain.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LearnService {

    void addCandles(String symbol, List<Candle> candles);

    String getPredict(String symbol);
}
