package blue.language.samples.paynote.types.domain;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class PayNoteDemoEvents {

    private PayNoteDemoEvents() {
    }

    @TypeAlias("PayNote Demo/Subscription Cycle Started")
    @TypeBlueId("PayNoteDemo-Subscription-Cycle-Started-BlueId")
    public static class SubscriptionCycleStarted {
        public String cycleId;
    }

    @TypeAlias("PayNote Demo/Marketplace Split Requested")
    @TypeBlueId("PayNoteDemo-Marketplace-Split-Requested-BlueId")
    public static class MarketplaceSplitRequested {
        public String orderId;
    }

    @TypeAlias("PayNote Demo/Agent Purchase Approved")
    @TypeBlueId("PayNoteDemo-Agent-Purchase-Approved-BlueId")
    public static class AgentPurchaseApproved {
        public String purchaseId;
    }

    @TypeAlias("PayNote Demo/Milestone Approved")
    @TypeBlueId("PayNoteDemo-Milestone-Approved-BlueId")
    public static class MilestoneApproved {
        public Integer milestone;
    }

    @TypeAlias("PayNote Demo/Voucher Triggered")
    @TypeBlueId("PayNoteDemo-Voucher-Triggered-BlueId")
    public static class VoucherTriggered {
        public String triggerId;
    }

    @TypeAlias("PayNote Demo/Capture Hook Ran")
    @TypeBlueId("PayNoteDemo-Capture-Hook-Ran-BlueId")
    public static class CaptureHookRan {
        public Integer amount;
        public String note;
    }
}
