package blue.language.samples.paynote.sdk.v2;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteOverlayTest {

    @Test
    void buildsTypedPayNotePoliciesWithoutStringlyEventConstruction() {
        Node payNote = PayNoteOverlay.payNote("Overlay Macro Test", 150, "EUR")
                .abstractParticipants()
                .reserve(150)
                .captureOnEvent("captureOnFundsReserved", PayNoteTypes.FundsReserved.class, 150)
                .once("captureOnlyOnce", PayNoteTypes.FundsReserved.class, "/flags/capturedOnce", steps -> steps
                        .replaceValue("MarkMacroRan", "/flags/onceMacroRan", true))
                .barrier("waitForTwoSignals",
                        CommonTypes.NamedEvent.class,
                        "/barriers/twoSignals",
                        "event.name",
                        PayNoteOverlay.defaultBarrierSignals("a", "b"),
                        steps -> steps.replaceValue("MarkSatisfied", "/flags/barrierSatisfied", true))
                .build();

        assertEquals(TypeAliases.PAYNOTE_DOCUMENT, payNote.getAsText("/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL, payNote.getAsText("/contracts/payerChannel/type/value"));
        assertEquals(TypeAliases.CORE_TRIGGERED_EVENT_CHANNEL,
                payNote.getAsText("/contracts/triggeredEventChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW,
                payNote.getAsText("/contracts/captureOnFundsReserved/type/value"));
        assertEquals("AopfdGqnwcxsw4mJzXbmjDMnASRtkce9BZB1n6QSRNXX",
                payNote.getAsText("/contracts/captureOnFundsReserved/event/type/blueId"));

        String onceGuardCode = payNote.getAsText("/contracts/captureOnlyOnce/steps/0/code/value");
        assertTrue(onceGuardCode.contains("alreadyDone"));

        String barrierCode = payNote.getAsText("/contracts/waitForTwoSignals/steps/0/code/value");
        assertTrue(barrierCode.contains("requiredSignals"));
        assertTrue(barrierCode.contains("barrier-satisfied"));
    }

    @Test
    void supportsCancellationAndReleaseMacrosForRealisticEscrowFlow() {
        Node payNote = PayNoteOverlay.payNote("Cancellation Flow", 210, "EUR")
                .abstractParticipants()
                .reserve(210)
                .requestCancellationOperation()
                .releaseOnEvent("releaseAfterApproval", CommonTypes.NamedEvent.class, 210)
                .build();

        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                payNote.getAsText("/contracts/requestCancellation/type/value"));
        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION,
                payNote.getAsText("/contracts/requestCancellationImpl/type/value"));
        assertEquals("cancellation-requested",
                payNote.getAsText("/contracts/requestCancellationImpl/steps/0/changeset/0/val/value"));

        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW,
                payNote.getAsText("/contracts/releaseAfterApproval/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED,
                payNote.getAsText("/contracts/releaseAfterApproval/steps/0/event/type/value"));
    }

    @Test
    void providesStandardParticipantAndShipmentFlowMacros() {
        Node payNote = PayNoteOverlay.payNote("Shipment Macro", 800, "USD")
                .standardParticipantChannels()
                .reserveOnInit(800)
                .confirmShipmentUnlocksCapture(800)
                .build();

        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                payNote.getAsText("/contracts/shipmentCompanyChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                payNote.getAsText("/contracts/allParticipantsChannel/type/value"));
        assertEquals(TypeAliases.CORE_LIFECYCLE_EVENT_CHANNEL,
                payNote.getAsText("/contracts/initLifecycleChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                payNote.getAsText("/contracts/confirmShipment/type/value"));
        assertEquals(PayNoteAliases.RESERVE_FUNDS_REQUESTED,
                payNote.getAsText("/contracts/onInitReserveFunds/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_FUNDS_REQUESTED,
                payNote.getAsText("/contracts/confirmShipmentImpl/steps/1/event/type/value"));
    }
}
