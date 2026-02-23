package blue.language.samples.paynote.examples.voucher;

import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
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
                        .js("CaptureUpToRemaining", captureUpToRemainingProgram()))
                .buildDocument();
    }

    private static JsProgram captureUpToRemainingProgram() {
        return BlueDocDsl.js(js -> js
                .line("const spent = event.message.amount;")
                .line("const total = document('/amount/total') || 0;")
                .line("const captured = document('/amount/captured') || 0;")
                .line("const remaining = total - captured;")
                .line("if (remaining <= 0) {")
                .line("  return { events: [] };")
                .line("}")
                .line("const toCapture = Math.min(spent, remaining);")
                .returnOutput(JsOutputBuilder.output()
                        .eventsRaw("[{ type: '" + PayNoteAliases.CAPTURE_FUNDS_REQUESTED + "', amount: toCapture }]")));
    }
}
