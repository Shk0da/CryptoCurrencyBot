package ai.trading.bot.service;

import ai.trading.bot.actor.LearnActor;
import ai.trading.bot.actor.Messages;
import ai.trading.bot.actor.SpringDIActor;
import ai.trading.bot.domain.Candle;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.repository.PredictionRepository;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.common.collect.Maps;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BasicLearnService implements LearnService {

    private final ActorSystem actorSystem = ActorSystem.create("LearnSystem");
    private final Map<String, ActorRef> actors = Maps.newConcurrentMap();

    protected abstract StockMarket stockMarket();

    protected abstract CandleRepository candleRepository();

    protected abstract PredictionRepository predictionRepository();

    @Async
    @Synchronized
    public void addCandles(String symbol, List<Candle> candles) {
        ActorRef actor = actors.getOrDefault(stockMarket() + "_" + symbol, null);
        if (actor == null) {
            actor = actorSystem.actorOf(Props.create(SpringDIActor.class, LearnActor.class, stockMarket(), symbol), "LearnActor_" + stockMarket() + "_" + symbol);
            actors.put(stockMarket() + "_" + symbol, actor);
        }

        if (!candles.isEmpty()) {
            actor.tell(Messages.LEARN, actorSystem.guardian());
        }

        log.debug("Candle list size {}: added {}, total: {}", symbol, candles.size(), candleRepository().getSize(symbol));
    }

    public String getPredict(String symbol) {
        ActorRef actor = actors.getOrDefault(stockMarket() + "_" + symbol, null);
        if (actor != null) {
            actor.tell(Messages.PREDICT, actorSystem.guardian());
        }
        return predictionRepository().getPredict(symbol);
    }
}
