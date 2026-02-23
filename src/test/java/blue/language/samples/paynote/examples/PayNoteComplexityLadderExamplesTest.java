package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteComplexityLadderExamplesTest {

    @Test
    void step1TinyPayNotesProvideUsefulDefaults() {
        Node lock = PayNoteComplexityLadderExamples.simpleCardLock();
        Node reserveCapture = PayNoteComplexityLadderExamples.simpleReserveAndCapture();
        Node refund = PayNoteComplexityLadderExamples.simpleRefundOperation();

        assertEquals(PayNoteAliases.PAYNOTE, lock.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED,
                lock.getAsText("/contracts/onInitLockCardCapture/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVE_FUNDS_AND_CAPTURE_IMMEDIATELY_REQUESTED,
                reserveCapture.getAsText("/contracts/onInitReserveAndCapture/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                refund.getAsText("/contracts/refundFullImpl/steps/0/event/type/value"));
    }

    @Test
    void step2MediumPayNoteSupportsCustomOperations() {
        Node medium = PayNoteComplexityLadderExamples.mediumShipmentEscrow();
        assertEquals(TypeAliases.CONVERSATION_OPERATION, medium.getAsText("/contracts/confirmShipment/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                medium.getAsText("/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED,
                medium.getAsText("/contracts/unlockCardCaptureWhenEvent/steps/0/event/type/value"));
        assertEquals("/shipping/trackingNumber",
                medium.getAsText("/policies/changesetAllowList/directChange/1/value"));
    }

    @Test
    void step3HugeJsExampleContainsHundredPlusLines() {
        Node huge = PayNoteComplexityLadderExamples.hugeJsRiskReview();
        String code = huge.getAsText("/contracts/riskReviewImpl/steps/0/code/value");
        int lineCount = code.split("\\n").length;
        assertTrue(lineCount >= 100, "expected at least 100 JS lines but got " + lineCount);
        assertTrue(code.contains("const factor104"));
        assertTrue(code.contains("aggregateScore"));
    }

    @Test
    void myPayNoteCanBeDefinedThenExtended() {
        Node base = MyPayNote.baseDocument("My Base PayNote");
        Node extended = MyPayNote.withExtraOperations("My Extended PayNote");

        assertEquals(PayNoteAliases.PAYNOTE, base.getAsText("/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                base.getAsText("/contracts/requestCancellation/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                extended.getAsText("/contracts/supportNote/type/value"));
        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION,
                extended.getAsText("/contracts/supportNoteImpl/type/value"));
    }
}
