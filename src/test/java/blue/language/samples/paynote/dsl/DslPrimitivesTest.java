package blue.language.samples.paynote.dsl;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DslPrimitivesTest {

    @Test
    void buildsDocumentSessionBootstrapWithReadableDsl() {
        Node bootstrap = BlueDocDsl.documentSessionBootstrap()
                .documentName("Candidate CV B - 2026-02-21T00:00:00Z")
                .putDocumentValue("processingStatus", "pending")
                .putDocumentObject("cv", cv -> cv
                        .put("name", "Mia Zielinska")
                        .put("experience", "QA and automation profile"))
                .contracts(c -> c
                        .timelineChannel("ownerChannel")
                        .operation("updateCv", "ownerChannel", "Update CV content deterministically"))
                .bindAccount("ownerChannel", "acc-current-user")
                .build();

        assertEquals("MyOS/Document Session Bootstrap", bootstrap.getType().getValue());
        assertEquals("pending", bootstrap.getAsText("/document/processingStatus/value"));
        assertEquals("Mia Zielinska", bootstrap.getAsText("/document/cv/name/value"));
        assertEquals("ownerChannel", bootstrap.getAsText("/document/contracts/updateCv/channel/value"));
        assertEquals("acc-current-user", bootstrap.getAsText("/channelBindings/ownerChannel/accountId/value"));
    }

    @Test
    void composesReadableJavaScriptProgram() {
        JsProgram program = BlueDocDsl.js(js -> js
                .line("const request = event.message?.request ?? {};")
                .line("const status = request.status ?? document('/processingStatus') ?? 'pending';")
                .returnObject(JsObjectBuilder.object()
                        .propArrayRaw("changeset", "[{ op: 'replace', path: '/processingStatus', val: status }]")));

        String code = program.code();
        assertNotNull(code);
        assertTrue(code.contains("event.message?.request"));
        assertTrue(code.contains("changeset"));
        assertTrue(code.contains("/processingStatus"));

        Node jsStep = new StepsBuilder()
                .js("ApplyStatus", program)
                .build()
                .get(0);
        assertEquals("Conversation/JavaScript Code", jsStep.getType().getValue());
        assertEquals("ApplyStatus", jsStep.getName());
        assertEquals(code, jsStep.getAsText("/code/value"));
    }

    @Test
    void supportsExpressionBasedUpdateDocumentStep() {
        Node step = new StepsBuilder()
                .updateDocumentFromExpression("PersistStatus", "steps.ApplyStatus.changeset")
                .build()
                .get(0);

        assertEquals("Conversation/Update Document", step.getType().getValue());
        assertEquals("${steps.ApplyStatus.changeset}", step.getAsText("/changeset/value"));
    }

    @Test
    void supportsRoleBasedBindingInDocumentBuilder() {
        Node bootstrap = MyOsDsl.bootstrap()
                .documentName("Role Binding Doc")
                .documentType(TypeAliases.MYOS_AGENT)
                .bindRoleEmail("payer", "payer@demo.com")
                .bindRoleAccount("payee", "ACC-123")
                .build();

        assertEquals("payer@demo.com", bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("ACC-123", bootstrap.getAsText("/channelBindings/payeeChannel/accountId/value"));
    }
}
