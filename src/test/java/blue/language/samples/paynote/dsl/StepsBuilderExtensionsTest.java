package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.MyOsPermissions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StepsBuilderExtensionsTest {

    @Test
    void myOsShortcutEmitsTypedPermissionAndSubscriptionEvents() {
        StepsBuilder steps = new StepsBuilder();

        steps.myOs().requestSingleDocPermission(
                        "recruitmentChannel",
                        "REQ_PROVIDER",
                        BlueDocDsl.expr("document('/llmProviderSessionId')"),
                        MyOsPermissions.create().read(true).singleOps("provideInstructions"))
                .myOs().subscribeToSession(
                        BlueDocDsl.expr("document('/providerSessionId')"),
                        "SUB_PROVIDER");

        List<Node> built = steps.build();
        assertEquals(2, built.size());

        Node requestPermission = built.get(0);
        assertEquals(TypeAliases.CONVERSATION_TRIGGER_EVENT, requestPermission.getAsText("/type/value"));
        assertEquals(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED,
                requestPermission.getAsText("/event/type/value"));
        assertEquals("recruitmentChannel", requestPermission.getAsText("/event/onBehalfOf/value"));
        assertEquals("REQ_PROVIDER", requestPermission.getAsText("/event/requestId/value"));
        assertEquals("${document('/llmProviderSessionId')}",
                requestPermission.getAsText("/event/targetSessionId/value"));
        assertEquals("provideInstructions",
                requestPermission.getAsText("/event/permissions/singleOps/0/value"));

        Node subscribeToSession = built.get(1);
        assertEquals(TypeAliases.MYOS_SUBSCRIBE_TO_SESSION_REQUESTED,
                subscribeToSession.getAsText("/event/type/value"));
        assertEquals("${document('/providerSessionId')}",
                subscribeToSession.getAsText("/event/targetSessionId/value"));
        assertEquals("SUB_PROVIDER", subscribeToSession.getAsText("/event/subscription/id/value"));
    }

    @Test
    void myOsNamespaceCoversLinkedDocsParticipantsOperationAndWorkerStart() {
        StepsBuilder steps = new StepsBuilder();

        steps.myOs().requestLinkedDocsPermission(
                        "recruitmentChannel",
                        "REQ_RECRUITMENT_CVS",
                        BlueDocDsl.expr("document('/recruitmentSessionId')"),
                        Map.of("cvs", MyOsPermissions.create().read(true).allOps(true)))
                .myOs().addParticipant("observerChannel", "observer@example.com")
                .myOs().removeParticipant("observerChannel")
                .myOs().callOperation(
                        "recruitmentChannel",
                        "session-123",
                        "provideInstructions",
                        new Node().properties("prompt", new Node().value("Classify CV")))
                .myOs().startWorkerSession(
                        "recruitmentChannel",
                        new Node().properties("kind", new Node().value("classifier")));

        List<Node> built = steps.build();
        assertEquals(5, built.size());

        assertEquals(TypeAliases.MYOS_LINKED_DOCUMENTS_PERMISSION_GRANT_REQUESTED,
                built.get(0).getAsText("/event/type/value"));
        assertEquals("REQ_RECRUITMENT_CVS", built.get(0).getAsText("/event/requestId/value"));
        assertEquals(Boolean.TRUE, built.get(0).get("/event/links/cvs/read"));
        assertEquals(Boolean.TRUE, built.get(0).get("/event/links/cvs/allOps"));

        assertEquals(TypeAliases.MYOS_ADDING_PARTICIPANT_REQUESTED, built.get(1).getAsText("/event/type/value"));
        assertEquals("observer@example.com", built.get(1).getAsText("/event/email/value"));

        assertEquals(TypeAliases.MYOS_REMOVING_PARTICIPANT_REQUESTED, built.get(2).getAsText("/event/type/value"));
        assertEquals("observerChannel", built.get(2).getAsText("/event/channelKey/value"));

        assertEquals(TypeAliases.MYOS_CALL_OPERATION_REQUESTED, built.get(3).getAsText("/event/type/value"));
        assertEquals("provideInstructions", built.get(3).getAsText("/event/operation/value"));
        assertEquals("Classify CV", built.get(3).getAsText("/event/request/prompt/value"));

        assertEquals(TypeAliases.MYOS_START_WORKER_SESSION_REQUESTED, built.get(4).getAsText("/event/type/value"));
        assertEquals("recruitmentChannel", built.get(4).getAsText("/event/agentChannelKey/value"));
        assertEquals("classifier", built.get(4).getAsText("/event/config/kind/value"));
    }

    @Test
    void extSupportsThirdPartyNamespacePattern() {
        StepsBuilder steps = new StepsBuilder();

        steps.ext(DemoBankSteps::new).holdFunds(10_000, "USD")
                .replaceValue("MarkReady", "/status", "ready");

        List<Node> built = steps.build();
        assertEquals(2, built.size());
        assertEquals(TypeAliases.COMMON_NAMED_EVENT, built.get(0).getAsText("/event/type/value"));
        assertEquals("demoBank-hold-funds", built.get(0).getAsText("/event/name/value"));
        assertEquals(10_000, built.get(0).getAsInteger("/event/payload/amountMinor/value").intValue());
        assertEquals("USD", built.get(0).getAsText("/event/payload/currency/value"));
        assertEquals(TypeAliases.CONVERSATION_UPDATE_DOCUMENT, built.get(1).getAsText("/type/value"));
    }

    private static final class DemoBankSteps {
        private final StepsBuilder parent;

        private DemoBankSteps(StepsBuilder parent) {
            this.parent = parent;
        }

        private StepsBuilder holdFunds(long amountMinor, String currency) {
            return parent.emitAdHocEvent("HoldFunds", "demoBank-hold-funds", payload -> payload
                    .put("amountMinor", amountMinor)
                    .put("currency", currency));
        }
    }
}
