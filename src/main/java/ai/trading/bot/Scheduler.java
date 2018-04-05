package ai.trading.bot;

import ai.trading.bot.actor.Messages;
import ai.trading.bot.config.ActorConfig;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.StockMarket;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@EnableScheduling
@Component("scheduler")
public class Scheduler {

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Scheduled(cron = "${scheduler.candle-collect.cron}")
    public void fireCollect() {
        if (actorSystem == null) return;

        instrumentRepository.getBinanceSymbols().forEach(symbol -> actorSystem
                .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "CollectorActor_" + StockMarket.Binance + "_" + symbol)
                .tell(Messages.Collect, actorSystem.guardian())
        );

        instrumentRepository.getBitfinexSymbols().forEach(symbol -> actorSystem
                .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "CollectorActor_" + StockMarket.BitFinex + "_" + symbol)
                .tell(Messages.Collect, actorSystem.guardian())
        );
    }

    @CacheEvict(allEntries = true, cacheNames = {"status", "ordersActive", "ordersHistory"})
    @Scheduled(fixedDelay = 61000)
    public void cacheEvict() {
        log.debug("CacheEvict for {\"status\", \"ordersActive\", \"ordersHistory\"}");
    }
}
