package ai.trading.bot.service;

import ai.trading.bot.domain.*;
import ai.trading.bot.repository.BalanceRepository;
import ai.trading.bot.repository.CandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AccountService {

    Boolean getTradeIsAllowed();

    void setTradeIsAllowed(Boolean val);

    CandleRepository candleRepository();

    BalanceRepository balanceRepository();

    List<Candle> getCandles(String symbol);

    Object createOrder(Order order);

    Object cancelOrder(String symbol, Long orderId);

    List<Wallet> getInfo();

    List<ActiveOrder> getActiveOrders(int limit);

    List<HistoryOrder> getHistoryOrders(int limit);
}
