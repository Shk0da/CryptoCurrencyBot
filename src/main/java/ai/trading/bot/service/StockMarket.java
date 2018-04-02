package ai.trading.bot.service;

public enum StockMarket {
    Binance, BitFinex;

    @Override
    public String toString() {
        return this.name();
    }
}
