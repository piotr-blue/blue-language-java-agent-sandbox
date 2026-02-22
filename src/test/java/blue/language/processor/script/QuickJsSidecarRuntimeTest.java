package blue.language.processor.script;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class QuickJsSidecarRuntimeTest {

    @Test
    void evaluatesJavaScriptExpressionsThroughSidecarProtocol() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for sidecar tests");

        try (QuickJsSidecarRuntime runtime = new QuickJsSidecarRuntime()) {
            ScriptRuntimeResult simple = runtime.evaluate(ScriptRuntimeRequest.of("1 + 1"));
            assertEquals("2", String.valueOf(simple.value()));

            Map<String, Object> bindings = new LinkedHashMap<>();
            bindings.put("a", 5);
            bindings.put("b", 7);
            ScriptRuntimeResult withBindings = runtime.evaluate(
                    new ScriptRuntimeRequest("a + b", bindings, BigInteger.valueOf(1234L)));

            assertEquals("12", String.valueOf(withBindings.value()));
            assertEquals(BigInteger.ZERO, withBindings.wasmGasUsed());
            assertEquals(new BigInteger("1234"), withBindings.wasmGasRemaining());

            ScriptRuntimeResult withEmit = runtime.evaluate(ScriptRuntimeRequest.of(
                    "emit({ kind: 'callback' }); 9"));
            assertTrue(withEmit.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> emittedPayload = (Map<String, Object>) withEmit.value();
            assertEquals("9", String.valueOf(emittedPayload.get("__result")));
            assertTrue(emittedPayload.get("events") instanceof List);

            ScriptRuntimeResult withoutDate = runtime.evaluate(ScriptRuntimeRequest.of("typeof Date"));
            assertEquals("undefined", String.valueOf(withoutDate.value()));

            ScriptRuntimeResult withoutProcess = runtime.evaluate(ScriptRuntimeRequest.of("typeof process"));
            assertEquals("undefined", String.valueOf(withoutProcess.value()));
        }
    }

    private boolean nodeAvailable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", "--version").start();
        int exit = process.waitFor();
        return exit == 0;
    }
}
