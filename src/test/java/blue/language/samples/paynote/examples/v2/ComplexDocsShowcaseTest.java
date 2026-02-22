package blue.language.samples.paynote.examples.v2;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexDocsShowcaseTest {

    @Test
    void buildsTenComplexDocumentsWithReusableTypedDsl() {
        Map<String, Node> docs = ComplexDocsShowcase.buildAll("2026-02-21T12:00:00Z");

        assertEquals(10, docs.size());
        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, docs.get("candidateCvBootstrap").getAsText("/type/value"));
        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, docs.get("recruitmentClassifierBootstrap").getAsText("/type/value"));
        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, docs.get("timedCaptureVoucher").getAsText("/type/value"));

        Node timedCapture = docs.get("timedCaptureVoucher");
        assertEquals(TypeAliases.PAYNOTE_DOCUMENT, timedCapture.getAsText("/document/type/value"));
        assertEquals(TypeAliases.CORE_DOCUMENT_UPDATE_CHANNEL,
                timedCapture.getAsText("/document/contracts/timerChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW,
                timedCapture.getAsText("/document/contracts/captureAfterTimer/type/value"));

        Node sportsBarrier = docs.get("shippingAndSportsBarrier");
        String barrierJs = sportsBarrier.getAsText("/document/contracts/waitForShipmentAndOutcome/steps/0/code/value");
        assertTrue(barrierJs.contains("shipment-delivered"));
        assertTrue(barrierJs.contains("match-won"));

        Node marketplace = docs.get("marketplaceSplit");
        assertEquals(TypeAliases.CONVERSATION_TRIGGER_EVENT,
                marketplace.getAsText("/document/contracts/issueChildrenOnReserve/steps/0/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DOCUMENT, marketplace.getAsText("/document/type/value"));

        Node fraudFlow = docs.get("fraudHoldAndRelease");
        assertEquals(TypeAliases.COMMON_NAMED_EVENT,
                fraudFlow.getAsText("/document/contracts/freezeOnFraudSignal/event/type/value"));
        assertEquals(TypeAliases.COMMON_NAMED_EVENT,
                fraudFlow.getAsText("/document/contracts/freezeOnFraudSignal/steps/1/event/type/value"));

        Node escrow = docs.get("dualApprovalEscrow");
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                escrow.getAsText("/document/contracts/refundFull/type/value"));
    }
}
