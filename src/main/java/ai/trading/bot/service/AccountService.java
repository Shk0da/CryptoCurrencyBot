package ai.trading.bot.service;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.HistoryOrder;
import ai.trading.bot.domain.Order;
import ai.trading.bot.domain.Wallet;
import ai.trading.bot.repository.CandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AccountService {

    Boolean getTradeIsAllowed();

    void setTradeIsAllowed(Boolean val);

    CandleRepository candleRepository();

    List<Candle> getCandles(String symbol);

    Object createOrder(Order order);

    Object cancelOrder(String symbol, Long orderId);

    List<Wallet> getInfo();

    List<Object> getActiveOrders(int limit);

    List<HistoryOrder> getHistoryOrders(int limit);
}
