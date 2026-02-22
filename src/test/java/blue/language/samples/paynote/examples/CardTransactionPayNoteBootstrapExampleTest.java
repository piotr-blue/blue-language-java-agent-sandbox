package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardTransactionPayNoteBootstrapExampleTest {

    @Test
    void buildsCardTransactionPayNoteWithAbstractContractsAndConcreteBindings() {
        Node bootstrap = CardTransactionPayNoteBootstrapExample.build(
                "2026-02-22T12:00:00Z",
                "acc-alice",
                "acc-bob-merchant",
                "acc-bank",
                "acc-dhl");

        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_PAYNOTE, bootstrap.getAsText("/document/type/value"));
        assertEquals(80000, bootstrap.getAsInteger("/document/amount/total/value").intValue());
        assertTrue(bootstrap.getAsText("/document/payNoteInitialStateDescription/summary/value").contains("$800.00"));

        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/payerChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/allParticipantsChannel/type/value"));
        assertEquals("acc-alice", bootstrap.getAsText("/channelBindings/payerChannel/accountId/value"));
        assertEquals("acc-dhl", bootstrap.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));

        String lockProgram = bootstrap.getAsText("/document/contracts/bootstrap/steps/0/code/value");
        String unlockProgram = bootstrap.getAsText("/document/contracts/confirmShipmentImpl/steps/1/code/value");
        assertTrue(lockProgram.contains(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED));
        assertTrue(unlockProgram.contains(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED));

        assertEquals("allow-listed-direct-change",
                bootstrap.getAsText("/document/policies/contractsChangePolicy/mode/value"));
        assertEquals("/shipping/trackingNumber",
                bootstrap.getAsText("/document/policies/changesetAllowList/directChange/1/value"));
    }
}
