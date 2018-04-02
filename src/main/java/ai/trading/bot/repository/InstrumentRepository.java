package ai.trading.bot.repository;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InstrumentRepository {

    @Getter
    @Value("#{'${binance.symbols}'.split(';')}")
    private List<String> binanceSymbols;

    @Getter
    @Value("#{'${bitfinex.symbols}'.split(';')}")
    private List<String> bitfinexSymbols;
}
