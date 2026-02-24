package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecruitmentClassifierBootstrapExampleTest {

    @Test
    void buildsRecruitmentClassifierBootstrapWithComplexWorkflowContracts() {
        Node bootstrap = RecruitmentClassifierBootstrapExample.build(
                "2026-02-21T13:00:00Z",
                "recruitment-session-abc",
                "llm-provider-session-xyz",
                "acc-recruitment-owner"
        );

        assertEquals("MyOS/Document Session Bootstrap", bootstrap.getType().getValue());
        assertEquals("Recruitment Classifier - 2026-02-21T13:00:00Z", bootstrap.getAsText("/document/name"));
        assertEquals("MyOS/Agent", bootstrap.getAsText("/document/type/value"));
        assertEquals("recruitment-session-abc", bootstrap.getAsText("/document/recruitmentSessionId/value"));
        assertEquals("llm-provider-session-xyz", bootstrap.getAsText("/document/llmProviderSessionId/value"));
        assertEquals("acc-recruitment-owner", bootstrap.getAsText("/channelBindings/recruitmentChannel/accountId/value"));
        assertEquals("0", bootstrap.getAsText("/channelBindings/myOsAdminChannel/accountId/value"));

        Node contracts = bootstrap.getAsNode("/document/contracts");
        assertNotNull(contracts.getProperties().get("requestAccess"));
        assertNotNull(contracts.getProperties().get("onCvEpoch"));
        assertNotNull(contracts.getProperties().get("onProviderResponse"));
        assertNotNull(contracts.getProperties().get("myOsAdminUpdateImpl"));

        assertEquals("MyOS/Single Document Permission Granted",
                bootstrap.getAsText("/document/contracts/onCvAccessGranted/event/type/value"));
        assertEquals("SUB_RECRUITMENT_CVS",
                bootstrap.getAsText("/document/contracts/onCvSubscriptionInitiated/event/subscriptionId/value"));
        assertEquals("MyOS/Session Epoch Advanced",
                bootstrap.getAsText("/document/contracts/onCvEpoch/event/update/type/value"));
        assertEquals("RECRUITMENT_CV_CLASSIFIER",
                bootstrap.getAsText("/document/contracts/onProviderResponse/event/update/inResponseTo/incomingEvent/requester/value"));

        String onCvEpochCode = bootstrap.getAsText("/document/contracts/onCvEpoch/steps/0/code/value");
        assertTrue(onCvEpochCode.contains("REQ_CV_CLASSIFY_"));
        assertTrue(onCvEpochCode.contains("provideInstructions"));
        assertTrue(onCvEpochCode.contains("Return raw JSON only."));

        String onProviderResponseCode = bootstrap.getAsText("/document/contracts/onProviderResponse/steps/0/code/value");
        assertTrue(onProviderResponseCode.contains("New senior CV to review"));
        assertTrue(onProviderResponseCode.contains("/classificationByCv/"));
        assertTrue(onProviderResponseCode.contains("changeProcessingStatus"));
    }
}
