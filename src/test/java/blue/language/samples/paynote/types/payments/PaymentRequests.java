package blue.language.samples.paynote.types.payments;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class PaymentRequests {

    private PaymentRequests() {
    }

    @TypeAlias("Payments/Payment Requested")
    @TypeBlueId("Payments-Base-Payment-Requested-BlueId")
    public static class PaymentRequested {
        public String processor;
        public Node payer;
        public Node payee;
        public String currency;
        public Integer amount;
        public Node attachedPayNote;
    }

    @TypeAlias("Payments/Ach Transfer Requested")
    @TypeBlueId("Payments-Ach-Transfer-Requested-BlueId")
    public static class AchTransferRequested extends PaymentRequested {
        public String sourceIban;
        public String destinationIban;
    }

    @TypeAlias("Payments/Card Charge Requested")
    @TypeBlueId("Payments-Card-Charge-Requested-BlueId")
    public static class CardChargeRequested extends PaymentRequested {
        public String cardToken;
    }

    @TypeAlias("Payments/Card Stored Credential Charge Requested")
    @TypeBlueId("Payments-Card-Stored-Credential-Charge-Requested-BlueId")
    public static class CardStoredCredentialChargeRequested extends PaymentRequested {
        public String storedCardRef;
    }

    @TypeAlias("Payments/Credit Line Merchant To Cardholder Payment Requested")
    @TypeBlueId("Payments-Credit-Line-Merchant-To-Cardholder-Payment-Requested-BlueId")
    public static class CreditLineMerchantToCardholderPaymentRequested extends PaymentRequested {
        public String creditLineId;
    }

    @TypeAlias("Payments/Internal Ledger Transfer Requested")
    @TypeBlueId("Payments-Internal-Ledger-Transfer-Requested-BlueId")
    public static class InternalLedgerTransferRequested extends PaymentRequested {
        public String ledgerAccountFrom;
        public String ledgerAccountTo;
    }

    @TypeAlias("Payments/Crypto Transfer Requested")
    @TypeBlueId("Payments-Crypto-Transfer-Requested-BlueId")
    public static class CryptoTransferRequested extends PaymentRequested {
        public String asset;
        public String walletAddress;
    }
}
