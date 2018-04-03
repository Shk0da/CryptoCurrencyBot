package ai.trading.bot.service;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.Order;
import ai.trading.bot.repository.CandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface AccountService {
    CandleRepository candleRepository();
    List<Candle> getCandles(String symbol);
    Map<String, Object> createOrder(Order order);
    Map<String, Object> cancelOrder(String symbol, Long orderId);
    Object getInfo();
}
