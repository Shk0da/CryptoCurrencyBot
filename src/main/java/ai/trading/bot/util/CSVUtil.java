package ai.trading.bot.util;

import ai.trading.bot.domain.Candle;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@UtilityClass
public final class CSVUtil {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSSZ");

    @SneakyThrows
    public static void saveCandles(List<Candle> candles, String fileName) {
        if (candles.isEmpty()) return;

        String separator = ",";
        StringBuilder stringBuilder = new StringBuilder();
        candles.forEach(candle -> {
            stringBuilder.append(candle.getSymbol());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getPrice());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getAsk());
            stringBuilder.append(separator);
            stringBuilder.append(candle.getBid());
            stringBuilder.append(separator);
            stringBuilder.append(simpleDateFormat.format(candle.getDateTime()));
            stringBuilder.append("\n");
        });

        File csvDataFile = new File(fileName + ".csv");
        if (csvDataFile.exists() || !csvDataFile.exists() && csvDataFile.createNewFile()) {
            FileCopyUtils.copy(
                    stringBuilder.toString(),
                    new OutputStreamWriter(new FileOutputStream(csvDataFile), UTF_8)
            );
        } else {
            log.error("Something wrong. File {} not save.", csvDataFile.getName());
        }
    }

    @SneakyThrows
    public static List<Candle> getCandles(String fileName, int size) {
        File csvDataFile = new File(fileName);
        if (!csvDataFile.exists()) return Lists.newArrayList();

        List<Candle> data = Lists.newArrayList();
        CSVReader reader = new CSVReader(new FileReader(csvDataFile));
        String[] line;
        while ((line = reader.readNext()) != null) {
            Candle candle = Candle.builder()
                    .symbol(line[0])
                    .price(parseDouble(line[1]))
                    .ask(parseDouble(line[2]))
                    .bid(parseDouble(line[3]))
                    .dateTime(simpleDateFormat.parse(line[4]))
                    .build();
            data.add(candle);
        }

        return data.subList(0, min(data.size(), size));
    }
}
