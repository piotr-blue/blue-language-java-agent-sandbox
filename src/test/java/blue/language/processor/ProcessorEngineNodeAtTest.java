package blue.language.processor;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProcessorEngineNodeAtTest {

    @Test
    void nodeAtResolvesEscapedPointerSegments() {
        Node root = new Node()
                .properties("a/b", new Node().value("slash"))
                .properties("a~b", new Node().value("tilde"));

        assertEquals("slash", ProcessorEngine.nodeAt(root, "/a~1b").getValue());
        assertEquals("tilde", ProcessorEngine.nodeAt(root, "/a~0b").getValue());
    }

    @Test
    void nodeAtPreservesTrailingEmptySegments() {
        Node root = new Node().properties("scope", new Node().value("value"));
        assertNull(ProcessorEngine.nodeAt(root, "/scope/"));
    }

    @Test
    void nodeAtRejectsMalformedEscapesAndPointers() {
        Node root = new Node().properties("x", new Node().value("y"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "/x~"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "/x~2"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "x"));
    }
}
