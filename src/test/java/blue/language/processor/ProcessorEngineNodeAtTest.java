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
    void nodeAtSupportsArrayTraversalWithStrictIndexSemantics() {
        Node root = new Node()
                .properties("list", new Node()
                        .items(
                                new Node().value("zero"),
                                new Node().value("one")
                        ));

        assertEquals("one", ProcessorEngine.nodeAt(root, "/list/1").getValue());
        assertNull(ProcessorEngine.nodeAt(root, "/list/01"));
    }

    @Test
    void nodeAtPrefersNumericPropertyOverArrayIndex() {
        Node root = new Node()
                .properties("list", new Node()
                        .items(new Node().value("index-zero"))
                        .properties("0", new Node().value("property-zero")));

        assertEquals("property-zero", ProcessorEngine.nodeAt(root, "/list/0").getValue());
    }

    @Test
    void nodeAtPreservesTrailingEmptySegments() {
        Node root = new Node().properties("scope", new Node().value("value"));
        assertNull(ProcessorEngine.nodeAt(root, "/scope/"));
    }

    @Test
    void nodeAtResolvesTrailingEmptySegmentWhenPropertyExists() {
        Node root = new Node().properties("scope", new Node()
                .properties("", new Node().value("empty-key")));

        assertEquals("empty-key", ProcessorEngine.nodeAt(root, "/scope/").getValue());
    }

    @Test
    void nodeAtRejectsMalformedEscapesAndPointers() {
        Node root = new Node().properties("x", new Node().value("y"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "/x~"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "/x~2"));
        assertThrows(IllegalArgumentException.class, () -> ProcessorEngine.nodeAt(root, "x"));
    }
}
