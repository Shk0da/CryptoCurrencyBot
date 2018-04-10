package ai.trading.bot.repository;

public interface BalanceRepository {
    void setBalance(String symbol, double value);
    double getBalance(String symbol);
    void setLimit(String symbol, double value);
    double getLimit(String symbol);
}
