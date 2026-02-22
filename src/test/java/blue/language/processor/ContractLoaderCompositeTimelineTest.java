package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractLoaderCompositeTimelineTest {

    @Test
    void rejectsCompositeTimelineWhenReferencedChildMissing() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  compositeA:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - missingChild\n");

        assertThrows(IllegalStateException.class, () -> loader.load(scope, "/"));
    }

    @Test
    void rejectsCompositeTimelineSelfCycle() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  compositeA:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - compositeA\n");

        assertThrows(IllegalStateException.class, () -> loader.load(scope, "/"));
    }

    @Test
    void rejectsCompositeTimelineReferenceCycle() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  compositeA:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - compositeB\n" +
                "  compositeB:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - compositeA\n");

        assertThrows(IllegalStateException.class, () -> loader.load(scope, "/"));
    }

    @Test
    void acceptsAcyclicCompositeTimelineReferences() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  child:\n" +
                "    type:\n" +
                "      blueId: Conversation/Timeline Channel\n" +
                "    timelineId: alpha\n" +
                "  compositeA:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - child\n");

        assertDoesNotThrow(() -> loader.load(scope, "/"));
    }

    @Test
    void acceptsNestedAcyclicCompositeTimelineReferences() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  compositeA:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - compositeB\n" +
                "  compositeB:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels:\n" +
                "      - compositeC\n" +
                "  compositeC:\n" +
                "    type:\n" +
                "      blueId: Conversation/Composite Timeline Channel\n" +
                "    channels: []\n");

        assertDoesNotThrow(() -> loader.load(scope, "/"));
    }
}
