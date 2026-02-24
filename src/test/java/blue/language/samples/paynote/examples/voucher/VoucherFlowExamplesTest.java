package blue.language.samples.paynote.examples.voucher;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoucherFlowExamplesTest {

    @Test
    void armchairProtectionFlowLocksThenUnlocksAndRequestsVoucherPayment() {
        Node doc = ArmchairProtectionWithVoucherPayNote.templateDoc();

        assertEquals(PayNoteAliases.PAYNOTE, doc.getAsText("/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION, doc.getAsText("/contracts/confirmSatisfaction/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_LOCK_REQUESTED,
                doc.getAsText("/contracts/onInitCaptureLock/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_UNLOCK_REQUESTED,
                doc.getAsText("/contracts/confirmSatisfactionImpl/steps/1/event/type/value"));
        assertEquals(TypeAliases.PAYMENTS_CREDIT_LINE_MERCHANT_TO_CARDHOLDER_PAYMENT_REQUESTED,
                doc.getAsText("/contracts/requestVoucherPayment/steps/0/event/type/value"));
    }

    @Test
    void balancedBowlVoucherFlowUsesMonitoringApprovalAndBudgetedCaptureJs() {
        Node doc = BalancedBowlVoucherPayNote.templateDoc();

        assertEquals(TypeAliases.CONVERSATION_OPERATION, doc.getAsText("/contracts/approveMonitoring/type/value"));
        assertEquals(TypeAliases.VOUCHER_MONITORING_APPROVED,
                doc.getAsText("/contracts/approveMonitoringImpl/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.RESERVE_FUNDS_REQUESTED,
                doc.getAsText("/contracts/onMonitoringApproved/steps/0/event/type/value"));
        assertEquals(TypeAliases.VOUCHER_START_MONITORING_REQUESTED,
                doc.getAsText("/contracts/onMonitoringApproved/steps/1/event/type/value"));

        String js = doc.getAsText("/contracts/onRestaurantTxn/steps/0/code/value");
        assertTrue(js.contains("Math.min(spent, remaining)"));
        assertTrue(js.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
    }
}
