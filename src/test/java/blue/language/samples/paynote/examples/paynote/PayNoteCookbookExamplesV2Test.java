package blue.language.samples.paynote.examples.paynote;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteCookbookExamplesV2Test {

    @Test
    void providesTieredCookbookWithTwentyFiveTickets() {
        Map<String, Node> tier1 = PayNoteCookbookExamplesV2.tier1Tiny();
        Map<String, Node> tier2 = PayNoteCookbookExamplesV2.tier2Medium();
        Map<String, Node> tier3 = PayNoteCookbookExamplesV2.tier3JsHeavy();
        Map<String, Node> all = PayNoteCookbookExamplesV2.allTickets();

        assertEquals(10, tier1.size());
        assertEquals(10, tier2.size());
        assertEquals(5, tier3.size());
        assertEquals(25, all.size());
    }

    @Test
    void everyTicketBuildsPayNoteWithName() {
        for (Map.Entry<String, Node> entry : PayNoteCookbookExamplesV2.allTickets().entrySet()) {
            Node document = entry.getValue();
            assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"), entry.getKey());
            assertFalse(document.getAsText("/name/value").trim().isEmpty(), entry.getKey());
        }
    }

    @Test
    void mediumBankTransferEscrowIncludesExpectedMultiApprovalOperations() {
        Node document = PayNoteCookbookExamplesV2.ticket14BankTransferEscrowMultiApproval();

        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/inspectorApproval/type/value"));
        assertEquals("inspectorChannel", document.getAsText("/contracts/inspectorApproval/channel/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/titleApproval/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                document.getAsText("/contracts/buyerFinalApproval/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_FUNDS_REQUESTED,
                document.getAsText("/contracts/buyerFinalApprovalImpl/steps/0/event/type/value"));
    }

    @Test
    void jsHeavyTicketsContainCriticalCaptureLogicFragments() {
        Node donation = PayNoteCookbookExamplesV2.ticket21DonationRoundUpJs();
        Node corporateSpend = PayNoteCookbookExamplesV2.ticket23CorporateSpendAllowlistJs();
        Node financing = PayNoteCookbookExamplesV2.ticket24MerchantFinancingInstallmentsJs();

        String donationJs = donation.getAsText("/contracts/roundUpDonation/steps/0/code/value");
        assertTrue(donationJs.contains("const cents = total % 100;"));
        assertTrue(donationJs.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));

        String spendJs = corporateSpend.getAsText("/contracts/onCorporateSpendReported/steps/0/code/value");
        assertTrue(spendJs.contains("allowlist.includes(merchant)"));
        assertTrue(spendJs.contains("hasReceipt"));

        String financingJs = financing.getAsText("/contracts/onInstallmentDue/steps/0/code/value");
        assertTrue(financingJs.contains("status === 'defaulted'"));
        assertTrue(financingJs.contains("Math.min(due, remaining)"));
    }
}
