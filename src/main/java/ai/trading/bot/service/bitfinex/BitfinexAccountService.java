package ai.trading.bot.service.bitfinex;

import ai.trading.bot.domain.ActiveOrder;
import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.HistoryOrder;
import ai.trading.bot.domain.Order;
import ai.trading.bot.domain.Wallet;
import ai.trading.bot.repository.BalanceRepository;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.service.AccountService;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service("bitfinexAccountService")
public class BitfinexAccountService implements AccountService {

    public static final String BASE_URL = "https://api.bitfinex.com";

    @Value("${bitfinex.api.key}")
    private String apiKey;

    @Value("${bitfinex.secret.key}")
    private String secretKey;

    @Autowired
    @Qualifier("bitfinexCandleRepository")
    private CandleRepository bitfinexCandleRepository;

    @Autowired
    @Qualifier("bitfinexBalanceRepository")
    private BalanceRepository balanceRepository;

    @Setter
    @Getter
    @Value("${bitfinex.trade.allowed:true}")
    private volatile Boolean tradeIsAllowed;

    private final JsonParser jsonParser = new JsonParser();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CandleRepository candleRepository() {
        return bitfinexCandleRepository;
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
            String jsonBody = restTemplate
                    .getForEntity(BASE_URL + "/v1/pubticker/" + symbol, String.class)
                    .getBody();
            JsonObject response = (JsonObject) jsonParser.parse(jsonBody);

            long timestamp = (long) (response.get("timestamp").getAsDouble() * 1000);
            candle = Candle.builder()
                    .symbol(symbol)
                    .dateTime(new Date(timestamp))
                    .ask(response.get("ask").getAsDouble())
                    .bid(response.get("bid").getAsDouble())
                    .price(response.get("last_price").getAsDouble())
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
            return sendPostRequest("order/new", order.toJsonObject()).getBody();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        }

        return null;
    }

    @Override
    public Object cancelOrder(String symbol, Long orderId) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("order_id", orderId.intValue());
            return sendPostRequest("order/cancel", json).getBody();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        }

        return null;
    }

    @Override
    public List<Wallet> getInfo() {
        try {
            JsonArray balances = new GsonBuilder()
                    .create()
                    .toJsonTree(sendPostRequest("balances").getBody())
                    .getAsJsonArray();

            List<Wallet> wallets = Lists.newArrayList();
            balances.forEach(jsonElement -> {
                        Double amount = jsonElement.getAsJsonObject().get("amount").getAsDouble();
                        Double available = jsonElement.getAsJsonObject().get("available").getAsDouble();
                        wallets.add(Wallet.builder()
                                .name(jsonElement.getAsJsonObject().get("currency").getAsString())
                                .free(available)
                                .locked(amount - available)
                                .build());
                    }
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
            JsonArray response = new GsonBuilder()
                    .create()
                    .toJsonTree(sendPostRequest("orders").getBody())
                    .getAsJsonArray();

            List<ActiveOrder> result = Lists.newArrayList();
            response.forEach(jsonElement -> {
                String status = "";
                if (jsonElement.getAsJsonObject().get("is_cancelled").getAsBoolean()) status = "canceled";
                if (jsonElement.getAsJsonObject().get("is_hidden").getAsBoolean()) status = "hidden";
                if (jsonElement.getAsJsonObject().get("was_forced").getAsBoolean()) status = "forced";

                result.add(ActiveOrder.builder()
                        .id(jsonElement.getAsJsonObject().get("id").getAsLong())
                        .symbol(jsonElement.getAsJsonObject().get("symbol").getAsString())
                        .side(jsonElement.getAsJsonObject().get("side").getAsString())
                        .type(jsonElement.getAsJsonObject().get("type").getAsString())
                        .amount(jsonElement.getAsJsonObject().get("original_amount").getAsDouble())
                        .price(jsonElement.getAsJsonObject().get("price").getAsDouble())
                        .timestamp((long) (jsonElement.getAsJsonObject().get("timestamp").getAsDouble() * 1000))
                        .status(status)
                        .build());
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

    @Override
    public List<HistoryOrder> getHistoryOrders(int limit) {
        try {
            JsonArray response = new GsonBuilder()
                    .create()
                    .toJsonTree(sendPostRequest("orders/hist?limit=" + limit).getBody())
                    .getAsJsonArray();

            List<HistoryOrder> result = Lists.newArrayList();
            response.forEach(jsonElement -> result.add(HistoryOrder.builder()
                    .id(jsonElement.getAsJsonObject().get("id").getAsLong())
                    .symbol(jsonElement.getAsJsonObject().get("symbol").getAsString())
                    .side(jsonElement.getAsJsonObject().get("side").getAsString())
                    .type(jsonElement.getAsJsonObject().get("type").getAsString())
                    .amount(jsonElement.getAsJsonObject().get("original_amount").getAsDouble())
                    .price(jsonElement.getAsJsonObject().get("price").getAsDouble())
                    .timestamp((long) (jsonElement.getAsJsonObject().get("timestamp").getAsDouble() * 1000))
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

    private ResponseEntity<Object> sendPostRequest(String uri) {
        try {
            TimeUnit.SECONDS.sleep(1); // fcking bitfinex frod
        } catch (InterruptedException nothing) {
        }
        return sendPostRequest(uri, null);
    }

    private ResponseEntity<Object> sendPostRequest(String uri, JsonObject data) {
        JsonObject body = new JsonObject();
        body.addProperty("request", "/v1/" + uri);
        body.addProperty("nonce", Long.toString(new Date().getTime() * 1000));
        if (data != null && data.size() > 0) {
            data.keySet().forEach(key -> {
                if ("order_id".equals(key)) {
                    body.addProperty(key, data.get(key).getAsInt());
                } else {
                    body.addProperty(key, data.get(key).getAsString());
                }
            });
        }

        log.debug("nonce: {}", body.get("nonce").getAsString());
        log.debug("body: {}", body);

        return restTemplate
                .exchange(
                        BASE_URL + "/v1/" + uri,
                        HttpMethod.POST,
                        new HttpEntity<>(body.toString(), getAuthHeader(body)),
                        Object.class
                );
    }

    private HttpHeaders getAuthHeader(JsonObject jsonObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("X-BFX-APIKEY", apiKey);

        String payload = Base64.getEncoder().encodeToString(jsonObject.toString().getBytes());
        headers.set("X-BFX-PAYLOAD", payload);
        headers.set("X-BFX-SIGNATURE", getSignature(payload, secretKey));

        return headers;
    }

    private String getSignature(String payload, String secretKey) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((secretKey).getBytes(StandardCharsets.UTF_8), "HmacSHA384");
            Mac mac = Mac.getInstance("HmacSHA384");
            mac.init(key);

            StringBuilder hash = new StringBuilder();
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error(ex.getMessage());
        }

        return digest;
    }
}
