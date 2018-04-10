package ai.trading.bot.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicBalanceRepository implements BalanceRepository {

    private Map<String, Double> balances = new ConcurrentHashMap<>();
    private Map<String, Double> limits = new ConcurrentHashMap<>();

    @Override
    public void setBalance(String symbol, double value) {
        balances.put(symbol, value);
    }

    @Override
    public double getBalance(String symbol) {
        return balances.getOrDefault(symbol, 0D);
    }

    @Override
    public void setLimit(String symbol, double value) {
        limits.put(symbol, value);
    }

    @Override
    public double getLimit(String symbol) {
        return limits.getOrDefault(symbol, 0D);
    }
}
