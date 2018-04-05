package ai.trading.bot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HistoryOrder {
    private Long id;
    private String symbol;
    private String side;
    private String type;
    private Double amount;
    private Double price;
    private Long timestamp;
}
