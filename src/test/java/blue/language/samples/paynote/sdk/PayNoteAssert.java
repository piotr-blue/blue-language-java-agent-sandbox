package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeRef;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PayNoteAssert {

    private final Node document;

    private PayNoteAssert(Node document) {
        this.document = document;
    }

    static PayNoteAssert assertThat(Node document) {
        return new PayNoteAssert(document);
    }

    PayNoteAssert isPayNoteDocument() {
        assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"));
        return this;
    }

    PayNoteAssert hasParticipant(String channelKey) {
        assertEquals("Conversation/Timeline Channel",
                document.getAsText("/contracts/" + channelKey + "/type/value"));
        return this;
    }

    PayNoteAssert hasOperationOnChannel(String operationKey, String channelKey) {
        assertEquals("Conversation/Operation",
                document.getAsText("/contracts/" + operationKey + "/type/value"));
        assertEquals(channelKey,
                document.getAsText("/contracts/" + operationKey + "/channel/value"));
        return this;
    }

    PayNoteAssert captureLocksOnInit() {
        assertTrue(contractStepsContainEventType("onInitCaptureLock", PayNoteAliases.CAPTURE_LOCK_REQUESTED),
                "Expected onInitCaptureLock workflow to emit capture lock requested");
        return this;
    }

    PayNoteAssert captureUnlocksViaOperation(String operationKey) {
        assertTrue(contractStepsContainEventType(operationKey + "Impl", PayNoteAliases.CAPTURE_UNLOCK_REQUESTED),
                "Expected " + operationKey + "Impl workflow to emit capture unlock requested");
        return this;
    }

    PayNoteAssert captureRequestsViaOperation(String operationKey) {
        assertTrue(contractStepsContainEventType(operationKey + "Impl", PayNoteAliases.CAPTURE_FUNDS_REQUESTED),
                "Expected " + operationKey + "Impl workflow to emit capture funds requested");
        return this;
    }

    PayNoteAssert workflowListensForEvent(String workflowKey, Class<?> eventTypeClass) {
        assertEquals(TypeRef.of(eventTypeClass).alias(),
                document.getAsText("/contracts/" + workflowKey + "/event/type/value"));
        return this;
    }

    PayNoteAssert workflowTriggersPaymentType(String workflowKey, Class<?> paymentEventTypeClass) {
        String expected = TypeRef.of(paymentEventTypeClass).alias();
        assertTrue(contractStepsContainEventType(workflowKey, expected),
                "Expected workflow " + workflowKey + " to trigger event type " + expected);
        return this;
    }

    private boolean contractStepsContainEventType(String contractKey, String eventTypeAlias) {
        Node stepsNode;
        try {
            stepsNode = document.getAsNode("/contracts/" + contractKey + "/steps");
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (stepsNode == null || stepsNode.getItems() == null) {
            return false;
        }
        List<Node> steps = stepsNode.getItems();
        for (Node step : steps) {
            if (step == null) {
                continue;
            }
            String type = safeText(step, "/event/type/value");
            if (eventTypeAlias.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private String safeText(Node node, String pointer) {
        try {
            return node.getAsText(pointer);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
