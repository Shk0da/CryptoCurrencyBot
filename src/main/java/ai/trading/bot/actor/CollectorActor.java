package ai.trading.bot.actor;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.provider.ApplicationContextProvider;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.service.AccountService;
import ai.trading.bot.service.StockMarket;
import akka.actor.UntypedAbstractActor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Scope("prototype")
@Component("collectorActor")
public class CollectorActor extends UntypedAbstractActor {

    private final String instrument;
    private final StockMarket market;
    private CandleRepository candleRepository;
    private AccountService accountService;

    public CollectorActor(StockMarket market, String instrument) {
        this.market = market;
        this.instrument = instrument;
    }

    @Override
    public void preStart() {
        String candleRepositoryName = market.name().toLowerCase() + "CandleRepository";
        candleRepository = (CandleRepository) ApplicationContextProvider
                .getApplicationContext()
                .getBean(candleRepositoryName);
        log.debug("{}::{} candleRepository has been init: {}::{}", market, instrument, candleRepositoryName, candleRepository);

        String accountServiceName = market.name().toLowerCase() + "AccountService";
        accountService = (AccountService) ApplicationContextProvider
                .getApplicationContext()
                .getBean(accountServiceName);
        log.debug("{}::{} accountService has been init: {}::{}", market, instrument, accountServiceName, accountService);
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (Messages.Collect.equals(message)) {
            try {
                List<Candle> candles = accountService.getCandles(instrument)
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                log.debug("{}::{} get {} candles.", market, instrument, candles.size());

                if (!candles.isEmpty()) {
                    candleRepository.addCandles(instrument, candles);
                }
            } catch (Exception ex) {
                log.error("{}: {}", market, ex.getMessage());
            }
        } else {
            unhandled(message);
        }
    }
}
