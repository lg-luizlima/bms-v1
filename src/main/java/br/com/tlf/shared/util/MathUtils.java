package br.com.tlf.shared.util;

import java.math.BigDecimal;

public class MathUtils {


    public static BigDecimal toPercentageInDecimal(BigDecimal percentage) {
        if (percentage == null) return null;
        if (percentage.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return percentage.divide(BigDecimal.valueOf(100), 10, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal toWholePercentage(BigDecimal percentage) {
        if (percentage == null) return null;
        if (percentage.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return percentage.multiply(BigDecimal.valueOf(100));
    }
}
