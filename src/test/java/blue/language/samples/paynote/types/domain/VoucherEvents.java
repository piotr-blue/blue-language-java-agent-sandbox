package blue.language.samples.paynote.types.domain;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class VoucherEvents {

    private VoucherEvents() {
    }

    @TypeAlias("Demo/Satisfaction Confirmed")
    @TypeBlueId("Voucher-Demo-Satisfaction-Confirmed-BlueId")
    public static class SatisfactionConfirmed {
        public String by;
    }

    @TypeAlias("DemoBank/Credit Line Payment Requested")
    @TypeBlueId("Voucher-DemoBank-Credit-Line-Payment-Requested-BlueId")
    public static class CreditLinePaymentRequested {
        public String payer;
        public String payee;
        public String currency;
        public Integer amountMinor;
        public String bootstrapRecipient;
        public Node attachedPayNoteTemplate;
    }

    @TypeAlias("Voucher/Monitoring Approved")
    @TypeBlueId("Voucher-Monitoring-Approved-BlueId")
    public static class MonitoringApproved {
        public String merchantId;
    }

    @TypeAlias("Voucher/Start Monitoring Requested")
    @TypeBlueId("Voucher-Start-Monitoring-Requested-BlueId")
    public static class StartMonitoringRequested {
        public String merchantId;
        public String scope;
        public String subject;
    }

    @TypeAlias("Voucher/Restaurant Transaction Reported")
    @TypeBlueId("Voucher-Restaurant-Transaction-Reported-BlueId")
    public static class RestaurantTransactionReported {
        public Integer amount;
        public String merchantId;
    }
}
