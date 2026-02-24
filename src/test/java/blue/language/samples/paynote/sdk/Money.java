package blue.language.samples.paynote.sdk;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private final IsoCurrency currency;
    private final long minor;

    private Money(IsoCurrency currency, long minor) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        this.currency = currency;
        this.minor = minor;
    }

    public static Money ofMinor(IsoCurrency currency, long minor) {
        return new Money(currency, minor);
    }

    public static Money ofMajor(IsoCurrency currency, BigDecimal major) {
        if (major == null) {
            throw new IllegalArgumentException("major amount is required");
        }
        int fractionDigits = currency.fractionDigits();
        if (major.scale() > fractionDigits) {
            throw new IllegalArgumentException("Amount scale " + major.scale()
                    + " exceeds currency fraction digits " + fractionDigits + " for " + currency.code());
        }
        BigDecimal shifted = major.setScale(fractionDigits, RoundingMode.UNNECESSARY)
                .movePointRight(fractionDigits);
        return new Money(currency, shifted.longValueExact());
    }

    public static Money ofMajor(IsoCurrency currency, String major) {
        return ofMajor(currency, new BigDecimal(major));
    }

    public IsoCurrency currency() {
        return currency;
    }

    public long minor() {
        return minor;
    }
}
