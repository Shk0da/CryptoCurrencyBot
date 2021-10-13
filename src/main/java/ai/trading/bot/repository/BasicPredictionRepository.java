package ai.trading.bot.repository;

import ai.trading.bot.domain.Signal;
import com.google.common.collect.Maps;
import lombok.Synchronized;

import java.util.Map;

public class BasicPredictionRepository implements PredictionRepository {

    private final Map<String, Signal> predicts = Maps.newConcurrentMap();

    public String getPredict(String symbol) {
        return predicts.getOrDefault(symbol, Signal.NONE).name();
    }

    @Synchronized
    public void addPredict(String symbol, Signal predict) {
        predicts.put(symbol, predict);
    }
}
