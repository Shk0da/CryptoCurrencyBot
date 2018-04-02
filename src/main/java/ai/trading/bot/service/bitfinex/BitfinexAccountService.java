package ai.trading.bot.service.bitfinex;

import ai.trading.bot.domain.Candle;
import ai.trading.bot.domain.Order;
import ai.trading.bot.service.AccountService;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("bitfinexAccountService")
public class BitfinexAccountService implements AccountService {

    public static final String BASE_URL = "https://api.bitfinex.com/v1/";

    @Value("${bitfinex.api.key}")
    private String apiKey;

    @Value("${bitfinex.secret.key}")
    private String secretKey;

    private final JsonParser jsonParser = new JsonParser();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<Candle> getCandles(String symbol) {
        Candle candle = null;
        try {
            String jsonBody = restTemplate
                    .getForEntity(BASE_URL + "pubticker/" + symbol, String.class)
                    .getBody();
            JsonObject response = (JsonObject) jsonParser.parse(jsonBody);

            long timestamp = (long) (response.get("timestamp").getAsDouble() * 1000);
            candle = Candle.builder()
                    .symbol(symbol)
                    .dateTime(new DateTime(timestamp))
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
    public Map<String, Object> createOrder(Order order) {
        return null;
    }

    @Override
    public Map<String, Object> cancelOrder(String symbol, Long orderId) {
        return null;
    }

    @Override
    public Object getInfo() {
        try {
            String jsonInfo = restTemplate
                    .postForEntity(BASE_URL + "balances", new HttpEntity<>(getAuthHeader()), String.class)
                    .getBody();
            return jsonParser.parse(jsonInfo);
        } catch (HttpClientErrorException ex) {
            log.error(ex.getResponseBodyAsString());
        }

        return null;
    }

    private HttpHeaders getAuthHeader() {
        return getAuthHeader(new JsonObject());
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
            SecretKeySpec key = new SecretKeySpec((secretKey).getBytes("UTF-8"), "HmacSHA384");
            Mac mac = Mac.getInstance("HmacSHA384");
            mac.init(key);

            StringBuilder hash = new StringBuilder();
            byte[] bytes = mac.doFinal(payload.getBytes("ASCII"));
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException ex) {
            log.error(ex.getMessage());
        }

        return digest;
    }
}
