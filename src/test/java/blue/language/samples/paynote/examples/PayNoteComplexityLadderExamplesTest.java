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
        Node shipment = PayNoteComplexityLadderExamples.tinyCaptureAfterShipmentOp();
        Node buyerApproval = PayNoteComplexityLadderExamples.tinyCaptureAfterBuyerApprovalOp();
        Node tracking = PayNoteComplexityLadderExamples.tinyCaptureAfterTrackingChange();
        Node event = PayNoteComplexityLadderExamples.tinyCaptureAfterEvent();
        Node reserveThenCapture = PayNoteComplexityLadderExamples.tinyReserveThenCaptureOnEvent();
        Node refund = PayNoteComplexityLadderExamples.tinyRefundOperation();
        Node release = PayNoteComplexityLadderExamples.tinyReleaseOperation();
        Node cancel = PayNoteComplexityLadderExamples.tinyCancellationOperation();

        assertEquals(PayNoteAliases.PAYNOTE, shipment.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_LOCK_REQUESTED,
                shipment.getAsText("/contracts/onInitCaptureLock/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                shipment.getAsText("/contracts/confirmShipmentImpl/steps/1/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                buyerApproval.getAsText("/contracts/approveCaptureImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                tracking.getAsText("/contracts/unlockCaptureOnDocChangeshippingtrackingNumber/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                event.getAsText("/contracts/unlockCaptureWhenDeliveryReported/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_FUNDS_REQUESTED,
                reserveThenCapture.getAsText("/contracts/captureWhenShipmentConfirmed/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                refund.getAsText("/contracts/requestRefundImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                release.getAsText("/contracts/releaseReservationImpl/steps/0/event/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                cancel.getAsText("/contracts/requestCancellation/type/value"));
    }

    @Test
    void step2MediumPayNoteSupportsCustomOperations() {
        Node medium = PayNoteComplexityLadderExamples.mediumShipmentEscrow();
        assertEquals(TypeAliases.CONVERSATION_OPERATION, medium.getAsText("/contracts/confirmShipment/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                medium.getAsText("/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                medium.getAsText("/contracts/confirmShipmentImpl/steps/2/event/type/value"));
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
