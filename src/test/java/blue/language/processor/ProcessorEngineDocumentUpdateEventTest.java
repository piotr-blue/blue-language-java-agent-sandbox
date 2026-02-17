package blue.language.processor;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProcessorEngineDocumentUpdateEventTest {

    @Test
    void createDocumentUpdateEventRelativizesPathAndClonesBeforeAfterNodes() {
        Node before = new Node().properties("x", new Node().value("before"));
        Node after = new Node().properties("x", new Node().value("after"));
        DocumentProcessingRuntime.DocumentUpdateData data = new DocumentProcessingRuntime.DocumentUpdateData(
                "/scope/mixed/01",
                before,
                after,
                JsonPatch.Op.REPLACE,
                "/scope",
                Collections.singletonList("/scope")
        );

        Node event = ProcessorEngine.createDocumentUpdateEvent(data, "/scope");

        assertEquals("Document Update", event.getProperties().get("type").getValue());
        assertEquals("replace", event.getProperties().get("op").getValue());
        assertEquals("/mixed/01", event.getProperties().get("path").getValue());
        assertEquals("before", event.getProperties().get("before").getProperties().get("x").getValue());
        assertEquals("after", event.getProperties().get("after").getProperties().get("x").getValue());

        // Event payloads should be clones, not original references.
        event.getProperties().get("before").getProperties().put("x", new Node().value("mutated"));
        event.getProperties().get("after").getProperties().put("x", new Node().value("mutated"));
        assertEquals("before", before.getProperties().get("x").getValue());
        assertEquals("after", after.getProperties().get("x").getValue());
    }

    @Test
    void createDocumentUpdateEventUsesExplicitNullNodesWhenBeforeAfterMissing() {
        DocumentProcessingRuntime.DocumentUpdateData data = new DocumentProcessingRuntime.DocumentUpdateData(
                "/scope/a",
                null,
                null,
                JsonPatch.Op.ADD,
                "/scope",
                Collections.singletonList("/scope")
        );

        Node event = ProcessorEngine.createDocumentUpdateEvent(data, "/scope");
        assertEquals(null, event.getProperties().get("before").getValue());
        assertEquals(null, event.getProperties().get("after").getValue());
    }

    @Test
    void matchesDocumentUpdatePreservesLeadingZeroSegmentSemantics() {
        assertTrue(ProcessorEngine.matchesDocumentUpdate("/scope", "/mixed/01", "/scope/mixed/01/value"));
        assertFalse(ProcessorEngine.matchesDocumentUpdate("/scope", "/mixed/01", "/scope/mixed/1/value"));
        assertFalse(ProcessorEngine.matchesDocumentUpdate("/scope", "/mixed/1", "/scope/mixed/01/value"));
    }

    @Test
    void matchesDocumentUpdateTreatsRootWatchAsMatchAllAndRejectsInvalidPointers() {
        assertTrue(ProcessorEngine.matchesDocumentUpdate("/scope", "/", "/scope/any/path"));
        assertFalse(ProcessorEngine.matchesDocumentUpdate("/scope", null, "/scope/any/path"));
        assertFalse(ProcessorEngine.matchesDocumentUpdate("/scope", "", "/scope/any/path"));
        assertThrows(IllegalArgumentException.class,
                () -> ProcessorEngine.matchesDocumentUpdate("/scope", "mixed/01", "/scope/mixed/01"));
        assertThrows(IllegalArgumentException.class,
                () -> ProcessorEngine.matchesDocumentUpdate("/scope", "/mixed/01", "scope/mixed/01"));
    }
}
