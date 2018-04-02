package ai.trading.bot.service;

import ai.trading.bot.domain.Candle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AccountService {
    List<Candle> getCandles(String symbol);
}
