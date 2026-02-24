package blue.language.samples.paynote.types.domain;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class CookbookEvents {

    private CookbookEvents() {
    }

    @TypeAlias("Cookbook/Delivery Confirmed")
    @TypeBlueId("Cookbook-Delivery-Confirmed-BlueId")
    public static class DeliveryConfirmed {
        public String orderId;
    }

    @TypeAlias("Cookbook/Dispute Opened")
    @TypeBlueId("Cookbook-Dispute-Opened-BlueId")
    public static class DisputeOpened {
        public String reason;
    }

    @TypeAlias("Cookbook/Installment Due")
    @TypeBlueId("Cookbook-Installment-Due-BlueId")
    public static class InstallmentDue {
        public Integer amount;
    }

    @TypeAlias("Cookbook/Trial Ended")
    @TypeBlueId("Cookbook-Trial-Ended-BlueId")
    public static class TrialEnded {
        public String subscriptionId;
    }

    @TypeAlias("Cookbook/Usage Reported")
    @TypeBlueId("Cookbook-Usage-Reported-BlueId")
    public static class UsageReported {
        public Integer units;
    }

    @TypeAlias("Cookbook/Kyc Approved")
    @TypeBlueId("Cookbook-Kyc-Approved-BlueId")
    public static class KycApproved {
        public String userId;
    }

    @TypeAlias("Cookbook/Fx Quote Accepted")
    @TypeBlueId("Cookbook-Fx-Quote-Accepted-BlueId")
    public static class FxQuoteAccepted {
        public String quoteId;
    }

    @TypeAlias("Cookbook/Event Date Reached")
    @TypeBlueId("Cookbook-Event-Date-Reached-BlueId")
    public static class EventDateReached {
        public String eventId;
    }

    @TypeAlias("Cookbook/Inspection Passed")
    @TypeBlueId("Cookbook-Inspection-Passed-BlueId")
    public static class InspectionPassed {
        public String inspectorId;
    }
}
