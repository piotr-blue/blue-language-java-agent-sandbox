package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateCvBootstrapExampleTest {

    @Test
    void buildsCandidateCvBootstrapDocumentWithDeterministicContracts() {
        Node bootstrap = CandidateCvBootstrapExample.build(
                "2026-02-21T12:00:00Z",
                "recruitment-session-123",
                "acc-current-1"
        );

        assertEquals("MyOS/Document Session Bootstrap", bootstrap.getType().getValue());
        assertEquals("Candidate CV B - 2026-02-21T12:00:00Z", bootstrap.getAsText("/document/name"));
        assertEquals("pending", bootstrap.getAsText("/document/processingStatus/value"));
        assertEquals("Mia Zielinska", bootstrap.getAsText("/document/cv/name/value"));

        Node contracts = bootstrap.getAsNode("/document/contracts");
        assertNotNull(contracts.getProperties().get("ownerChannel"));
        assertNotNull(contracts.getProperties().get("links"));
        assertNotNull(contracts.getProperties().get("updateCv"));
        assertNotNull(contracts.getProperties().get("changeProcessingStatus"));
        assertNotNull(contracts.getProperties().get("changeProcessingStatusImpl"));
        assertNotNull(contracts.getProperties().get("updateCvImpl"));

        assertEquals("recruitment-session-123",
                bootstrap.getAsText("/document/contracts/links/recruitmentLink/sessionId/value"));
        assertEquals("acc-current-1", bootstrap.getAsText("/channelBindings/ownerChannel/accountId/value"));

        String applyStatusCode = bootstrap.getAsText("/document/contracts/changeProcessingStatusImpl/steps/0/code/value");
        assertTrue(applyStatusCode.contains("const status"));
        assertTrue(applyStatusCode.contains("/processingStatus"));

        String applyCvUpdateCode = bootstrap.getAsText("/document/contracts/updateCvImpl/steps/0/code/value");
        assertTrue(applyCvUpdateCode.contains("CV updated"));
        assertTrue(applyCvUpdateCode.contains("/cv/experience"));
        assertEquals("${steps.ApplyCvUpdate.changeset}",
                bootstrap.getAsText("/document/contracts/updateCvImpl/steps/1/changeset/value"));
    }
}
