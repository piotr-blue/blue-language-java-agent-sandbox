package blue.language.processor.script;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuickJSEvaluator implements AutoCloseable {

    private final ScriptRuntime runtime;

    public QuickJSEvaluator() {
        this(new QuickJsSidecarRuntime());
    }

    public QuickJSEvaluator(ScriptRuntime runtime) {
        this.runtime = runtime;
    }

    public ScriptRuntimeResult evaluate(String code, Map<String, Object> bindings, BigInteger wasmGasLimit) {
        Map<String, Object> safeBindings = bindings == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<>(bindings);
        try {
            return runtime.evaluate(new ScriptRuntimeRequest(code, safeBindings, wasmGasLimit));
        } catch (ScriptRuntimeException ex) {
            throw new CodeBlockEvaluationError("QuickJS code block evaluation failed", ex);
        }
    }

    @Override
    public void close() {
        runtime.close();
    }
}
