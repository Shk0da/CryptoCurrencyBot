package ai.trading.bot.controller;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.AccountService;
import ai.trading.bot.service.StockMarket;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    @Autowired
    private InstrumentRepository instrumentRepository;

    @GetMapping(value = "/markets")
    public ResponseEntity<List<String>> markets() {
        return new ResponseEntity<>(Lists.newArrayList(StockMarket.values())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping(value = "/trading", params = {"active"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> trading(Boolean active) {
        try {
            binanceAccountService.setTradeIsAllowed(active);
            bitfinexAccountService.setTradeIsAllowed(active);
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String result = (binanceAccountService.getTradeIsAllowed() && bitfinexAccountService.getTradeIsAllowed())
                ? "on"
                : "off";

        log.debug("trading: {}", result);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(path = "/logging", produces = MediaType.TEXT_PLAIN_VALUE)
    public String logging() {
        try {
            return FileUtils.readFileToString(new File("./logs/bot.log"));
        } catch (IOException ex) {
            return "Failed open log file.";
        }
    }

    @GetMapping(value = "/status", params = {"market"})
    public ResponseEntity<Object> status(String market) {
        switch (StockMarket.valueOf(market)) {
            case BitFinex:
                return new ResponseEntity<>(bitfinexAccountService.getInfo(), HttpStatus.OK);
            case Binance:
                return new ResponseEntity<>(binanceAccountService.getInfo(), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = "/symbols", params = {"market"})
    public ResponseEntity<List<String>> symbols(String market) {
        switch (StockMarket.valueOf(market)) {
            case BitFinex:
                return new ResponseEntity<>(instrumentRepository.getBitfinexSymbols(), HttpStatus.OK);
            case Binance:
                return new ResponseEntity<>(instrumentRepository.getBinanceSymbols(), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
