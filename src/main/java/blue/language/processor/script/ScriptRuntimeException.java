package blue.language.processor.script;

public class ScriptRuntimeException extends RuntimeException {
    private final String errorName;
    private final String runtimeMessage;
    private final boolean stackAvailable;

    public ScriptRuntimeException(String message) {
        this(message, null, null, false, null);
    }

    public ScriptRuntimeException(String message, Throwable cause) {
        this(message, null, null, false, cause);
    }

    public ScriptRuntimeException(String message,
                                  String errorName,
                                  String runtimeMessage,
                                  boolean stackAvailable) {
        this(message, errorName, runtimeMessage, stackAvailable, null);
    }

    public ScriptRuntimeException(String message,
                                  String errorName,
                                  String runtimeMessage,
                                  boolean stackAvailable,
                                  Throwable cause) {
        super(message, cause);
        this.errorName = errorName;
        this.runtimeMessage = runtimeMessage;
        this.stackAvailable = stackAvailable;
    }

    public String errorName() {
        return errorName;
    }

    public String runtimeMessage() {
        return runtimeMessage;
    }

    public boolean stackAvailable() {
        return stackAvailable;
    }
}
