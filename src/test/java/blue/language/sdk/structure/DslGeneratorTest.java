package blue.language.sdk.structure;

import blue.language.model.Node;
import blue.language.sdk.DocBuilder;
import blue.language.sdk.MyOsPermissions;
import blue.language.sdk.paynote.PayNotes;
import blue.language.types.myos.Agent;
import blue.language.types.myos.SingleDocumentPermissionGranted;
import blue.language.types.myos.SubscriptionToSessionInitiated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DslGeneratorTest {

    @Test
    void counterDocIncludesOperationFieldAndSectionSyntax() {
        Node document = counterDoc();
        String dsl = DslGenerator.generate(document);

        assertTrue(dsl.contains(".operation(\"increment\")"));
        assertTrue(dsl.contains(".field(\"/counter\", 0)"));
        assertTrue(dsl.contains(".section("));
    }

    @Test
    void compositeChannelDocContainsCompositeChannelCall() {
        Node document = DocBuilder.doc()
                .name("Composite")
                .channels("a", "b")
                .compositeChannel("union", "a", "b")
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".compositeChannel(\"union\", \"a\", \"b\")"));
    }

    @Test
    void myOsAdminDocUsesMyOsAdminShortcut() {
        Node document = DocBuilder.doc()
                .name("MyOS admin")
                .myOsAdmin("myOsAdminChannel")
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".myOsAdmin(\"myOsAdminChannel\")"));
        assertFalse(dsl.contains("myOsAdminUpdateImpl"));
    }

    @Test
    void onInitAndOnDocChangeDocContainsWorkflowCalls() {
        Node document = DocBuilder.doc()
                .name("Workflow doc")
                .onInit("init", steps -> steps.replaceValue("Ready", "/status", "ready"))
                .onDocChange("onPrice", "/price", steps -> steps.replaceValue("Mark", "/changed", true))
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".onInit(\"init\""));
        assertTrue(dsl.contains(".onChannelEvent(\"onPrice\"") || dsl.contains("onPrice"));
    }

    @Test
    void aiIntegrationContainsMyOsResponseAndSubscriptionWorkflowCalls() {
        Node document = aiDoc();
        String dsl = DslGenerator.generate(document);

        assertTrue(dsl.contains(".onMyOsResponse(\"onLlmAccessGranted\""));
        assertTrue(dsl.contains(".onSubscriptionUpdate(\"onLlmUpdate\""));
    }

    @Test
    void payNoteSimpleCaptureContainsPayNoteEntryPointAndCaptureFlow() {
        Node document = PayNotes.payNote("Simple capture")
                .currency("USD")
                .amountMinor(1000)
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("unlockCapture", "payerChannel", "unlock")
                    .requestOnOperation("requestCapture", "guarantorChannel", "request")
                    .done()
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains("PayNotes.payNote(\"Simple capture\")"));
        assertTrue(dsl.contains(".capture()"));
        assertTrue(dsl.contains(".lockOnInit()"));
    }

    @Test
    void payNoteReserveAndCaptureDocContainsReserveAndCaptureBlocks() {
        Node document = PayNotes.payNote("Milestone")
                .currency("USD")
                .amountMinor(1000)
                .reserve().requestOnInit().done()
                .capture().requestOnInit().done()
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".reserve()"));
        assertTrue(dsl.contains(".capture()"));
    }

    @Test
    void documentWithJsStepContainsJsRawCall() {
        Node document = DocBuilder.doc()
                .name("JS doc")
                .onInit("init", steps -> steps.jsRaw("Compute", "return { value: 1 };"))
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".jsRaw(\"Compute\""));
        assertTrue(dsl.contains("return { value: 1 };"));
    }

    @Test
    void directChangeDocumentContainsDirectChangeCall() {
        Node document = DocBuilder.doc()
                .name("Direct change")
                .channel("ownerChannel")
                .directChange("applyPatch", "ownerChannel", "Apply changes")
                .buildDocument();

        String dsl = DslGenerator.generate(document);
        assertTrue(dsl.contains(".directChange(\"applyPatch\""));
    }

    @Test
    void emptyDocumentProducesMinimalDsl() {
        String dsl = DslGenerator.generate(new Node());
        assertNotNull(dsl);
        assertTrue(dsl.contains("DocBuilder.doc()"));
        assertTrue(dsl.contains(".buildDocument();"));
    }

    private static Node counterDoc() {
        return DocBuilder.doc()
                .name("Counter")
                .set("/counter", 0)
                .channel("ownerChannel")
                .operation("increment")
                .channel("ownerChannel")
                .description("Increment counter")
                .requestType(Integer.class)
                .steps(steps -> steps.replaceExpression("Inc", "/counter", "document('/counter') + 1"))
                .done()
                .buildDocument();
    }

    private static Node aiDoc() {
        return DocBuilder.doc()
                .name("AI Doc")
                .type(Agent.class)
                .set("/llmProviderSessionId", "session-llm-001")
                .set("/status", "idle")
                .channel("ownerChannel")
                .onInit("requestLlmAccess", steps -> steps.myOs().requestSingleDocPermission(
                        "ownerChannel",
                        "REQ_LLM",
                        DocBuilder.expr("document('/llmProviderSessionId')"),
                        MyOsPermissions.create().read(true)))
                .onMyOsResponse("onLlmAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_LLM",
                        steps -> steps.myOs().subscribeToSession(DocBuilder.expr("document('/llmProviderSessionId')"), "SUB_LLM"))
                .onSubscriptionUpdate("onLlmUpdate",
                        "SUB_LLM",
                        SubscriptionToSessionInitiated.class,
                        steps -> steps.replaceValue("SetReady", "/status", "ready"))
                .buildDocument();
    }
}
