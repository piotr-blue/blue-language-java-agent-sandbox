package blue.language.processor.script;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class QuickJSEvaluatorTest {

    @Test
    void evaluatesSynchronousCodeAndExposesBindings() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Map<String, Object> bindings = new LinkedHashMap<>();
            bindings.put("steps", 7);
            bindings.put("event", new LinkedHashMap<String, Object>() {{
                put("payload", new LinkedHashMap<String, Object>() {{
                    put("value", 5);
                }});
            }});

            ScriptRuntimeResult result = evaluator.evaluate(
                    "steps + event.payload.value",
                    bindings,
                    new BigInteger("1000000000"));

            assertEquals("12", String.valueOf(result.value()));
            assertTrue(result.wasmGasUsed() != null && result.wasmGasUsed().compareTo(BigInteger.ZERO) > 0);
            assertTrue(result.wasmGasRemaining() != null
                    && result.wasmGasRemaining().compareTo(new BigInteger("1000000000")) < 0
                    && result.wasmGasRemaining().compareTo(BigInteger.ZERO) >= 0);
        }
    }

    @Test
    void supportsCanonHelpersAndDocumentBindings() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Map<String, Object> bindings = new LinkedHashMap<>();
            Map<String, Object> canonicalEvent = new LinkedHashMap<>();
            canonicalEvent.put("payload", new LinkedHashMap<String, Object>() {{
                put("id", new LinkedHashMap<String, Object>() {{
                    put("value", "evt-123");
                }});
            }});
            bindings.put("eventCanonical", canonicalEvent);
            bindings.put("__documentDataSimple", new LinkedHashMap<String, Object>() {{
                put("unit", "points");
            }});
            bindings.put("__documentDataCanonical", new LinkedHashMap<String, Object>() {{
                put("unit", new LinkedHashMap<String, Object>() {{
                    put("value", "points");
                }});
            }});

            ScriptRuntimeResult result = evaluator.evaluate(
                    "({ id: canon.unwrap(canon.at(eventCanonical, '/payload/id')), unit: document('/unit'), canonicalUnit: document.canonical('/unit').value })",
                    bindings,
                    BigInteger.valueOf(1000L));

            assertTrue(result.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) result.value();
            assertEquals("evt-123", String.valueOf(value.get("id")));
            assertEquals("points", String.valueOf(value.get("unit")));
            assertEquals("points", String.valueOf(value.get("canonicalUnit")));
        }
    }

    @Test
    void canonUnwrapSupportsDeepAndShallowModes() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            ScriptRuntimeResult result = evaluator.evaluate(
                    "const canonicalEvent = {\n" +
                            "  payload: {\n" +
                            "    id: { value: 'evt-123' },\n" +
                            "    tags: { items: [{ value: 'a' }, { value: 'b' }] }\n" +
                            "  },\n" +
                            "  name: { value: 'example' }\n" +
                            "};\n" +
                            "const pointer = canon.at(canonicalEvent, '/payload/id');\n" +
                            "return {\n" +
                            "  pointerUnwrapped: canon.unwrap(pointer),\n" +
                            "  eventPlain: canon.unwrap(canonicalEvent),\n" +
                            "  eventShallow: canon.unwrap(canonicalEvent, false),\n" +
                            "  arrayPlain: canon.unwrap({ items: [{ value: 1 }, { value: 2 }] })\n" +
                            "};",
                    new LinkedHashMap<String, Object>(),
                    BigInteger.valueOf(1000L));

            assertTrue(result.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) result.value();
            assertEquals("evt-123", String.valueOf(value.get("pointerUnwrapped")));

            assertTrue(value.get("eventPlain") instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> eventPlain = (Map<String, Object>) value.get("eventPlain");
            assertEquals("example", String.valueOf(eventPlain.get("name")));
            assertTrue(eventPlain.get("payload") instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) eventPlain.get("payload");
            assertEquals("evt-123", String.valueOf(payload.get("id")));
            assertTrue(payload.get("tags") instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> tags = (List<Object>) payload.get("tags");
            assertEquals(2, tags.size());
            assertEquals("a", String.valueOf(tags.get(0)));
            assertEquals("b", String.valueOf(tags.get(1)));

            assertTrue(value.get("eventShallow") instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> eventShallow = (Map<String, Object>) value.get("eventShallow");
            assertTrue(eventShallow.get("payload") instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> shallowPayload = (Map<String, Object>) eventShallow.get("payload");
            assertTrue(shallowPayload.get("id") instanceof Map);

            assertTrue(value.get("arrayPlain") instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> arrayPlain = (List<Object>) value.get("arrayPlain");
            assertEquals("1", String.valueOf(arrayPlain.get(0)));
            assertEquals("2", String.valueOf(arrayPlain.get(1)));
        }
    }

    @Test
    void wrapsSyntaxErrorsInCodeBlockEvaluationError() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            CodeBlockEvaluationError error = assertThrows(
                    CodeBlockEvaluationError.class,
                    () -> evaluator.evaluate(
                            "const data = await Promise.resolve(1); data;",
                            new LinkedHashMap<String, Object>(),
                            BigInteger.valueOf(1000L)));
            assertTrue(error.getMessage().contains("Failed to evaluate code block"));
            assertTrue(error.code().contains("await"));
        }
    }

    @Test
    void evaluatorCanBeReusedAcrossMultipleCalls() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            ScriptRuntimeResult first = evaluator.evaluate("1", new LinkedHashMap<String, Object>(), BigInteger.valueOf(500L));
            ScriptRuntimeResult second = evaluator.evaluate("2", new LinkedHashMap<String, Object>(), BigInteger.valueOf(500L));

            assertEquals("1", String.valueOf(first.value()));
            assertEquals("2", String.valueOf(second.value()));
        }
    }

    @Test
    void capturesEmitCallsInReturnedEnvelope() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            ScriptRuntimeResult result = evaluator.evaluate(
                    "emit({ kind: 'debug', value: 42 }); 7",
                    new LinkedHashMap<String, Object>(),
                    BigInteger.valueOf(1000L));

            assertTrue(result.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) result.value();
            assertEquals("7", String.valueOf(payload.get("__result")));
            assertTrue(payload.get("events") instanceof List);
            @SuppressWarnings("unchecked")
            List<Object> events = (List<Object>) payload.get("events");
            assertEquals(1, events.size());
        }
    }

    @Test
    void providesDefaultBindingsWhenMissing() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            ScriptRuntimeResult result = evaluator.evaluate(
                    "({ eventIsNull: event === null, eventCanonicalIsNull: eventCanonical === null, " +
                            "stepsIsArray: Array.isArray(steps), stepsLength: steps.length, " +
                            "currentContractIsNull: currentContract === null, " +
                            "currentContractCanonicalIsNull: currentContractCanonical === null })",
                    new LinkedHashMap<String, Object>(),
                    BigInteger.valueOf(1000L));

            assertTrue(result.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) result.value();
            assertEquals(Boolean.TRUE, value.get("eventIsNull"));
            assertEquals(Boolean.TRUE, value.get("eventCanonicalIsNull"));
            assertEquals(Boolean.TRUE, value.get("stepsIsArray"));
            assertEquals("0", String.valueOf(value.get("stepsLength")));
            assertEquals(Boolean.TRUE, value.get("currentContractIsNull"));
            assertEquals(Boolean.TRUE, value.get("currentContractCanonicalIsNull"));
        }
    }

    @Test
    void fallsBackCanonicalBindingsToPlainBindingsWhenMissing() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Map<String, Object> bindings = new LinkedHashMap<>();
            bindings.put("event", new LinkedHashMap<String, Object>() {{
                put("payload", new LinkedHashMap<String, Object>() {{
                    put("id", "evt-456");
                }});
            }});
            bindings.put("currentContract", new LinkedHashMap<String, Object>() {{
                put("channel", "test");
            }});

            ScriptRuntimeResult result = evaluator.evaluate(
                    "({ eventId: eventCanonical.payload.id, contractChannel: currentContractCanonical.channel })",
                    bindings,
                    BigInteger.valueOf(1000L));

            assertTrue(result.value() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) result.value();
            assertEquals("evt-456", String.valueOf(value.get("eventId")));
            assertEquals("test", String.valueOf(value.get("contractChannel")));
        }
    }

    @Test
    void rejectsUnsupportedBindingKeys() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            IllegalArgumentException thrown = assertThrows(
                    IllegalArgumentException.class,
                    () -> evaluator.evaluate(
                            "1",
                            new LinkedHashMap<String, Object>() {{
                                put("add", 1);
                            }},
                            BigInteger.valueOf(100L)));
            assertTrue(thrown.getMessage().contains("Unsupported QuickJS binding"));
        }
    }

    @Test
    void rejectsNonFunctionDocumentBinding() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            IllegalArgumentException thrown = assertThrows(
                    IllegalArgumentException.class,
                    () -> evaluator.evaluate(
                            "1",
                            new LinkedHashMap<String, Object>() {{
                                put("document", new LinkedHashMap<String, Object>());
                            }},
                            BigInteger.valueOf(100L)));
            assertTrue(thrown.getMessage().contains("document binding must be a function"));
        }
    }

    @Test
    void rejectsNonFunctionEmitBinding() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs evaluator tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            IllegalArgumentException thrown = assertThrows(
                    IllegalArgumentException.class,
                    () -> evaluator.evaluate(
                            "1",
                            new LinkedHashMap<String, Object>() {{
                                put("emit", Boolean.TRUE);
                            }},
                            BigInteger.valueOf(100L)));
            assertTrue(thrown.getMessage().contains("emit binding must be a function"));
        }
    }

    private boolean nodeAvailable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", "--version").start();
        int exit = process.waitFor();
        return exit == 0;
    }
}
