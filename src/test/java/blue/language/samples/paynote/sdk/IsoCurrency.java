package blue.language.samples.paynote.sdk;

import java.util.Currency;

public enum IsoCurrency {
    USD("USD"),
    EUR("EUR"),
    CHF("CHF"),
    GBP("GBP"),
    JPY("JPY");

    private final Currency currency;

    IsoCurrency(String code) {
        this.currency = Currency.getInstance(code);
    }

    public String code() {
        return currency.getCurrencyCode();
    }

    public int fractionDigits() {
        return currency.getDefaultFractionDigits();
    }

    public Currency asJavaCurrency() {
        return currency;
    }
}
