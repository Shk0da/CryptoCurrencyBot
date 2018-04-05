package ai.trading.bot.domain;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Order {

    public enum Side {SELL, BUY}

    public enum Type {LIMIT, MARKET, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT, LIMIT_MAKER}

    private String symbol;
    private Side side;
    private Type type;
    private Double quantity;
    private Double price;

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("symbol", symbol.toLowerCase());
        jsonObject.addProperty("amount", String.format("%f", quantity));
        jsonObject.addProperty("price", String.format("%f", price != null ? price : 0.01));
        jsonObject.addProperty("side", side.name().toLowerCase());
        jsonObject.addProperty("type", type.name().toLowerCase());

        return jsonObject;
    }

    public String toQuery() {
        String query = "symbol=%s&side=%s&type=%s&quantity=%.7f&";
        if (price != null && price > 0) {
            query += "price=" + Double.toString(price) + "&";
        }
        return String.format(query, symbol, side.name(), type.name(), quantity);
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }
}
