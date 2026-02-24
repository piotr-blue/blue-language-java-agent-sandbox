package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypedEventDslTest {

    @Test
    void emitsRealRepoTypesByDefault() {
        PayNoteTypes.CaptureFundsRequested event = new PayNoteTypes.CaptureFundsRequested();
        event.amount = 120;

        Node step = new StepsBuilder()
                .emit("EmitCaptureRequest", event)
                .build()
                .get(0);

        assertEquals(TypeAliases.CONVERSATION_TRIGGER_EVENT, step.getAsText("/type/value"));
        assertEquals("DvxKVEFsDmgA1hcBDfh7t42NgTRLaxXjCrB48DufP3i3",
                step.getAsText("/event/type/blueId"));
        assertEquals(120, step.getAsInteger("/event/amount/value").intValue());
    }

    @Test
    void adHocEventsUseSingleExplicitContainerType() {
        Node step = new StepsBuilder()
                .emitAdHocEvent("EmitAdHoc", "candidate-shortlisted", payload -> payload
                        .put("cvSessionId", "CV-123")
                        .put("score", 92))
                .build()
                .get(0);

        assertEquals(TypeAliases.COMMON_NAMED_EVENT, step.getAsText("/event/type/value"));
        assertEquals("candidate-shortlisted", step.getAsText("/event/name/value"));
        assertEquals("CV-123", step.getAsText("/event/payload/cvSessionId/value"));
        assertEquals(92, step.getAsInteger("/event/payload/score/value").intValue());
    }
}
