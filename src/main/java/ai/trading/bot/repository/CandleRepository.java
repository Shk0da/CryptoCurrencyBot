package ai.trading.bot.repository;

import ai.trading.bot.domain.Candle;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandleRepository {

    Integer getLimit();

    List<Candle> getCandles(String symbol);

    void clearAllCandles();

    void clearCandles(String symbol);

    void addCandles(String symbol, List<Candle> candles);

    void addCandle(Candle candle);

    Candle getLastCandle(String symbol);

    List<Candle> getLastCandles(String symbol, Integer size);

    Integer getSize(String symbol);
}
