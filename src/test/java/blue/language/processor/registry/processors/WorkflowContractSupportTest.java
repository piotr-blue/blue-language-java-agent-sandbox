package blue.language.processor.registry.processors;

import blue.language.NodeProvider;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowContractSupportTest {

    @Test
    void matchesEventFilterResolvesProviderBackedTypeChains() {
        Node event = new Node()
                .type(new Node().blueId("Custom/Derived Event"))
                .properties("kind", new Node().value("alpha"));
        Node filter = new Node()
                .type(new Node().blueId("Custom/Base Event"))
                .properties("kind", new Node().value("alpha"));

        NodeProvider provider = blueId -> {
            if (!"Custom/Derived Event".equals(blueId)) {
                return Collections.emptyList();
            }
            Node definition = new Node();
            definition.type(new Node().blueId("Custom/Base Event"));
            return Collections.singletonList(definition);
        };

        assertFalse(WorkflowContractSupport.matchesEventFilter(event, filter));
        assertTrue(WorkflowContractSupport.matchesEventFilter(event, filter, provider));
    }

    @Test
    void matchesTypeRequirementResolvesProviderBackedEntryTypeChains() {
        Node candidate = new Node().properties("payload",
                new Node().type(new Node().blueId("Custom/Derived Payload")).value("ok"));
        Node requirement = new Node()
                .type(new Node().blueId("Dictionary"))
                .properties("entries",
                        new Node().properties("payload",
                                new Node().type(new Node().blueId("Custom/Base Payload"))));

        NodeProvider provider = blueId -> {
            if (!"Custom/Derived Payload".equals(blueId)) {
                return Collections.emptyList();
            }
            Node definition = new Node();
            definition.type(new Node().blueId("Custom/Base Payload"));
            return Collections.singletonList(definition);
        };

        assertFalse(WorkflowContractSupport.matchesTypeRequirement(candidate, requirement));
        assertTrue(WorkflowContractSupport.matchesTypeRequirement(candidate, requirement, provider));
    }
}
