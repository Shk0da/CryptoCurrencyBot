package ai.trading.bot.service.binance;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.service.AccountService;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("binanceAccountService")
public class BinanceAccountService implements AccountService {

    @Override
    public List<Candle> getCandles(String symbol) {
        return Lists.newArrayList();
    }
}
