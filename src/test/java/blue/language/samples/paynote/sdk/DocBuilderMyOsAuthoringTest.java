package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocBuilderMyOsAuthoringTest {

    @Test
    void supportsMyOsNamespaceStyleAuthoringFromSimpleDocBuilder() {
        Node document = SimpleDocBuilder.name("CV Classifier Agent")
                .type("MyOS/Agent")
                .description("Classifies linked CVs via llm-provider.")
                .participant("recruitmentChannel")
                .myOsAdmin("myOsAdminChannel")
                .onInit("requestAccess", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_PROVIDER",
                                SimpleDocBuilder.expr("document('/llmProviderSessionId')"),
                                MyOsPermissions.create()
                                        .read(true)
                                        .singleOps("provideInstructions"))
                        .myOs().requestLinkedDocsPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_CVS",
                                SimpleDocBuilder.expr("document('/recruitmentSessionId')"),
                                Map.of("cvs", MyOsPermissions.create()
                                        .read(true)
                                        .allOps(true))))
                .onMyOsResponse("onLlmProviderAccessGranted",
                        MyOsTypes.SingleDocumentPermissionGranted.class,
                        "REQ_RECRUITMENT_PROVIDER",
                        steps -> steps.myOs().subscribeToSession(
                                SimpleDocBuilder.expr("document('/llmProviderSessionId')"),
                                "SUB_RECRUITMENT_PROVIDER"))
                .buildDocument();

        assertEquals("MyOS/Agent", document.getAsText("/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/recruitmentChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/myOsAdminUpdate/type/value"));
        assertEquals(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED,
                document.getAsText("/contracts/requestAccess/steps/0/event/type/value"));
        assertEquals(TypeAliases.MYOS_LINKED_DOCUMENTS_PERMISSION_GRANT_REQUESTED,
                document.getAsText("/contracts/requestAccess/steps/1/event/type/value"));
        assertEquals(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANTED,
                document.getAsText("/contracts/onLlmProviderAccessGranted/event/type/value"));
        assertEquals("REQ_RECRUITMENT_PROVIDER",
                document.getAsText("/contracts/onLlmProviderAccessGranted/event/requestId/value"));
        assertEquals(TypeAliases.MYOS_SUBSCRIBE_TO_SESSION_REQUESTED,
                document.getAsText("/contracts/onLlmProviderAccessGranted/steps/0/event/type/value"));
    }
}
