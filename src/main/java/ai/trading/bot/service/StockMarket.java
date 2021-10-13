package ai.trading.bot.service;

import jdk.internal.jline.internal.Nullable;

public enum StockMarket {

    Binance, BitFinex;

    @Nullable
    public static StockMarket of(String name) {
        try {
            return StockMarket.valueOf(name);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.name();
    }
}
