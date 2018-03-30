package ai.trading.bot.domain;

import org.joda.time.DateTime;

public interface Candle {
    DateTime getDate();
    double getBid();
    double getAsk();
    int getValue();
}
