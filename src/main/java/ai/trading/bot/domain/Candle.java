package ai.trading.bot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@Getter
@Builder
@ToString
public class Candle {
    private String symbol;
    private Double price;
    private Double bid;
    private Double ask;
    private Date dateTime;
}
