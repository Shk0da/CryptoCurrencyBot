package ai.trading.bot.controller;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.service.AccountService;
import ai.trading.bot.service.StockMarket;
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
    @Qualifier("binanceCandleRepository")
    private CandleRepository binanceCandleRepository;

    @Autowired
    @Qualifier("bitfinexCandleRepository")
    private CandleRepository bitfinexCandleRepository;

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
                result = bitfinexCandleRepository.getLastCandles(symbol, limit);
                break;
            case Binance:
                result = binanceCandleRepository.getLastCandles(symbol, limit);
                break;
            default:
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
