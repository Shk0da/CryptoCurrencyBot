package ai.trading.bot.util;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class IndicatorsUtil {

    private static final Core talib = new Core();

    public static final int MACD_FAST_PERIOD = 7;
    public static final int MACD_SLOW_PERIOD = 21;
    public static final int MACD_PERIOD = 14;
    public static final int RSI_PERIOD = 14;
    public static final int ADX_PERIOD = 14;
    public static final int MA_BLACK_PERIOD = 63;
    public static final int MA_WHITE_PERIOD = 7;
    public static final int EMA_PERIOD = 14;

    public static double macd(double[] inClose) {
        double[] outMACD = new double[inClose.length];
        double[] outMACDSignal = new double[inClose.length];
        double[] outMACDHist = new double[inClose.length];
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();

        RetCode retCode = talib.macd(0, inClose.length - 1, inClose,
                MACD_FAST_PERIOD, MACD_SLOW_PERIOD, MACD_PERIOD,
                outBegIdx, outNBElement, outMACD, outMACDSignal, outMACDHist);

        double value = 0.0;
        if (RetCode.Success == retCode && outNBElement.value > 0) {
            value = outMACD[outNBElement.value - 1];
        }

        log.trace("MACD: {}", value);
        return value;
    }

    public static double rsi(double[] inClose) {
        double[] outReal = new double[inClose.length];
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();

        RetCode retCodeRSI = talib.rsi(0, inClose.length - 1, inClose,
                RSI_PERIOD,
                outBegIdx, outNBElement, outReal);

        double rsi = 0.0;
        if (RetCode.Success == retCodeRSI && outNBElement.value > 0) {
            rsi = outReal[outNBElement.value - 1];
        }

        log.trace("RSI: {}", rsi);
        return rsi;
    }

    public static double adx(double[] inClose, double[] inLow, double[] inHigh) {
        double[] outADX = new double[inClose.length];
        MInteger beginADX = new MInteger();
        MInteger lengthADX = new MInteger();
        RetCode retCodeADX = talib.adx(
                0, inClose.length - 1, inHigh, inLow, inClose,
                ADX_PERIOD,
                beginADX, lengthADX, outADX
        );

        double adx = 0.0;
        if (RetCode.Success == retCodeADX && lengthADX.value > 0) {
            adx = outADX[lengthADX.value - 1];
        }

        log.trace("ADX: {}", adx);
        return adx;
    }

    public static double movingAverageBlack(double[] inClose) {
        double[] outBlackMA = new double[inClose.length];
        MInteger beginBlackMA = new MInteger();
        MInteger lengthBlackMA = new MInteger();
        RetCode retCodeBlackMA = talib.trima(
                0, inClose.length - 1, inClose,
                MA_BLACK_PERIOD,
                beginBlackMA, lengthBlackMA, outBlackMA
        );

        double blackMA = 0.0;
        if (RetCode.Success == retCodeBlackMA && lengthBlackMA.value > 0) {
            blackMA = outBlackMA[lengthBlackMA.value - 1];
        }

        log.trace("MovingAverageBlack: {}", blackMA);
        return blackMA;
    }

    public static double movingAverageWhite(double[] inClose) {
        double[] outWhiteMA = new double[inClose.length];
        MInteger beginWhiteMA = new MInteger();
        MInteger lengthWhiteMA = new MInteger();
        RetCode retCodeWhiteMA = talib.trima(
                0, inClose.length - 1, inClose,
                MA_WHITE_PERIOD,
                beginWhiteMA, lengthWhiteMA, outWhiteMA
        );

        double whiteMA = 0.0;
        if (RetCode.Success == retCodeWhiteMA && lengthWhiteMA.value > 0) {
            whiteMA = outWhiteMA[lengthWhiteMA.value - 1];
        }

        log.trace("MovingAverageWhite: {}", whiteMA);
        return whiteMA;
    }

    public static double ema(double[] inClose) {
        double[] outReal = new double[inClose.length];
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();

        RetCode retCode = talib.ema(
                0, inClose.length - 1, inClose,
                EMA_PERIOD,
                outBegIdx, outNBElement, outReal
        );

        double ema = 0.0;
        if (RetCode.Success == retCode && outNBElement.value > 0) {
            ema = outReal[outNBElement.value - 1];
        }

        log.trace("EMA: {}", ema);
        return ema;
    }
}
