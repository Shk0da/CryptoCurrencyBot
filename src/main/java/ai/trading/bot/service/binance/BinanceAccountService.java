package ai.trading.bot.service.binance;

import ai.trading.bot.domain.*;
import ai.trading.bot.repository.BalanceRepository;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.repository.InstrumentRepository;
import ai.trading.bot.service.AccountService;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("binanceAccountService")
public class BinanceAccountService implements AccountService {

    public static final String BASE_URL_V1 = "https://www.binance.com/api/v1/";
    public static final String BASE_URL_V3 = "https://www.binance.com/api/v3/";

    @Value("${binance.api.key}")
    private String apiKey;

    @Value("${binance.secret.key}")
    private String secretKey;

    @Autowired
    @Qualifier("binanceCandleRepository")
    private CandleRepository binanceCandleRepository;

    @Autowired
    @Qualifier("binanceBalanceRepository")
    private BalanceRepository balanceRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Setter
    @Getter
    @Value("${binance.trade.allowed:true}")
    private volatile Boolean tradeIsAllowed;

    private final JsonParser jsonParser = new JsonParser();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CandleRepository candleRepository() {
        return binanceCandleRepository;
    }

    @Override
    public BalanceRepository balanceRepository() {
        return balanceRepository;
    }

    @Override
    public List<Candle> getCandles(String symbol) {
        if (!tradeIsAllowed) return Lists.newArrayList();

        Candle candle = null;
        try {
            String jsonPrice = restTemplate
                    .getForEntity(BASE_URL_V3 + "ticker/price?symbol=" + symbol, String.class)
                    .getBody();
            JsonObject responsePrice = (JsonObject) jsonParser.parse(jsonPrice);

            String jsonBookTicker = restTemplate
                    .getForEntity(BASE_URL_V3 + "ticker/bookTicker?symbol=" + symbol, String.class)
                    .getBody();
            JsonObject responseBookTicker = (JsonObject) jsonParser.parse(jsonBookTicker);

            candle = Candle.builder()
                    .symbol(symbol)
                    .price(responsePrice.get("price").getAsDouble())
                    .ask(responseBookTicker.get("askPrice").getAsDouble())
                    .bid(responseBookTicker.get("bidPrice").getAsDouble())
                    .dateTime(DateTime.now())
                    .build();

            log.debug(candle.toString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return Lists.newArrayList(candle);
    }

    @Override
    public Object createOrder(Order order) {
        if (!tradeIsAllowed) return new HashMap<String, String>() {{
            put("tradeIsAllowed", "false");
        }};

        try {
            return sendPostRequest("order", order.toQuery()).getBody();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        }

        return null;
    }

    @Override
    public Object cancelOrder(String symbol, Long orderId) {
        try {
            String query = "symbol=" + symbol + "&";
            if (orderId != null && orderId > 0) {
                query += "orderId=" + orderId + "&";
            }
            return restTemplate
                    .exchange(
                            BASE_URL_V3 + "order?" + query + getSignatureParam(query),
                            HttpMethod.DELETE,
                            new HttpEntity<>(getAuthHeader()),
                            Object.class
                    ).getBody();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        }

        return null;
    }

    private long getServerTime() {
        try {
            String jsonPrice = restTemplate
                    .exchange(BASE_URL_V1 + "time", HttpMethod.GET, null, String.class)
                    .getBody();

            long serverTime = jsonParser.parse(jsonPrice)
                    .getAsJsonObject()
                    .get("serverTime")
                    .getAsLong();

            log.debug("ServerTime: {}", serverTime);
            return serverTime;
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return System.currentTimeMillis();
    }

    @Override
    public List<Wallet> getInfo() {
        try {
            Map<String, Object> accountData = (Map<String, Object>) sendGetRequest("account").getBody();
            JsonArray balances = new GsonBuilder()
                    .create()
                    .toJsonTree(accountData.get("balances"))
                    .getAsJsonArray();

            List<Wallet> wallets = Lists.newArrayList();
            balances.forEach(jsonElement ->
                    wallets.add(Wallet.builder()
                            .name(jsonElement.getAsJsonObject().get("asset").getAsString())
                            .free(jsonElement.getAsJsonObject().get("free").getAsDouble())
                            .locked(jsonElement.getAsJsonObject().get("locked").getAsDouble())
                            .build())
            );

            return wallets.stream()
                    .filter(wallet -> wallet.getFree() > 0 || wallet.getLocked() > 0)
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return Lists.newArrayList();
    }

    @Override
    public List<ActiveOrder> getActiveOrders(int limit) {
        try {
            List<Object> response = (List<Object>) restTemplate
                    .exchange(
                            BASE_URL_V3 + "openOrders?" + getSignatureParam(),
                            HttpMethod.GET,
                            new HttpEntity<>(getAuthHeader()),
                            Object.class
                    ).getBody();

            JsonArray orders = new GsonBuilder()
                    .create()
                    .toJsonTree(response)
                    .getAsJsonArray();

            List<ActiveOrder> result = Lists.newArrayList();
            orders.forEach(jsonElement -> result.add(ActiveOrder.builder()
                    .id(jsonElement.getAsJsonObject().get("orderId").getAsLong())
                    .symbol(jsonElement.getAsJsonObject().get("symbol").getAsString())
                    .side(jsonElement.getAsJsonObject().get("side").getAsString())
                    .type(jsonElement.getAsJsonObject().get("type").getAsString())
                    .amount(jsonElement.getAsJsonObject().get("origQty").getAsDouble())
                    .price(jsonElement.getAsJsonObject().get("price").getAsDouble())
                    .timestamp(jsonElement.getAsJsonObject().get("time").getAsLong())
                    .status(jsonElement.getAsJsonObject().get("status").getAsString())
                    .build()));

            return result.subList(0, result.size() > limit ? limit : result.size())
                    .stream()
                    .sorted((f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()))
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return null;
    }

    @Override
    public List<HistoryOrder> getHistoryOrders(int limit) {
        try {
            List<HistoryOrder> result = Lists.newArrayList();
            instrumentRepository.getBinanceSymbols().forEach(symbol -> {
                String query = "symbol=" + symbol + "&limit=" + limit + "&";
                List<Object> response = (List<Object>) restTemplate
                        .exchange(
                                BASE_URL_V3 + "allOrders?" + query + getSignatureParam(query),
                                HttpMethod.GET,
                                new HttpEntity<>(getAuthHeader()),
                                Object.class
                        ).getBody();

                JsonArray orders = new GsonBuilder()
                        .create()
                        .toJsonTree(response)
                        .getAsJsonArray();

                orders.forEach(jsonElement ->
                        result.add(HistoryOrder.builder()
                                .id(jsonElement.getAsJsonObject().get("orderId").getAsLong())
                                .symbol(jsonElement.getAsJsonObject().get("symbol").getAsString())
                                .side(jsonElement.getAsJsonObject().get("side").getAsString())
                                .type(jsonElement.getAsJsonObject().get("type").getAsString())
                                .amount(jsonElement.getAsJsonObject().get("origQty").getAsDouble())
                                .price(jsonElement.getAsJsonObject().get("price").getAsDouble())
                                .timestamp(jsonElement.getAsJsonObject().get("time").getAsLong())
                                .build())
                );
            });

            return result.subList(0, result.size() > limit ? limit : result.size())
                    .stream()
                    .sorted((f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()))
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        return null;
    }

    private ResponseEntity<Object> sendGetRequest(String uri) {
        return sendGetRequest(uri, "");
    }

    private ResponseEntity<Object> sendGetRequest(String uri, String query) {
        return restTemplate
                .exchange(
                        BASE_URL_V3 + uri + "?" + query + getSignatureParam(query),
                        HttpMethod.GET,
                        new HttpEntity<>(getAuthHeader()),
                        Object.class
                );
    }

    private ResponseEntity<Object> sendPostRequest(String uri, String query) {
        return restTemplate
                .exchange(
                        BASE_URL_V3 + uri + "?" + query + getSignatureParam(query),
                        HttpMethod.POST,
                        new HttpEntity<>(getAuthHeader()),
                        Object.class
                );
    }

    private HttpHeaders getAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("X-MBX-APIKEY", apiKey);

        return headers;
    }

    private String getSignatureParam() {
        return getSignatureParam("");
    }

    private String getSignatureParam(String payload) {
        String query = "recvWindow=15000&timestamp=" + getServerTime();
        return query + "&signature=" + getSignature(payload + query, secretKey);
    }

    private String getSignature(String payload, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            return new String(Hex.encodeHex(mac.doFinal(payload.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign message.", e);
        }
    }
}
