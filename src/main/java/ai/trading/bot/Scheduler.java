package ai.trading.bot;

import ai.trading.bot.actor.Messages;
import ai.trading.bot.config.ActorConfig;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.StockMarket;
import ai.trading.bot.service.StockMarketKeeperService;
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

    @Autowired
    private StockMarketKeeperService stockMarketKeeperService;

    @Scheduled(cron = "${scheduler.candle-collect.cron}")
    public void fireCollect() {
        if (actorSystem == null) return;

        for (StockMarket market : StockMarket.values()) {
            if (!stockMarketKeeperService.isEnabled(market)) continue;

            instrumentRepository.getSymbolsByMarket(market).forEach(symbol -> actorSystem
                    .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "CollectorActor_" + market + "_" + symbol)
                    .tell(Messages.COLLECT, actorSystem.guardian())
            );
        }
    }

    @CacheEvict(allEntries = true, cacheNames = {"status", "ordersActive", "ordersHistory"})
    @Scheduled(fixedDelay = 300_000)
    public void cacheEvict() {
        log.debug("CacheEvict for {\"status\", \"ordersActive\", \"ordersHistory\"}");
    }
}
