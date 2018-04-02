package ai.trading.bot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.joda.time.DateTime;

@Getter
@Builder
@ToString
public class Candle {
    private String symbol;
    private Double price;
    private Double bid;
    private Double ask;
    private DateTime dateTime;
}
