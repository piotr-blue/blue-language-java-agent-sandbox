package blue.language.samples.paynote.sdk.v2;

import blue.language.model.Node;
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
}
