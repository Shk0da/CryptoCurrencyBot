package ai.trading.bot.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Wallet {
    private String name;
    private Double free;
    private Double locked;
}
