package ai.trading.bot.controller;

import ai.trading.bot.domain.ActiveOrder;
import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.HistoryOrder;
import ai.trading.bot.domain.Wallet;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.AccountService;
import ai.trading.bot.service.StockMarket;
import ai.trading.bot.service.StockMarketKeeperService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private StockMarketKeeperService stockMarketKeeperService;

    @GetMapping(value = "/markets")
    public ResponseEntity<List<String>> markets() {
        return new ResponseEntity<>(Lists.newArrayList(StockMarket.values())
                .stream()
                .filter(market -> stockMarketKeeperService.isEnabled(market))
                .map(Enum::name)
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping(value = "/trading", params = {"active"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> trading(Boolean active) {
        boolean isAllAllowed = true;
        try {
            for (StockMarket market : StockMarket.values()) {
                if (!stockMarketKeeperService.isEnabled(market)) continue;

                AccountService accountService = stockMarketKeeperService.accountServiceByMarket(market);
                accountService.setTradeIsAllowed(active);
                isAllAllowed = isAllAllowed && accountService.getTradeIsAllowed();
            }
        } catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String result = isAllAllowed ? "on" : "off";
        log.debug("trading: {}", result);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(value = "/trading/status", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> tradingStatus() {
        boolean isAllAllowed = true;
        for (StockMarket market : StockMarket.values()) {
            if (!stockMarketKeeperService.isEnabled(market)) continue;

            AccountService accountService = stockMarketKeeperService.accountServiceByMarket(market);
            isAllAllowed = isAllAllowed && accountService.getTradeIsAllowed();
        }
        String result = isAllAllowed ? "on" : "off";
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(path = "/logging", produces = MediaType.TEXT_PLAIN_VALUE)
    public String logging() {
        try {
            List<String> logs = FileUtils.readLines(new File("./logs/bot.log"));
            Collections.reverse(logs);
            return String.join("\n", logs.subList(0, logs.size() < 100 ? logs.size() : 100));
        } catch (IOException ex) {
            return "Failed open log file.";
        }
    }

    @Cacheable("status")
    @GetMapping(value = "/status", params = {"market"})
    public ResponseEntity<List<Wallet>> status(String market) {
        StockMarket stockMarket = StockMarket.of(market);
        if (null == stockMarket) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        AccountService accountService = stockMarketKeeperService.accountServiceByMarket(stockMarket);
        return new ResponseEntity<>(accountService.getInfo(), HttpStatus.OK);
    }

    @GetMapping(value = "/symbols", params = {"market"})
    public ResponseEntity<List<String>> symbols(String market) {
        StockMarket stockMarket = StockMarket.of(market);
        if (null == stockMarket || !stockMarketKeeperService.isEnabled(stockMarket)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(instrumentRepository.getSymbolsByMarket(stockMarket), HttpStatus.OK);
    }

    @GetMapping(value = "/candles", params = {"market", "symbol", "limit"})
    public ResponseEntity<List<Candle>> candles(String market, String symbol, Integer limit) {
        StockMarket stockMarket = StockMarket.of(market);
        if (null == stockMarket || !stockMarketKeeperService.isEnabled(stockMarket)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AccountService accountService = stockMarketKeeperService.accountServiceByMarket(stockMarket);
        List<Candle> result = accountService.candleRepository().getLastCandles(symbol, limit);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Cacheable("ordersActive")
    @GetMapping(value = "/orders/active", params = {"market", "limit"})
    public ResponseEntity<List<ActiveOrder>> ordersActive(String market, int limit) {
        StockMarket stockMarket = StockMarket.of(market);
        if (null == stockMarket || !stockMarketKeeperService.isEnabled(stockMarket)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AccountService accountService = stockMarketKeeperService.accountServiceByMarket(stockMarket);
        return new ResponseEntity<>(accountService.getActiveOrders(limit), HttpStatus.OK);
    }

    @Cacheable("ordersHistory")
    @GetMapping(value = "/orders/history", params = {"market", "limit"})
    public ResponseEntity<List<HistoryOrder>> ordersHistory(String market, int limit) {
        StockMarket stockMarket = StockMarket.of(market);
        if (null == stockMarket || !stockMarketKeeperService.isEnabled(stockMarket)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AccountService accountService = stockMarketKeeperService.accountServiceByMarket(stockMarket);
        return new ResponseEntity<>(accountService.getHistoryOrders(limit), HttpStatus.OK);
    }
}
