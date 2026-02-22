package blue.language.processor.script;

public class CodeBlockEvaluationError extends RuntimeException {
    public CodeBlockEvaluationError(String message, Throwable cause) {
        super(message, cause);
    }
}
