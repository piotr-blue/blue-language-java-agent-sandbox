package blue.language.samples.paynote.dsl;

public final class DocPath {

    private final String pointer;

    private DocPath(String pointer) {
        this.pointer = pointer;
    }

    public static DocPath of(String pointer) {
        if (pointer == null) {
            throw new IllegalArgumentException("pointer cannot be null");
        }
        String trimmed = pointer.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("pointer cannot be empty");
        }
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException("pointer must start with '/': " + pointer);
        }
        return new DocPath(trimmed);
    }

    public String pointer() {
        return pointer;
    }

    @Override
    public String toString() {
        return pointer;
    }
}
