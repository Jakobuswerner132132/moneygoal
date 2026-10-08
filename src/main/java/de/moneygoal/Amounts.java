package de.moneygoal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Betraege lesen ("5k", "1,5m", "2.500.000") und deutsch formatieren (1.234.567). */
public final class Amounts {
    private Amounts() {
    }

    public static String format(long value) {
        String digits = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                sb.append('.');
            }
            sb.append(digits.charAt(i));
        }
        return (value < 0 ? "-" : "") + sb;
    }

    /** Gibt null zurueck, wenn der Text kein gueltiger Betrag ist. */
    public static Long parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace("$", "")
                .replace("\u20ac", "")
                .replace("_", "")
                .replace(" ", "")
                .replace("'", "");
        if (s.isEmpty()) {
            return null;
        }

        BigDecimal multiplier = BigDecimal.ONE;
        if (s.endsWith("mrd")) {
            multiplier = new BigDecimal("1000000000");
            s = s.substring(0, s.length() - 3);
        } else if (s.endsWith("mio")) {
            multiplier = new BigDecimal("1000000");
            s = s.substring(0, s.length() - 3);
        } else {
            char last = s.charAt(s.length() - 1);
            if (last == 'k') {
                multiplier = new BigDecimal("1000");
                s = s.substring(0, s.length() - 1);
            } else if (last == 'm') {
                multiplier = new BigDecimal("1000000");
                s = s.substring(0, s.length() - 1);
            } else if (last == 'b') {
                multiplier = new BigDecimal("1000000000");
                s = s.substring(0, s.length() - 1);
            } else if (last == 't') {
                multiplier = new BigDecimal("1000000000000");
                s = s.substring(0, s.length() - 1);
            }
        }
        if (s.isEmpty()) {
            return null;
        }
        boolean hasSuffix = multiplier.compareTo(BigDecimal.ONE) != 0;

        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        String numeric;
        if (lastDot < 0 && lastComma < 0) {
            numeric = s;
        } else {
            char sep;
            int lastSep;
            if (lastDot > lastComma) {
                sep = '.';
                lastSep = lastDot;
            } else {
                sep = ',';
                lastSep = lastComma;
            }
            boolean bothKinds = lastDot >= 0 && lastComma >= 0;
            int count = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == sep) {
                    count++;
                }
            }
            int digitsAfter = s.length() - lastSep - 1;

            boolean decimal;
            if (bothKinds) {
                decimal = true;
            } else if (count > 1) {
                decimal = false;
            } else {
                decimal = hasSuffix || digitsAfter != 3;
            }

            String intPart = s.substring(0, lastSep).replace(".", "").replace(",", "");
            String fracPart = s.substring(lastSep + 1);
            numeric = decimal ? intPart + "." + fracPart : intPart + fracPart;
        }

        try {
            BigDecimal value = new BigDecimal(numeric).multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
            return value.longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }
}
