package blue.language.processor.script;

import java.math.BigInteger;

public final class ScriptRuntimeResult {
    private final Object value;
    private final BigInteger wasmGasUsed;
    private final BigInteger wasmGasRemaining;

    public ScriptRuntimeResult(Object value, BigInteger wasmGasUsed, BigInteger wasmGasRemaining) {
        this.value = value;
        this.wasmGasUsed = wasmGasUsed;
        this.wasmGasRemaining = wasmGasRemaining;
    }

    public Object value() {
        return value;
    }

    public BigInteger wasmGasUsed() {
        return wasmGasUsed;
    }

    public BigInteger wasmGasRemaining() {
        return wasmGasRemaining;
    }
}
