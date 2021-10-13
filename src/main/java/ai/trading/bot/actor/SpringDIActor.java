package ai.trading.bot.actor;

import ai.trading.bot.provider.ApplicationContextProvider;
import ai.trading.bot.service.StockMarket;
import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpringDIActor implements IndirectActorProducer {

    private Actor actorInstance;
    private final Class<? extends Actor> type;
    private final String instrument;
    private final StockMarket market;

    public SpringDIActor(Class<? extends Actor> type, StockMarket market, String instrument) {
        this.type = type;
        this.market = market;
        this.instrument = instrument;
    }

    @Override
    public Class<? extends Actor> actorClass() {
        return type;
    }

    @Override
    public Actor produce() {
        Actor newActor = actorInstance;
        actorInstance = null;
        if (newActor == null) {
            try {
                newActor = type.getConstructor(StockMarket.class, String.class).newInstance(market, instrument);
            } catch (Exception ex) {
                log.error("Unable to create actor of type: {}({}, {}). Because {}.", type, market, instrument, ex.getMessage());
                return null;
            }
        }

        ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(newActor);
        return newActor;
    }
}
