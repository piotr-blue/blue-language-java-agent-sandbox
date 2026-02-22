package blue.language.processor.script;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class QuickJsExpressionUtilsTest {

    @Test
    void expressionHelpersDetectAndExtractExpressions() {
        assertTrue(QuickJsExpressionUtils.isExpression("${value}"));
        assertFalse(QuickJsExpressionUtils.isExpression("${foo} + ${bar}"));
        assertFalse(QuickJsExpressionUtils.isExpression("plain text"));

        assertTrue(QuickJsExpressionUtils.containsExpression("${value}"));
        assertTrue(QuickJsExpressionUtils.containsExpression("hello ${value} world"));
        assertFalse(QuickJsExpressionUtils.containsExpression("hello world"));

        assertEquals("steps.answer", QuickJsExpressionUtils.extractExpressionContent("${steps.answer}"));
        assertThrows(IllegalArgumentException.class,
                () -> QuickJsExpressionUtils.extractExpressionContent("steps.answer"));
    }

    @Test
    void evaluatesExpressionsAndResolvesTemplates() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs expression tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Map<String, Object> bindings = new LinkedHashMap<>();
            Map<String, Object> steps = new LinkedHashMap<>();
            steps.put("value", 6);
            steps.put("factor", 7);
            bindings.put("steps", steps);
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("unit", "points");
            bindings.put("__documentData", document);

            Object result = QuickJsExpressionUtils.evaluateQuickJsExpression(
                    evaluator,
                    "steps.value * steps.factor",
                    bindings,
                    null);
            assertEquals("42", String.valueOf(result));

            String rendered = QuickJsExpressionUtils.resolveTemplateString(
                    evaluator,
                    "Hello ${steps.value}, total ${steps.value * steps.factor} ${document('/unit')}",
                    bindings,
                    null);
            assertEquals("Hello 6, total 42 points", rendered);

            String missing = QuickJsExpressionUtils.resolveTemplateString(
                    evaluator,
                    "Hello ${steps.missing}",
                    bindings,
                    null);
            assertEquals("Hello ", missing);
        }
    }

    @Test
    void resolveExpressionsUsesIncludeExcludePathPredicates() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs expression tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Node root = new Node()
                    .properties("keep", new Node().value("Value stays"))
                    .properties("direct", new Node().value("${steps.answer}"))
                    .properties("template", new Node().value("Total: ${steps.answer}"))
                    .properties("nested", new Node().items(Arrays.asList(
                            new Node().properties("flag", new Node().value("${steps.flag}")),
                            new Node().properties("flag", new Node().value("no substitution"))
                    )));

            Map<String, Object> bindings = new LinkedHashMap<>();
            Map<String, Object> steps = new LinkedHashMap<>();
            steps.put("answer", 42);
            steps.put("flag", "yes");
            bindings.put("steps", steps);

            final List<BigInteger> gasCharges = new ArrayList<>();
            QuickJsExpressionUtils.PointerPredicate shouldResolve = QuickJsExpressionUtils.createPathPredicate(
                    Arrays.asList("/direct", "/template", "/nested/**"),
                    Arrays.asList("/nested/1/**"));

            Node resolved = QuickJsExpressionUtils.resolveExpressions(
                    root,
                    evaluator,
                    bindings,
                    null,
                    shouldResolve,
                    null,
                    gasCharges::add);

            assertEquals("Value stays", resolved.getProperties().get("keep").getValue());
            assertEquals(new BigInteger("42"), resolved.getProperties().get("direct").getValue());
            assertEquals("Total: 42", resolved.getProperties().get("template").getValue());
            assertEquals("yes", resolved.getProperties().get("nested").getItems().get(0).getProperties().get("flag").getValue());
            assertEquals("no substitution", resolved.getProperties().get("nested").getItems().get(1).getProperties().get("flag").getValue());
            assertEquals("${steps.answer}", root.getProperties().get("direct").getValue());
            assertTrue(gasCharges.size() >= 2);
        }
    }

    @Test
    void wrapsEvaluationFailuresInCodeBlockEvaluationError() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs expression tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            CodeBlockEvaluationError error = assertThrows(
                    CodeBlockEvaluationError.class,
                    () -> QuickJsExpressionUtils.evaluateQuickJsExpression(
                            evaluator,
                            "invalid ?? expression",
                            new LinkedHashMap<String, Object>(),
                            null));
            assertTrue(error.getMessage().contains("Failed to evaluate code block"));
            assertTrue(error.getMessage().contains("invalid ?? expression"));
            assertEquals("invalid ?? expression", error.code());
        }
    }

    @Test
    void createPathPredicateSupportsIncludeExcludePatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/include/**"),
                Arrays.asList("/include/skip/**"));

        assertTrue(predicate.test("/include/path", null));
        assertFalse(predicate.test("/include/skip/here", null));
        assertFalse(predicate.test("/other", null));
    }

    @Test
    void createPathPredicateSupportsNoCaseAndNoGlobstarOptions() {
        QuickJsExpressionUtils.PointerPredicate nocasePredicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/include/**"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, true, false));
        assertTrue(nocasePredicate.test("/Include/Path", null));

        QuickJsExpressionUtils.PointerPredicate noGlobstarPredicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/include/**/item"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, true));
        assertTrue(noGlobstarPredicate.test("/include/path/item", null));
        assertFalse(noGlobstarPredicate.test("/include/path/deeper/item", null));
    }

    @Test
    void createPathPredicateDotOptionControlsHiddenSegments() {
        QuickJsExpressionUtils.PointerPredicate defaultPredicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/**"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));
        assertTrue(defaultPredicate.test("/.hidden", null));

        QuickJsExpressionUtils.PointerPredicate noDotPredicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/**"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(false, false, false));
        assertFalse(noDotPredicate.test("/.hidden", null));
        assertTrue(noDotPredicate.test("/visible", null));
    }

    @Test
    void createPathPredicateSupportsBraceExpansionPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/contracts/{primary,secondary}/**"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertTrue(predicate.test("/contracts/primary/channel", null));
        assertTrue(predicate.test("/contracts/secondary/channel", null));
        assertFalse(predicate.test("/contracts/tertiary/channel", null));
    }

    @Test
    void createPathPredicateSupportsNestedBraceExpansionPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/a/{b,{c,d}}/value"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertTrue(predicate.test("/a/b/value", null));
        assertTrue(predicate.test("/a/c/value", null));
        assertTrue(predicate.test("/a/d/value", null));
        assertFalse(predicate.test("/a/e/value", null));
    }

    @Test
    void createPathPredicateSupportsCharacterClassPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/contracts/[ab]/value"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertTrue(predicate.test("/contracts/a/value", null));
        assertTrue(predicate.test("/contracts/b/value", null));
        assertFalse(predicate.test("/contracts/c/value", null));
    }

    @Test
    void createPathPredicateSupportsNegatedCharacterClassPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/contracts/[!ab]/value"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertFalse(predicate.test("/contracts/a/value", null));
        assertFalse(predicate.test("/contracts/b/value", null));
        assertTrue(predicate.test("/contracts/c/value", null));
    }

    @Test
    void createPathPredicateSupportsAtExtglobPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/contracts/@(primary|secondary)/value"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertTrue(predicate.test("/contracts/primary/value", null));
        assertTrue(predicate.test("/contracts/secondary/value", null));
        assertFalse(predicate.test("/contracts/tertiary/value", null));
    }

    @Test
    void createPathPredicateSupportsOptionalExtglobPatterns() {
        QuickJsExpressionUtils.PointerPredicate predicate = QuickJsExpressionUtils.createPathPredicate(
                Arrays.asList("/contract?(s)/value"),
                null,
                new QuickJsExpressionUtils.PathMatchOptions(true, false, false));

        assertTrue(predicate.test("/contract/value", null));
        assertTrue(predicate.test("/contracts/value", null));
        assertFalse(predicate.test("/contractss/value", null));
    }

    @Test
    void resolveExpressionsHonorsShouldDescendPredicate() throws IOException, InterruptedException {
        assumeTrue(nodeAvailable(), "Node.js binary is required for quickjs expression tests");

        try (QuickJSEvaluator evaluator = new QuickJSEvaluator()) {
            Node root = new Node()
                    .properties("resolve", new Node().value("${steps.answer}"))
                    .properties("literal", new Node()
                            .properties("nested", new Node().value("${steps.answer}")));

            Map<String, Object> bindings = new LinkedHashMap<>();
            Map<String, Object> steps = new LinkedHashMap<>();
            steps.put("answer", 42);
            bindings.put("steps", steps);

            Node resolved = QuickJsExpressionUtils.resolveExpressions(
                    root,
                    evaluator,
                    bindings,
                    null,
                    QuickJsExpressionUtils.createPathPredicate(Arrays.asList("/**"), null),
                    new QuickJsExpressionUtils.PointerPredicate() {
                        @Override
                        public boolean test(String pointer, Node node) {
                            return !"/literal".equals(pointer);
                        }
                    },
                    null);

            assertEquals(new BigInteger("42"), resolved.getProperties().get("resolve").getValue());
            assertEquals("${steps.answer}", resolved.getProperties().get("literal").getProperties().get("nested").getValue());
        }
    }

    private boolean nodeAvailable() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", "--version").start();
        int exit = process.waitFor();
        return exit == 0;
    }
}
