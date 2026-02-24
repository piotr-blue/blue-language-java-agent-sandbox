package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.types.payments.PaymentRequests;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentRequestTypingTest {

    @Test
    void resolvesV1PaymentSubtypeAliases() {
        assertEquals(TypeAliases.PAYMENTS_PAYMENT_REQUESTED,
                TypeRef.of(PaymentRequests.PaymentRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_ACH_TRANSFER_REQUESTED,
                TypeRef.of(PaymentRequests.AchTransferRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_SEPA_TRANSFER_REQUESTED,
                TypeRef.of(PaymentRequests.SepaTransferRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_WIRE_TRANSFER_REQUESTED,
                TypeRef.of(PaymentRequests.WireTransferRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_CARD_PAYMENT_REQUESTED,
                TypeRef.of(PaymentRequests.CardPaymentRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_CARD_TOKEN_PAYMENT_REQUESTED,
                TypeRef.of(PaymentRequests.CardTokenPaymentRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_CREDIT_LINE_MERCHANT_TO_CARDHOLDER_PAYMENT_REQUESTED,
                TypeRef.of(PaymentRequests.CreditLineMerchantToCardholderPaymentRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_INTERNAL_LEDGER_TRANSFER_REQUESTED,
                TypeRef.of(PaymentRequests.InternalLedgerTransferRequested.class).alias());
        assertEquals(TypeAliases.PAYMENTS_CRYPTO_TRANSFER_REQUESTED,
                TypeRef.of(PaymentRequests.CryptoTransferRequested.class).alias());
    }

    @Test
    void triggerPaymentPayloadBuilderSupportsBaseAndSubtypeFields() {
        StepsBuilder steps = new StepsBuilder();

        steps.triggerPayment("CryptoPayout",
                PaymentRequests.CryptoTransferRequested.class,
                payload -> payload
                        .processor("guarantorChannel")
                        .payer("treasury")
                        .payee("beneficiary")
                        .currency("USD")
                        .amountMinor(7500)
                        .asset("USDC")
                        .chain("base")
                        .fromWalletRef("treasury_hot_wallet")
                        .toAddress("0xabc123")
                        .txPolicy("require-two-signers"));

        Node root = new Node().items(steps.build());
        assertEquals(TypeAliases.PAYMENTS_CRYPTO_TRANSFER_REQUESTED, root.getAsText("/0/event/type/value"));
        assertEquals("guarantorChannel", root.getAsText("/0/event/processor/value"));
        assertEquals("USD", root.getAsText("/0/event/currency/value"));
        assertEquals(7500L, root.getAsInteger("/0/event/amountMinor/value").longValue());
        assertEquals("USDC", root.getAsText("/0/event/asset/value"));
        assertEquals("base", root.getAsText("/0/event/chain/value"));
        assertEquals("0xabc123", root.getAsText("/0/event/toAddress/value"));
    }
}
