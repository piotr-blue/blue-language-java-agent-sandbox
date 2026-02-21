package blue.language.samples.paynote.counter;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.ResolvedSnapshot;
import blue.language.snapshot.WorkingDocument;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CounterJavaAuthoringDemoTest {

    private final Blue blue = new Blue();

    @Test
    void authorsCounterInJavaAndAttachesExtraContracts() {
        CounterDocument counter = CounterBuilder.baseline("timeline-demo-002");
        CounterBuilder.withExtensions(counter, "ownerChannel");

        Node node = blue.objectToNode(counter);
        Node contracts = node.getProperties().get("contracts");

        assertNotNull(contracts);
        assertNotNull(contracts.getProperties().get("increment"));
        assertNotNull(contracts.getProperties().get("decrement"));
        assertNotNull(contracts.getProperties().get("counterChanged"));
        assertNotNull(contracts.getProperties().get("onCounterChanged"));
        assertNotNull(contracts.getProperties().get("say"));
        assertNotNull(contracts.getProperties().get("sayImpl"));

        Blue resolvingBlue = new Blue(blueId -> {
            return Collections.singletonList(new Node().name("Demo Type " + blueId));
        });
        ResolvedSnapshot snapshot = resolvingBlue.resolveToSnapshot(node);
        WorkingDocument workingDocument = WorkingDocument.forSnapshot(resolvingBlue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/counter", new Node().value(9)));
        ResolvedSnapshot committed = workingDocument.commit();

        assertEquals(Integer.valueOf(9), committed.resolvedRoot().toNode().getAsInteger("/counter/value"));
        assertEquals("/counter", committed.resolvedRoot().toNode()
                .getProperties()
                .get("contracts")
                .getProperties()
                .get("counterChanged")
                .getProperties()
                .get("path")
                .getValue());
    }
}
