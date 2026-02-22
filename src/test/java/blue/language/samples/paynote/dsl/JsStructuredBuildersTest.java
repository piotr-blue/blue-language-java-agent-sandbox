package blue.language.samples.paynote.dsl;

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
}
