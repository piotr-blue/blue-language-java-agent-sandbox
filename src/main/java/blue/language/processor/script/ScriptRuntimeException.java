package blue.language.processor.script;

public class ScriptRuntimeException extends RuntimeException {
    public ScriptRuntimeException(String message) {
        super(message);
    }

    public ScriptRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
