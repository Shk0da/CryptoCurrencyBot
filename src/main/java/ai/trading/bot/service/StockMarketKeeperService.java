package ai.trading.bot.service;

import jdk.internal.jline.internal.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockMarketKeeperService {

    @Value("#{'${stock-market.enabled}'.split(',')}")
    private List<StockMarket> stockMarketEnabled;

    @Autowired
    @Qualifier("binanceAccountService")
    private AccountService binanceAccountService;

    @Autowired
    @Qualifier("bitfinexAccountService")
    private AccountService bitfinexAccountService;

    public boolean isEnabled(StockMarket stockMarket) {
        return stockMarketEnabled.contains(stockMarket);
    }

    @Nullable
    public AccountService accountServiceByMarket(StockMarket stockMarket) {
        switch (stockMarket) {
            case Binance: return binanceAccountService;
            case BitFinex: return bitfinexAccountService;
            default: return null;
        }
    }
}
