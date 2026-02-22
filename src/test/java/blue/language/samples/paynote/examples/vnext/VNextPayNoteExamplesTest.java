package blue.language.samples.paynote.examples.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VNextPayNoteExamplesTest {

    @Test
    void buildsIphoneEscrowAndTemplateChainWithTypedEvents() {
        Node iphone = VNextPayNoteExamples.iphoneShipmentEscrow();
        VNextPayNoteExamples.TemplateChain chain = VNextPayNoteExamples.shipmentEscrowTemplateChain("2026-02-22T12:00:00Z");

        assertEquals(PayNoteAliases.CARD_TRANSACTION_PAYNOTE, iphone.getAsText("/document/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                iphone.getAsText("/document/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED,
                iphone.getAsText("/document/contracts/onInitLockCardCapture/steps/0/event/type/value"));
        String directChangeJs = iphone.getAsText("/document/contracts/directChangeImpl/steps/0/code/value");
        assertTrue(directChangeJs.contains("request.changeset ?? []"));

        assertEquals(0, chain.template.getAsInteger("/document/amount/total/value").intValue());
        assertEquals(20000, chain.specialized.getAsInteger("/document/amount/total/value").intValue());
        assertEquals("CHF", chain.specialized.getAsText("/document/funding/sourceCurrency/value"));
        assertEquals("acc_alice_001", chain.instance.getAsText("/channelBindings/payerChannel/accountId/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                chain.instance.getAsText("/document/contracts/confirmShipmentImpl/steps/0/event/type/value"));
    }

    @Test
    void buildsSubscriptionMarketplaceAgentMilestoneAndVoucherExamples() {
        Node subscription = VNextPayNoteExamples.subscriptionPayNote();
        Node marketplace = VNextPayNoteExamples.marketplaceSplitPayNote();
        Node agentBudget = VNextPayNoteExamples.agentBudgetPayNote();
        Node milestone = VNextPayNoteExamples.milestoneContractorPayNote();
        Node voucher = VNextPayNoteExamples.reversePaymentVoucherPayNote();

        assertEquals(PayNoteAliases.PAYNOTE, subscription.getAsText("/document/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DEMO_SUBSCRIPTION_CYCLE_STARTED,
                subscription.getAsText("/document/contracts/issueMonthlyChild/event/type/value"));
        assertTrue(subscription.getAsText("/document/contracts/issueMonthlyChildOnce/steps/0/code/value")
                .contains("alreadyDone"));

        assertEquals(PayNoteAliases.PAYNOTE, marketplace.getAsText("/document/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DEMO_MARKETPLACE_SPLIT_REQUESTED,
                marketplace.getAsText("/document/contracts/configureSplitOnce/event/type/value"));
        assertTrue(marketplace.getAsText("/document/contracts/configureSplitOnce/steps/0/code/value")
                .contains("alreadyDone"));

        assertEquals(PayNoteAliases.PAYNOTE, agentBudget.getAsText("/document/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DEMO_AGENT_PURCHASE_APPROVED,
                agentBudget.getAsText("/document/contracts/issueAgentPurchaseChild/event/type/value"));
        assertTrue(agentBudget.getAsText("/document/contracts/approvePurchaseOnce/steps/0/code/value")
                .contains("alreadyDone"));

        assertEquals(PayNoteAliases.PAYNOTE, milestone.getAsText("/document/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DEMO_MILESTONE_APPROVED,
                milestone.getAsText("/document/contracts/captureOnMilestone/event/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                milestone.getAsText("/document/contracts/releaseUnspentAmount/type/value"));
        assertTrue(milestone.getAsText("/document/contracts/recordMilestoneOnce/steps/0/code/value")
                .contains("alreadyDone"));

        assertEquals(PayNoteAliases.PAYNOTE, voucher.getAsText("/document/type/value"));
        assertEquals(TypeAliases.PAYNOTE_DEMO_VOUCHER_TRIGGERED,
                voucher.getAsText("/document/contracts/monitorExternalTransactionImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVE_FUNDS_AND_CAPTURE_IMMEDIATELY_REQUESTED,
                voucher.getAsText("/document/contracts/onInitReserveAndCapture/steps/0/event/type/value"));
        assertTrue(voucher.getAsText("/document/contracts/directVoucherEditImpl/steps/0/code/value")
                .contains("request.changeset ?? []"));
    }
}
