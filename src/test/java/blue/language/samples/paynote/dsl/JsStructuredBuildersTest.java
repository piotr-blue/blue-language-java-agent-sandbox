package blue.language.samples.paynote.dsl;

import blue.language.samples.paynote.types.domain.ShippingEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsStructuredBuildersTest {

    @Test
    void buildsPatchAndOutputPayloadWithoutLargeManualStrings() {
        String changeset = JsPatchBuilder.patch()
                .replaceValue("/processingStatus", "status")
                .removePath("/pendingClassification/requestId")
                .build();

        String output = JsOutputBuilder.output()
                .changesetRaw(changeset)
                .eventsArray(JsArrayBuilder.array()
                        .itemObject(JsObjectBuilder.object()
                                .propString("type", TypeAliases.CONVERSATION_EVENT)
                                .propString("message", "CV updated")))
                .build();

        assertTrue(changeset.contains("/processingStatus"));
        assertTrue(changeset.contains("remove"));
        assertTrue(output.contains("changeset"));
        assertTrue(output.contains(TypeAliases.CONVERSATION_EVENT));
    }

    @Test
    void supportsIfElseCompositionInJsProgramBuilder() {
        JsProgram program = BlueDocDsl.js(js -> js
                .constVar("shouldAlert", "true")
                .ifElseBlock("shouldAlert",
                        thenBranch -> thenBranch.returnStatement("{ events: [1] }"),
                        elseBranch -> elseBranch.returnStatement("{ events: [] }")));

        String code = program.code();
        assertTrue(code.contains("if (shouldAlert) {"));
        assertTrue(code.contains("} else {"));
        assertTrue(code.contains("events"));
    }

    @Test
    void supportsRequestCoalesceSafeNumberAndTypedEventsHelpers() {
        JsProgram program = BlueDocDsl.js(js -> js
                .readRequest("request")
                .constVar("status", JsCommon.coalesce("request.status", "document('/status')", "'pending'"))
                .safeNumber("amountMinor", "request.amountMinor", "0")
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw(JsPatchBuilder.changeset()
                                .replaceExpression("/status", "status")
                                .replaceExpression("/amountMinor", "amountMinor")
                                .build())
                        .eventsArray(JsArrayBuilder.array()
                                .itemObject(JsCommon.typedEvent(ShippingEvents.ShipmentConfirmed.class,
                                        event -> event.propString("source", "shipmentCompanyChannel"))))));

        String code = program.code();
        assertTrue(code.contains("event.message?.request ?? {}"));
        assertTrue(code.contains("request.status ?? document('/status') ?? 'pending'"));
        assertTrue(code.contains("Number.isFinite"));
        assertTrue(code.contains("Shipping/Shipment Confirmed"));
    }

    @Test
    void generatedProgramsRemainDeterministic() {
        JsProgram program = BlueDocDsl.js(js -> js
                .readRequest("request")
                .constVar("id", JsCommon.coalesce("request.id", "'fallback-id'"))
                .returnOutput(JsOutputBuilder.output().emptyEvents()));

        String code = program.code();
        assertTrue(!code.contains("Date.now"));
        assertTrue(!code.contains("Math.random"));
        assertTrue(!code.contains("new Date"));
    }
}
