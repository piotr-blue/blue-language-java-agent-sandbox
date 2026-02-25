package blue.language.samples.paynote2.examples.voucher;

import blue.language.model.Node;
import blue.language.samples.paynote.types.domain.VoucherEvents;
import blue.language.samples.paynote2.sdk.PayNotes;

public final class BalancedBowlVoucherPayNote {

    private BalancedBowlVoucherPayNote() {
    }

    public static Node templateDoc() {
        return PayNotes.payNote("Balanced Bowl Voucher - 100 USD")
                .description("Reserve voucher budget and capture spending reported by merchant monitoring.")
                .currency("USD")
                .amountMinor(10000)
                .channel("merchantChannel", "Balanced Bowl")
                .capture()
                    .lockOnInit()
                    .unlockOnEvent(VoucherEvents.MonitoringApproved.class)
                    .requestPartialOnOperation(
                            "captureReportedSpend",
                            "merchantChannel",
                            "Capture reported amount.",
                            "event.message.request.amount")
                    .done()
                .onEvent("onMonitoringApproved", VoucherEvents.MonitoringApproved.class, steps -> steps.emitType(
                        "StartMonitoring",
                        VoucherEvents.StartMonitoringRequested.class,
                        payload -> payload
                                .put("merchantId", "balanced_bowl_001")
                                .put("scope", "merchant-only")
                                .put("subject", "payeeChannel")))
                .buildDocument();
    }
}
