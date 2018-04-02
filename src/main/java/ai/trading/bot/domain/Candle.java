package ai.trading.bot.domain;

import org.joda.time.DateTime;

public interface Candle {
    DateTime getDateTime();
    double getClose();
    double getBid();
    double getAsk();
    int getValue();
}
