package blue.language.samples.paynote.examples.voucher;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.VoucherEvents;

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
                    .unlockExternalOnOperation("confirmSatisfaction", op -> op
                            .channel("payerChannel")
                            .description("Confirm armchair is satisfactory and allow capture.")
                            .noRequest()
                            .steps(steps -> steps.emitType("SatisfactionConfirmed",
                                    VoucherEvents.SatisfactionConfirmed.class,
                                    payload -> payload.put("by", "payerChannel"))))
                    .done()
                .onFundsCaptured("requestVoucherPayment", steps -> steps.emitType(
                        "SynchronyCreditLinePaymentRequested",
                        VoucherEvents.CreditLinePaymentRequested.class,
                        payload -> payload
                                .put("payer", "payeeChannel")
                                .put("payee", "payerChannel")
                                .put("currency", "USD")
                                .put("amountMinor", 10000)
                                .put("bootstrapRecipient", "guarantorChannel")
                                .putNode("attachedPayNoteTemplate", BalancedBowlVoucherPayNote.templateDoc())))
                .buildDocument();
    }
}
