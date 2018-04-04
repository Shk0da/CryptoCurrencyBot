package ai.trading.bot.controller;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.service.AccountService;
import ai.trading.bot.service.StockMarket;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    @Qualifier("binanceAccountService")
    private AccountService binanceAccountService;

    @Autowired
    @Qualifier("bitfinexAccountService")
    private AccountService bitfinexAccountService;

    @GetMapping(value = "/markets")
    public ResponseEntity<List<String>> markets() {
        return new ResponseEntity<>(Lists.newArrayList(StockMarket.values())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping(value = "/trading", params = {"active"})
    public ResponseEntity<String> trading(Boolean active) {
        try {
            binanceAccountService.setTradeIsAllowed(active);
            bitfinexAccountService.setTradeIsAllowed(active);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>("Successful!", HttpStatus.OK);
    }

    @GetMapping(value = "/status")
    public ResponseEntity<Map<StockMarket, Object>> status() {
        Map<StockMarket, Object> status = new HashMap<StockMarket, Object>() {{
            put(StockMarket.Binance, binanceAccountService.getInfo());
            put(StockMarket.BitFinex, bitfinexAccountService.getInfo());
        }};
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @GetMapping(value = "/candles", params = {"market", "symbol", "limit"})
    public ResponseEntity<List<Candle>> candles(String market, String symbol, Integer limit) {
        List<Candle> result;
        switch (StockMarket.valueOf(market)) {
            case BitFinex:
                result = bitfinexAccountService.candleRepository().getLastCandles(symbol, limit);
                break;
            case Binance:
                result = binanceAccountService.candleRepository().getLastCandles(symbol, limit);
                break;
            default:
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(value = "/orders/active", params = {"market", "limit"})
    public ResponseEntity<List<Object>> ordersActive(StockMarket market, int limit) {
        switch (market) {
            case Binance:
                return new ResponseEntity<>(binanceAccountService.getActiveOrders(limit), HttpStatus.OK);
            case BitFinex:
                return new ResponseEntity<>(bitfinexAccountService.getActiveOrders(limit), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/orders/history", params = {"market", "limit"})
    public ResponseEntity<Object> ordersHistory(StockMarket market, int limit) {
        switch (market) {
            case Binance:
                return new ResponseEntity<>(binanceAccountService.getHistoryOrders(limit), HttpStatus.OK);
            case BitFinex:
                return new ResponseEntity<>(bitfinexAccountService.getHistoryOrders(limit), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
