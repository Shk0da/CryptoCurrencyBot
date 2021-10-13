package ai.trading.bot.util;

import ai.trading.bot.domain.Candle;
import com.clearspring.analytics.util.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
public class StockDataSetIterator implements DataSetIterator {

    private static final int CHUNK_SHIFT = 91;
    private static final int VECTOR_K = 5;
    private static final int VECTOR_SIZE_1 = 5 * VECTOR_K;
    private static final int VECTOR_SIZE_2 = 5;
    private static final int LENGTH = 22;
    private static final int MINI_BATCH_SIZE = 32;

    private DataSetPreProcessor dataSetPreProcessor;
    private final List<Integer> exampleStartOffsets = new LinkedList<>();

    private final List<Candle> train;
    @Getter
    private final List<Pair<INDArray, Double>> test;

    @Getter
    double[][] indicators;
    @Getter
    double[] maxs = new double[]{Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
    @Getter
    double[] mins = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
    @Getter
    double[] closes = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};

    private final double splitRatio;
    private final List<Candle> stockDataList;

    public StockDataSetIterator(List<Candle> stockDataList) {
        this(stockDataList, 1.0);
    }

    public StockDataSetIterator(List<Candle> stockDataList, double splitRatio) {
        this.stockDataList = stockDataList;
        this.splitRatio = splitRatio;

        int split = (int) Math.round(stockDataList.size() * splitRatio);

        int initCapacity = VECTOR_K + 1;
        indicators = new double[initCapacity][];
        for (int i = 0; i < initCapacity; i++) {
            indicators[i] = new double[stockDataList.size()];
        }
        initializeIndicators(stockDataList);

        int startIndexTrainData = 0;
        for (double[] indicator : indicators) {
            for (int j = 0; j < indicator.length; j++) {
                if (indicator[j] != 0.0 && j > startIndexTrainData) {
                    startIndexTrainData = j;
                    break;
                }
            }
        }

        double[][] indicatorsSlice = new double[initCapacity][];
        for (int i = 0; i < initCapacity; i++) {
            indicatorsSlice[i] = new double[stockDataList.size() - startIndexTrainData];
        }

        for (int i = 0; i < initCapacity; i++) {
            System.arraycopy(indicators[i], startIndexTrainData, indicatorsSlice[i], 0, indicatorsSlice[i].length);
        }

        indicators = indicatorsSlice;
        train = (startIndexTrainData < split) ? stockDataList.subList(startIndexTrainData, split) : stockDataList.subList(0, 0);
        test = generateTestDataSet(stockDataList.subList(split, stockDataList.size()));

        initializeOffsets();
    }

    public static double normalize(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.0001;
    }

    public static double deNormalize(double input, double min, double max) {
        return min + (input - 0.0001) * (max - min) / 0.8;
    }

    public static int getVectorSize() {
        return (CHUNK_SHIFT + VECTOR_SIZE_1 + VECTOR_SIZE_2) * 2;
    }

    @Override
    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();

        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, inputColumns(), LENGTH}, 'f');
        INDArray label = Nd4j.create(new int[]{actualMiniBatchSize, totalOutcomes(), LENGTH}, 'f');
        for (int index = 0; index < actualMiniBatchSize - 1; index++) {
            int startIdx = exampleStartOffsets.remove(0) + VECTOR_SIZE_2;
            int l = startIdx - VECTOR_SIZE_2;
            int window = startIdx + LENGTH;
            for (int i = startIdx; i < window - 1; i++) {
                // input
                int k = 0;
                List<String> debugVector = new ArrayList<>(inputColumns());
                while (k < VECTOR_SIZE_1) {
                    int n = 0;
                    while (n <= VECTOR_K) {
                        double indicator = (l < indicators[n].length - 1) ? indicators[n][l] : indicators[n][indicators[n].length - 1];
                        input.putScalar(new int[]{index, k, i - startIdx}, normalize(indicator, mins[n], maxs[n]));
                        debugVector.add("$n($l):$indicator");
                        k++;
                        n++;
                    }
                    l++;
                }
                l = i - VECTOR_SIZE_2 + 1;
                k = VECTOR_SIZE_1;
                int j = VECTOR_SIZE_2;
                while (k < VECTOR_SIZE_1 + VECTOR_SIZE_2) {
                    double close = (i - j < train.size() - 1) ? train.get(i - j).getPrice() : train.get(train.size() - 1).getPrice();
                    input.putScalar(new int[]{index, k, i - startIdx}, normalize(close, closes[0], closes[1]));
                    debugVector.add((i - j) + ":$close");
                    k++;
                    j--;
                }
                // label
                int predictIndex = train.size() - 1;
                if (train.size() - 1 > i + 3) predictIndex = i + 3;
                if (train.size() - 1 > i + 2) predictIndex = i + 2;
                if (train.size() - 1 > i + 1) predictIndex = i + 1;
                if (train.size() - 1 > i) predictIndex = i;

                for (int labelValue = 0; labelValue < totalOutcomes() - 1; labelValue++) {
                    label.putScalar(new int[]{index, labelValue, i - startIdx}, normalize(indicators[labelValue][predictIndex], mins[labelValue], maxs[labelValue]));
                    debugVector.add("resInd" + labelValue + " $predictIndex:" + indicators[labelValue][predictIndex]);
                }
                label.putScalar(new int[]{index, totalOutcomes() - 1, i - startIdx}, normalize(train.get(predictIndex).getPrice(), closes[0], closes[1]));
                debugVector.add("resClose" + (totalOutcomes() - 1) + " $predictIndex:" + train.get(predictIndex).getPrice());
                log.trace("NextDataSet $index(" + (i - startIdx) + "): " + debugVector);
            }
            if (exampleStartOffsets.size() == 0) break;
        }

        return new DataSet(input, label);
    }

    @Override
    public void remove() {
        //
    }

    @Override
    public int totalExamples() {
        return this.train.size() - LENGTH - 1;
    }

    @Override
    public int inputColumns() {
        return VECTOR_SIZE_1 + VECTOR_SIZE_2 + 1;
    }

    @Override
    public int totalOutcomes() {
        return this.indicators.length + 1;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        this.initializeOffsets();
    }

    @Override
    public int batch() {
        return MINI_BATCH_SIZE;
    }

    @Override
    public int cursor() {
        return this.totalExamples() - this.exampleStartOffsets.size();
    }

    @Override
    public int numExamples() {
        return this.totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        this.dataSetPreProcessor = dataSetPreProcessor;
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return this.dataSetPreProcessor;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return this.exampleStartOffsets.size() > 0;
    }

    @Override
    public DataSet next() {
        return this.next(MINI_BATCH_SIZE);
    }

    private void initializeIndicators(List<Candle> stockDataList) {
        List<Candle> candles = Lists.newArrayList(stockDataList);

        // MACD
        int chunkMacdSize = IndicatorsUtil.MACD_SLOW_PERIOD + CHUNK_SHIFT;
        int chunkMacdCounter = chunkMacdSize;
        for (int i = chunkMacdSize; i < candles.size() - 1; i++) {
            double[] inClose = new double[chunkMacdSize];
            int k = 0;
            int j = i - chunkMacdSize;
            while (k < chunkMacdSize) {
                inClose[k] = candles.get(j).getPrice();
                k++;
                j++;
            }
            double value = IndicatorsUtil.macd(inClose);
            indicators[0][chunkMacdCounter++] = value;
            if (value < mins[0]) mins[0] = value;
            if (value > maxs[0]) maxs[0] = value;
        }

        // RSI
        int chunkRsiSize = IndicatorsUtil.RSI_PERIOD + CHUNK_SHIFT;
        int chunkRsiCounter = chunkRsiSize;
        for (int i = chunkRsiSize; i < candles.size() - 1; i++) {
            double[] inClose = new double[chunkRsiSize];
            int k = 0;
            int j = i - chunkRsiSize;
            while (k < chunkRsiSize) {
                inClose[k] = candles.get(j).getPrice();
                k++;
                j++;
            }
            double value = IndicatorsUtil.rsi(inClose);
            indicators[1][chunkRsiCounter++] = value;
            if (value < mins[1]) mins[1] = value;
            if (value > maxs[1]) maxs[1] = value;
        }

        // ADX
        int chunkAdxSize = IndicatorsUtil.ADX_PERIOD + CHUNK_SHIFT;
        int chunkAdxCounter = chunkAdxSize;
        for (int i = chunkAdxSize; i < candles.size() - 1; i++) {
            double[] inClose = new double[chunkAdxSize];
            double[] inHigh = new double[chunkAdxSize];
            double[] inLow = new double[chunkAdxSize];
            int k = 0;
            int j = i - chunkAdxSize;
            while (k < chunkAdxSize) {
                inClose[k] = candles.get(j).getPrice();
                inHigh[k] = candles.get(j).getBid();
                inLow[k] = candles.get(j).getAsk();
                k++;
                j++;
            }
            double value = IndicatorsUtil.adx(inClose, inLow, inHigh);
            indicators[2][chunkAdxCounter++] = value;
            if (value < mins[2]) mins[2] = value;
            if (value > maxs[2]) maxs[2] = value;
        }

        // MA Black
        int chunkMABCounter = IndicatorsUtil.MA_BLACK_PERIOD;
        for (int i = IndicatorsUtil.MA_BLACK_PERIOD; i < candles.size() - 1; i++) {
            double[] inClose = new double[IndicatorsUtil.MA_BLACK_PERIOD];
            int k = 0;
            int j = i - IndicatorsUtil.MA_BLACK_PERIOD;
            while (k < IndicatorsUtil.MA_BLACK_PERIOD) {
                inClose[k] = candles.get(j).getPrice();
                k++;
                j++;
            }
            double value = IndicatorsUtil.movingAverageBlack(inClose);
            indicators[3][chunkMABCounter++] = value;
            if (value < mins[3]) mins[3] = value;
            if (value > maxs[3]) maxs[3] = value;
        }

        // MA White
        int chunkMAWCounter = IndicatorsUtil.MA_WHITE_PERIOD;
        for (int i = IndicatorsUtil.MA_WHITE_PERIOD; i < candles.size() - 1; i++) {
            double[] inClose = new double[IndicatorsUtil.MA_WHITE_PERIOD];
            int k = 0;
            int j = i - IndicatorsUtil.MA_WHITE_PERIOD;
            while (k < IndicatorsUtil.MA_WHITE_PERIOD) {
                inClose[k] = candles.get(j).getPrice();
                k++;
                j++;
            }
            double value = IndicatorsUtil.movingAverageWhite(inClose);
            indicators[4][chunkMAWCounter++] = value;
            if (value < mins[4]) mins[4] = value;
            if (value > maxs[4]) maxs[4] = value;
        }

        // EMA
        int chunkEmaCounter = IndicatorsUtil.EMA_PERIOD;
        for (int i = IndicatorsUtil.EMA_PERIOD; i < candles.size() - 1; i++) {
            double[] inClose = new double[IndicatorsUtil.EMA_PERIOD];
            int k = 0;
            int j = i - IndicatorsUtil.EMA_PERIOD;
            while (k < IndicatorsUtil.EMA_PERIOD) {
                inClose[k] = candles.get(j).getPrice();
                k++;
                j++;
            }
            double value = IndicatorsUtil.ema(inClose);
            indicators[5][chunkEmaCounter++] = value;
            if (value < mins[5]) mins[5] = value;
            if (value > maxs[5]) maxs[5] = value;
        }

        for (Candle candle : candles) {
            if (candle.getPrice() < closes[0]) closes[0] = candle.getPrice();
            if (candle.getPrice() > closes[1]) closes[1] = candle.getPrice();
        }
    }

    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = train.size() - LENGTH;
        for (int i = 0; i < window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    private List<Pair<INDArray, Double>> generateTestDataSet(List<Candle> stockDataList) {
        List<Pair<INDArray, Double>> test = new ArrayList<>();
        int l = train.size();
        for (int i = VECTOR_SIZE_2; i < stockDataList.size() - 1; i++) {
            int k = 0;
            INDArray input = Nd4j.create(new int[]{1, inputColumns()}, 'f');
            List<String> debugVector = new ArrayList<>(inputColumns());
            while (k < VECTOR_SIZE_1) {
                int n = 0;
                while (n <= VECTOR_K) {
                    double indicator = (l < indicators[n].length - 1) ? indicators[n][l] : indicators[n][indicators[n].length - 1];
                    input.putScalar(new int[]{0, k}, normalize(indicator, mins[n], maxs[n]));
                    debugVector.add("$n($l):$indicator");
                    k++;
                    n++;
                }
                l++;
            }
            l = train.size() + i - VECTOR_SIZE_2 + 1;
            k = VECTOR_SIZE_1;
            int j = VECTOR_SIZE_2;
            while (k < VECTOR_SIZE_1 + VECTOR_SIZE_2) {
                double close = (i - j < stockDataList.size() - 1) ? stockDataList.get(i - j).getPrice() : stockDataList.get(stockDataList.size() - 1).getPrice();
                input.putScalar(new int[]{0, k}, normalize(close, closes[0], closes[1]));
                debugVector.add((i - j) + ":" + close);
                k++;
                j--;
            }
            test.add(new ImmutablePair<>(input, normalize(stockDataList.get(i - j).getPrice(), closes[0], closes[1])));
            debugVector.add("res " + (i - j) + ": " + stockDataList.get(i - j).getPrice());
            log.trace("TestDataSet " + (i - VECTOR_SIZE_2) + ": " + debugVector);
        }
        return test;
    }
}
