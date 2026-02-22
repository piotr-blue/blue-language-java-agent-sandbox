package blue.language.processor.script;

public class CodeBlockEvaluationError extends RuntimeException {

    private static final int MAX_SNIPPET_LENGTH = 120;
    private final String code;

    public CodeBlockEvaluationError(String code, Throwable cause) {
        super("Failed to evaluate code block: " + truncate(code), cause);
        this.code = code;
    }

    public String code() {
        return code;
    }

    private static String truncate(String code) {
        if (code == null) {
            return "";
        }
        if (code.length() <= MAX_SNIPPET_LENGTH) {
            return code;
        }
        return code.substring(0, MAX_SNIPPET_LENGTH - 3) + "...";
    }
}
