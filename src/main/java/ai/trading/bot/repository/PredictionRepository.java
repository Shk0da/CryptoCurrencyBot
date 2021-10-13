package ai.trading.bot.repository;

import ai.trading.bot.domain.Signal;
import org.springframework.stereotype.Repository;

@Repository
public interface PredictionRepository {

    String getPredict(String symbol);

    void addPredict(String symbol, Signal predict);
}
