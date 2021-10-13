package ai.trading.bot.repository;

import ai.trading.bot.service.StockMarket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InstrumentRepository {

    @Value("#{'${binance.symbols}'.split(';')}")
    private List<String> binanceSymbols;

    @Value("#{'${bitfinex.symbols}'.split(';')}")
    private List<String> bitfinexSymbols;

    public List<String> getSymbolsByMarket(StockMarket stockMarket) {
        switch (stockMarket) {
            case Binance: return binanceSymbols;
            case BitFinex: return bitfinexSymbols;
            default: return new ArrayList<>();
        }
    }
}
