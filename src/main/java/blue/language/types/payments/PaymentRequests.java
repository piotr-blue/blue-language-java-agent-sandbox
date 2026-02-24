package blue.language.types.payments;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

public final class PaymentRequests {

    private PaymentRequests() {
    }

    @TypeBlueId("Payments-Base-Payment-Requested-BlueId")
    public static class PaymentRequested {
        public String processor;
        public Node payer;
        public Node payee;
        public String currency;
        public Long amountMinor;
        public Node attachedPayNote;
    }

    @TypeBlueId("Payments-Ach-Transfer-Requested-BlueId")
    public static class AchTransferRequested extends PaymentRequested {
        public String routingNumber;
        public String accountNumber;
        public String accountType;
        public String network;
        public String companyEntryDescription;
    }

    @TypeBlueId("Payments-Sepa-Transfer-Requested-BlueId")
    public static class SepaTransferRequested extends PaymentRequested {
        public String ibanFrom;
        public String ibanTo;
        public String bicTo;
        public String remittanceInformation;
    }

    @TypeBlueId("Payments-Wire-Transfer-Requested-BlueId")
    public static class WireTransferRequested extends PaymentRequested {
        public String bankSwift;
        public String bankName;
        public String accountNumber;
        public String beneficiaryName;
        public String beneficiaryAddress;
    }

    @TypeBlueId("Payments-Card-Payment-Requested-BlueId")
    public static class CardPaymentRequested extends PaymentRequested {
        public String cardOnFileRef;
        public String merchantDescriptor;
    }

    @TypeBlueId("Payments-Card-Token-Payment-Requested-BlueId")
    public static class CardTokenPaymentRequested extends PaymentRequested {
        public String networkToken;
        public String tokenProvider;
        public String cryptogram;
    }

    @TypeBlueId("Payments-Credit-Line-Merchant-To-Cardholder-Payment-Requested-BlueId")
    public static class CreditLineMerchantToCardholderPaymentRequested extends PaymentRequested {
        public String creditLineId;
        public String merchantAccountId;
        public String cardholderAccountId;
    }

    @TypeBlueId("Payments-Internal-Ledger-Transfer-Requested-BlueId")
    public static class InternalLedgerTransferRequested extends PaymentRequested {
        public String ledgerAccountFrom;
        public String ledgerAccountTo;
        public String memo;
    }

    @TypeBlueId("Payments-Crypto-Transfer-Requested-BlueId")
    public static class CryptoTransferRequested extends PaymentRequested {
        public String asset;
        public String chain;
        public String fromWalletRef;
        public String toAddress;
        public String txPolicy;
    }
}
