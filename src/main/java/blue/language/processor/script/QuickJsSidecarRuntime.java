package blue.language.processor.script;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class QuickJsSidecarRuntime implements ScriptRuntime {

    private static final ObjectMapper PROTOCOL_MAPPER = new ObjectMapper();

    private final String nodeBinary;
    private final Path sidecarScript;
    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;

    public QuickJsSidecarRuntime() {
        this("node", defaultSidecarScriptPath());
    }

    public QuickJsSidecarRuntime(String nodeBinary, Path sidecarScript) {
        this.nodeBinary = nodeBinary != null ? nodeBinary : "node";
        this.sidecarScript = sidecarScript != null ? sidecarScript : defaultSidecarScriptPath();
    }

    @Override
    public synchronized ScriptRuntimeResult evaluate(ScriptRuntimeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        ensureStarted();
        String id = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", id);
        payload.put("code", request.code());
        payload.put("bindings", request.bindings());
        payload.put("wasmGasLimit", request.wasmGasLimit() != null ? request.wasmGasLimit().toString() : null);

        try {
            writer.write(PROTOCOL_MAPPER.writeValueAsString(payload));
            writer.newLine();
            writer.flush();

            String line = reader.readLine();
            if (line == null) {
                throw new ScriptRuntimeException("QuickJS sidecar terminated unexpectedly");
            }
            Map<String, Object> response = PROTOCOL_MAPPER.readValue(
                    line,
                    new TypeReference<Map<String, Object>>() {
                    });
            Object responseId = response.get("id");
            if (responseId == null || !id.equals(String.valueOf(responseId))) {
                throw new ScriptRuntimeException("QuickJS sidecar protocol mismatch: expected id " + id);
            }
            Object ok = response.get("ok");
            if (!(ok instanceof Boolean) || !((Boolean) ok)) {
                Object error = response.get("error");
                RuntimeErrorDetails details = describeError(error);
                throw new ScriptRuntimeException(
                        "QuickJS sidecar evaluation failed: " + details.formattedMessage(),
                        details.name(),
                        details.message(),
                        details.stackAvailable());
            }
            boolean valueDefined = response.containsKey("result");
            if (response.get("resultDefined") instanceof Boolean) {
                valueDefined = ((Boolean) response.get("resultDefined")).booleanValue();
            }
            Object result = response.get("result");
            BigInteger used = toBigInteger(response.get("wasmGasUsed"));
            BigInteger remaining = toBigInteger(response.get("wasmGasRemaining"));
            return new ScriptRuntimeResult(result, used, remaining, valueDefined);
        } catch (IOException ex) {
            throw new ScriptRuntimeException("Failed to evaluate QuickJS sidecar request", ex);
        }
    }

    @Override
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignore
            }
            writer = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
                // ignore
            }
            reader = null;
        }
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    private void ensureStarted() {
        if (process != null && process.isAlive()) {
            return;
        }
        if (!Files.exists(sidecarScript)) {
            throw new ScriptRuntimeException("QuickJS sidecar script not found: " + sidecarScript.toAbsolutePath());
        }
        ProcessBuilder builder = new ProcessBuilder(nodeBinary, sidecarScript.toAbsolutePath().toString());
        builder.redirectErrorStream(true);
        try {
            process = builder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException ex) {
            throw new ScriptRuntimeException("Unable to start QuickJS sidecar process", ex);
        }
    }

    private static BigInteger toBigInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigInteger) {
            return (BigInteger) raw;
        }
        try {
            return new BigInteger(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static RuntimeErrorDetails describeError(Object payload) {
        if (!(payload instanceof Map)) {
            String fallback = String.valueOf(payload);
            return new RuntimeErrorDetails(null, fallback, false, fallback);
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) payload;
        Object name = map.get("name");
        Object message = map.get("message");
        Object stack = map.get("stack");
        StringBuilder builder = new StringBuilder();
        String normalizedName = null;
        String normalizedMessage = null;
        if (name != null && String.valueOf(name).trim().length() > 0) {
            normalizedName = String.valueOf(name).trim();
            builder.append(normalizedName);
        }
        if (message != null && String.valueOf(message).trim().length() > 0) {
            normalizedMessage = String.valueOf(message).trim();
            if (builder.length() > 0) {
                builder.append(": ");
            }
            builder.append(normalizedMessage);
        }
        if (builder.length() == 0) {
            builder.append(String.valueOf(payload));
        }
        boolean stackAvailable = stack != null && String.valueOf(stack).trim().length() > 0;
        if (stackAvailable) {
            builder.append(" [stack available]");
        }
        return new RuntimeErrorDetails(normalizedName, normalizedMessage, stackAvailable, builder.toString());
    }

    private static final class RuntimeErrorDetails {
        private final String name;
        private final String message;
        private final boolean stackAvailable;
        private final String formattedMessage;

        RuntimeErrorDetails(String name, String message, boolean stackAvailable, String formattedMessage) {
            this.name = name;
            this.message = message;
            this.stackAvailable = stackAvailable;
            this.formattedMessage = formattedMessage;
        }

        String name() {
            return name;
        }

        String message() {
            return message;
        }

        boolean stackAvailable() {
            return stackAvailable;
        }

        String formattedMessage() {
            return formattedMessage;
        }
    }

    private static Path defaultSidecarScriptPath() {
        String configured = System.getenv("QUICKJS_SIDECAR_PATH");
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured.trim());
        }
        return Paths.get("tools", "quickjs-sidecar", "index.js");
    }
}
