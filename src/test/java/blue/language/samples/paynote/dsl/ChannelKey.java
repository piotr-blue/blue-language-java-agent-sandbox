package blue.language.samples.paynote.dsl;

public final class ChannelKey {

    private final String value;

    private ChannelKey(String value) {
        this.value = value;
    }

    public static ChannelKey of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("channel key cannot be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("channel key cannot be empty");
        }
        return new ChannelKey(trimmed);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
