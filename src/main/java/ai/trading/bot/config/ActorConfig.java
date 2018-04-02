package ai.trading.bot.config;

import ai.trading.bot.actor.CollectorActor;
import ai.trading.bot.actor.SpringDIActor;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.StockMarket;
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

    @Bean(name = "actorSystem")
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("aitradinbot");
        try {
            instrumentRepository.getBinanceSymbols().forEach(symbol -> {
                system.actorOf(Props.create(SpringDIActor.class, CollectorActor.class, StockMarket.Binance, symbol), "CollectorActor_" + StockMarket.Binance + "_" + symbol);
                log.info("Create: CollectorActor_" + StockMarket.Binance + "_" + symbol);
            });

            instrumentRepository.getBitfinexSymbols().forEach(symbol -> {
                system.actorOf(Props.create(SpringDIActor.class, CollectorActor.class, StockMarket.BitFinex, symbol), "CollectorActor_" + StockMarket.BitFinex + "_" + symbol);
                log.info("Create: CollectorActor_" + StockMarket.BitFinex + "_" + symbol);
            });
        } catch (InvalidActorNameException ex) {
            log.error(ex.getMessage());
        }

        return system;
    }
}
