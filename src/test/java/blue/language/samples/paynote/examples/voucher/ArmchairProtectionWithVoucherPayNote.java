package blue.language.samples.paynote.examples.voucher;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.types.payments.PaymentRequests;

public final class ArmchairProtectionWithVoucherPayNote {

    private ArmchairProtectionWithVoucherPayNote() {
    }

    public static Node templateDoc() {
        return PayNotes.payNote("Armchair Protection + Voucher")
                .initialStateDescription(
                        "Capture is blocked until payer confirms satisfaction.",
                        "Payer confirms satisfaction, then capture is unblocked and voucher payment is requested.")
                .capture()
                    .lockOnInit()
                    .unlockOnOperation("confirmSatisfaction", op -> op
                            .channel("payerChannel")
                            .description("Confirm armchair is satisfactory and allow capture.")
                            .noRequest()
                            .steps(steps -> steps.emitType("SatisfactionConfirmed",
                                    VoucherEvents.SatisfactionConfirmed.class,
                                    payload -> payload.put("by", "payerChannel"))))
                    .done()
                .onFundsCaptured("requestVoucherPayment", steps -> steps.triggerPayment(
                        "DemoBankCreditLinePaymentRequested",
                        PaymentRequests.CreditLineMerchantToCardholderPaymentRequested.class,
                        payload -> payload
                                .processor("guarantorChannel")
                                .payer("payeeChannel")
                                .payee("payerChannel")
                                .currency("USD")
                                .amountMinor(10000)
                                .creditLineId("demoBank-facility-001")
                                .attachPayNote(BalancedBowlVoucherPayNote.templateDoc())))
                .buildDocument();
    }
}
