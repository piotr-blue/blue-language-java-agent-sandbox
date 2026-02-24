package blue.language.samples.paynote.examples.voucher;

import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.sdk.IsoCurrency;
import blue.language.samples.paynote.sdk.PayNotes;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.model.Node;

public final class BalancedBowlVoucherPayNote {

    private BalancedBowlVoucherPayNote() {
    }

    public static Node templateDoc() {
        return PayNotes.payNote("Balanced Bowl Voucher — $100")
                .currency(IsoCurrency.USD)
                .amountTotalMajor("100.00")
                .initialStateDescription(
                        "$100 voucher at Balanced Bowl.",
                        "Approve monitoring, then reserve funds. Each reported spend captures up to remaining balance.")
                .operation("approveMonitoring")
                    .channel("payeeChannel")
                    .description("Approve monitoring for Balanced Bowl transactions.")
                    .noRequest()
                    .steps(steps -> steps.emitType("MonitoringApproved",
                            VoucherEvents.MonitoringApproved.class,
                            payload -> payload.put("merchantId", "balanced_bowl_001")))
                    .done()
                .onEvent("onMonitoringApproved", VoucherEvents.MonitoringApproved.class, steps -> steps
                        .triggerEvent("ReserveFundsRequested", new Node()
                                .type(PayNoteAliases.RESERVE_FUNDS_REQUESTED)
                                .properties("amount", new Node().value(BlueDocDsl.expr("document('/amount/total')"))))
                        .emitType("StartMonitoring",
                                VoucherEvents.StartMonitoringRequested.class,
                                payload -> payload
                                        .put("merchantId", "balanced_bowl_001")
                                        .put("scope", "merchant-only")
                                        .put("subject", "payeeChannel")))
                .onEvent("onRestaurantTxn", VoucherEvents.RestaurantTransactionReported.class, steps -> steps
                        .jsRaw("CaptureUpToRemaining", captureUpToRemainingCode()))
                .buildDocument();
    }

    private static String captureUpToRemainingCode() {
        return ""
                + "const spent = event.message.amount;\n"
                + "const total = document('/amount/total') || 0;\n"
                + "const captured = document('/amount/captured') || 0;\n"
                + "const remaining = total - captured;\n"
                + "if (remaining <= 0) {\n"
                + "  return { events: [] };\n"
                + "}\n"
                + "const toCapture = Math.min(spent, remaining);\n"
                + "return {\n"
                + "  events: [\n"
                + "    { type: '" + PayNoteAliases.CAPTURE_FUNDS_REQUESTED + "', amount: toCapture }\n"
                + "  ]\n"
                + "};";
    }
}
