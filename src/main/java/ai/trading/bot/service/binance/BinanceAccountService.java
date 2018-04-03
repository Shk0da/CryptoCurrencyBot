package ai.trading.bot.service.binance;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.Order;
import ai.trading.bot.repository.CandleRepository;
import ai.trading.bot.service.AccountService;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.List;
import java.util.Map;

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

    private final JsonParser jsonParser = new JsonParser();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public CandleRepository candleRepository() {
        return binanceCandleRepository;
    }

    @Override
    public List<Candle> getCandles(String symbol) {
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
    public Map<String, Object> createOrder(Order order) {
        return null;
    }

    @Override
    public Map<String, Object> cancelOrder(String symbol, Long orderId) {
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
    public Object getInfo() {
        try {
            return sendGetRequest("account").getBody();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
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
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            return new String(Hex.encodeHex(sha256_HMAC.doFinal(payload.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign message.", e);
        }
    }
}
