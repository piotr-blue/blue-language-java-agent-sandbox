package blue.language.processor;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for the slim handler context surface.
 */
final class ProcessorExecutionContextTest {

    @Test
    void documentHelpersExposeSnapshots() {
        Node document = new Node()
                .properties("value", new Node().value(1))
                .properties("nested", new Node().properties("inner", new Node().value("x")));

        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");

        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        Node snapshot = context.documentAt("/nested/inner");
        assertNotNull(snapshot);
        assertEquals("x", snapshot.getValue());

        Node missing = context.documentAt("/unknown");
        assertNull(missing);

        assertTrue(context.documentContains("/value"));
        assertFalse(context.documentContains("/value/missing"));

        // Ensure the returned node is a clone (mutation should not leak back).
        snapshot.value("mutated");
        Node reread = context.documentAt("/nested/inner");
        assertEquals("x", reread.getValue());
    }

    @Test
    void emitEventQueuesAndChargesGas() {
        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, new Node());
        execution.loadBundles("/");
        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        context.emitEvent(new Node().value("payload"));

        ScopeRuntimeContext scopeRuntime = execution.runtime().scope("/");
        assertEquals(1, scopeRuntime.triggeredQueue().size());
        assertTrue(execution.runtime().totalGas() >= 20L);
    }

    @Test
    void documentHelpersSupportEscapedAndListPointers() {
        Node document = new Node()
                .properties("a/b", new Node().value("slash"))
                .properties("list", new Node().items(
                        new Node().value("zero"),
                        new Node().value("one")))
                .properties("box", new Node().properties("01", new Node().value("leading-zero")));

        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");
        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        assertEquals("slash", context.documentAt("/a~1b").getValue());
        assertEquals("one", context.documentAt("/list/1").getValue());
        assertTrue(context.documentContains("/box/01"));
        assertFalse(context.documentContains("/list/01"));
    }

    @Test
    void documentHelpersRejectMalformedEscapedPointers() {
        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, new Node().properties("x", new Node().value("y")));
        execution.loadBundles("/");
        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        assertThrows(IllegalArgumentException.class, () -> context.documentAt("/x~"));
        assertThrows(IllegalArgumentException.class, () -> context.documentContains("/x~2"));
    }

    @Test
    void documentHelpersSupportTrailingEmptyPropertySegments() {
        Node document = new Node()
                .properties("scope", new Node().properties("", new Node().value("empty-key")));

        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");
        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        assertEquals("empty-key", context.documentAt("/scope/").getValue());
        assertTrue(context.documentContains("/scope/"));
    }

    @Test
    void documentHelpersPreferNumericPropertyOverListIndex() {
        Node document = new Node()
                .properties("list", new Node()
                        .items(new Node().value("index-zero"))
                        .properties("0", new Node().value("property-zero")));

        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");
        ProcessorExecutionContext context = execution.createContext("/", execution.bundleForScope("/"), new Node(), false, false);

        assertEquals("property-zero", context.documentAt("/list/0").getValue());
    }
}
