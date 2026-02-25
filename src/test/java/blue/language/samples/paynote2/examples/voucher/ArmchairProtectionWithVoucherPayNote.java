package blue.language.samples.paynote2.examples.voucher;

import blue.language.model.Node;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.samples.paynote2.sdk.PayNotes;
import blue.language.types.payments.PaymentRequests;

public final class ArmchairProtectionWithVoucherPayNote {

    private ArmchairProtectionWithVoucherPayNote() {
    }

    public static Node templateDoc() {
        return PayNotes.payNote("Armchair Protection + Voucher")
                .description("Capture unlocks after buyer satisfaction, then a voucher payment is requested.")
                .currency("USD")
                .amountMinor(10000)
                .capture()
                .lockOnInit()
                .unlockOnOperation(
                        "confirmSatisfaction",
                        "payerChannel",
                        "Buyer confirms satisfaction.",
                        steps -> steps.emitType("SatisfactionConfirmed", VoucherEvents.SatisfactionConfirmed.class, null))
                .done()
                .onEvent("requestVoucherPayment", PayNoteTypes.FundsCaptured.class, steps -> steps.triggerPayment(
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
