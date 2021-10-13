package ai.trading.bot.config;

import ai.trading.bot.actor.CollectorActor;
import ai.trading.bot.actor.SpringDIActor;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.StockMarket;
import ai.trading.bot.service.StockMarketKeeperService;
import akka.actor.ActorSystem;
import akka.actor.InvalidActorNameException;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ActorConfig {

    public static final String ACTOR_PATH_HEAD = "akka://aitradinbot/user/";

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private StockMarketKeeperService stockMarketKeeperService;

    @Bean(name = "actorSystem")
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("aitradinbot");
        try {
            for (StockMarket stockMarket : StockMarket.values()) {
                if (!stockMarketKeeperService.isEnabled(stockMarket)) continue;

                instrumentRepository.getSymbolsByMarket(stockMarket).forEach(symbol -> {
                    system.actorOf(Props.create(SpringDIActor.class, CollectorActor.class, stockMarket, symbol), "CollectorActor_" + stockMarket + "_" + symbol);
                    log.info("Create: CollectorActor_" + stockMarket + "_" + symbol);
                });
            }
        } catch (InvalidActorNameException ex) {
            log.error(ex.getMessage());
        }

        return system;
    }
}
