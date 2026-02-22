package blue.language.samples.paynote.examples.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VNextRecruitmentClassifierExampleTest {

    @Test
    void buildsTypedRecruitmentClassifierWithStructuredJsPrograms() {
        Node bootstrap = VNextRecruitmentClassifierExample.build(
                "2026-02-22T16:00:00Z",
                "RECRUITMENT_SESSION_001",
                "LLM_PROVIDER_SESSION_001",
                "acc_recruitment_owner_1");

        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals(TypeAliases.MYOS_AGENT, bootstrap.getAsText("/document/type/value"));
        assertEquals("acc_recruitment_owner_1", bootstrap.getAsText("/channelBindings/recruitmentChannel/accountId/value"));

        assertEquals(TypeAliases.MYOS_SUBSCRIPTION_UPDATE,
                bootstrap.getAsText("/document/contracts/onCvEpoch/event/type/value"));
        assertEquals(TypeAliases.MYOS_SESSION_EPOCH_ADVANCED,
                bootstrap.getAsText("/document/contracts/onCvEpoch/event/update/type/value"));

        String requestClassificationJs = bootstrap.getAsText("/document/contracts/onCvEpoch/steps/0/code/value");
        assertTrue(requestClassificationJs.contains("REQ_CV_CLASSIFY_"));
        assertTrue(requestClassificationJs.contains("Recruitment/CV Classification Requested"));
        assertTrue(requestClassificationJs.contains("provideInstructions"));
        assertTrue(!requestClassificationJs.contains("Conversation/Event"));

        String providerResponseJs = bootstrap.getAsText("/document/contracts/onProviderResponse/steps/0/code/value");
        assertTrue(providerResponseJs.contains("Recruitment/Senior Candidate Detected"));
        assertTrue(providerResponseJs.contains("Number.isFinite"));
        assertTrue(providerResponseJs.contains("CV classified: "));
        assertTrue(!providerResponseJs.contains("Conversation/Event"));
    }
}
