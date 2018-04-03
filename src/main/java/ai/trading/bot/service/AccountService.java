package ai.trading.bot.service;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.Order;
import ai.trading.bot.repository.CandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AccountService {
    Boolean getTradeIsAllowed();
    CandleRepository candleRepository();
    List<Candle> getCandles(String symbol);
    Object createOrder(Order order);
    Object cancelOrder(String symbol, Long orderId);
    Object getInfo();
}
