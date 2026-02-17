package blue.language.processor;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentProcessingRuntimeDirectWriteTest {

    @Test
    void directWriteRejectsInvalidPointerPaths() {
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(new Node());
        Node value = new Node().value("x");

        assertThrows(IllegalArgumentException.class, () -> runtime.directWrite(null, value));
        assertThrows(IllegalArgumentException.class, () -> runtime.directWrite("", value));
        assertThrows(IllegalArgumentException.class, () -> runtime.directWrite("x", value));
        assertThrows(IllegalArgumentException.class, () -> runtime.directWrite("/x~2", value));
    }

    @Test
    void directWriteRejectsRootPointer() {
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(new Node());
        assertThrows(IllegalArgumentException.class, () -> runtime.directWrite("/", new Node().value("x")));
    }

    @Test
    void directWriteSupportsEscapedPointerSegments() {
        Node document = new Node();
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(document);

        runtime.directWrite("/a~1b", new Node().value("slash"));
        runtime.directWrite("/a~0b", new Node().value("tilde"));

        assertEquals("slash", document.getProperties().get("a/b").getValue());
        assertEquals("tilde", document.getProperties().get("a~b").getValue());
    }
}
