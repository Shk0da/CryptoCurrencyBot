package ai.trading.bot.repository;

import ai.trading.bot.domain.Candle;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BasicCandleRepository implements CandleRepository {

    @Getter
    @Value("${candle.repository.limit:21}")
    private Integer limit;

    private final Map<String, TreeMap<Date, Candle>> candles = Maps.newHashMap();

    @Override
    public List<Candle> getCandles(String symbol) {
        return Lists.newArrayList(this.candles.getOrDefault(symbol, Maps.newTreeMap()).values());
    }

    @Override
    @Synchronized
    public void clearAllCandles() {
        this.candles.clear();
    }

    @Override
    @Synchronized
    public void clearCandles(String symbol) {
        this.candles.put(symbol, Maps.newTreeMap());
    }

    @Override
    @Synchronized
    public void addCandles(String symbol, List<Candle> candles) {
        List<Candle> current = getCandles(symbol);
        current.addAll(candles);
        this.candles.put(symbol, getMapFromList(current));
    }

    @Override
    @Synchronized
    public void addCandle(Candle candle) {
        List<Candle> current = getCandles(candle.getSymbol());
        current.add(candle);
        this.candles.put(candle.getSymbol(), getMapFromList(current));
    }

    @Override
    @Synchronized
    public Candle getLastCandle(String symbol) {
        List<Candle> current = getCandles(symbol);
        return current.isEmpty() ? null : current.get(current.size() - 1);
    }

    @Override
    @Synchronized
    public List<Candle> getLastCandles(String symbol, Integer size) {
        List<Candle> current = getCandles(symbol);

        if (current.isEmpty()) {
            return current;
        }

        if (size > current.size()) {
            size = current.size() - 1;
        }

        int fromIndex = current.size() - size;
        if (fromIndex < 0) {
            return current;
        }

        return current.subList(fromIndex, current.size());
    }

    @Override
    public Integer getSize(String symbol) {
        return getCandles(symbol).size();
    }

    private TreeMap<Date, Candle> getMapFromList(List<Candle> current) {
        if (current.size() > limit * 1.2) {
            current = current.subList(current.size() - limit, current.size());
        }
        return current.stream()
                .filter(candle -> candle.getPrice() > 0)
                .collect(Collectors.toMap(Candle::getDateTime, item -> item, (a, b) -> b, TreeMap::new));
    }
}
