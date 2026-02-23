package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.dsl.DocPath;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteBuilderTest {

    @Test
    void convertsMajorAmountsUsingCurrencyScale() {
        Node usd = PayNotes.payNote("USD")
                .currency(IsoCurrency.USD)
                .amountTotalMajor(new BigDecimal("4.25"))
                .buildDocument();
        assertEquals(425L, usd.getAsInteger("/amount/total/value").longValue());

        Node jpy = PayNotes.payNote("JPY")
                .currency(IsoCurrency.JPY)
                .amountTotalMajor(new BigDecimal("125"))
                .buildDocument();
        assertEquals(125L, jpy.getAsInteger("/amount/total/value").longValue());

        IllegalArgumentException invalidScale = assertThrows(IllegalArgumentException.class, () ->
                PayNotes.payNote("Invalid")
                        .currency(IsoCurrency.USD)
                        .amountTotalMajor(new BigDecimal("4.522")));
        assertTrue(invalidScale.getMessage().contains("exceeds currency fraction digits"));
    }

    @Test
    void keepsPayNoteTransactionAgnosticAndImplicitCoreParticipants() {
        Node document = PayNotes.payNote("iPhone Purchase")
                .attach(CardTransaction.defaultRef())
                .currency(IsoCurrency.USD)
                .amountTotalMinor(80000)
                .participants(p -> p.shipper())
                .participantsUnion(ChannelKey.of("allParticipantsChannel"),
                        PayNoteRole.PAYER, PayNoteRole.PAYEE, PayNoteRole.GUARANTOR, PayNoteRole.SHIPPER)
                .cardCapture().lockOnInit()
                .operation("confirmShipment")
                    .channel(PayNoteRole.SHIPPER)
                    .description("Confirm shipment")
                    .steps(steps -> steps.emitType("ShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                            payload -> payload.put("source", PayNoteRole.SHIPPER.channelKey().value())))
                    .done()
                .cardCapture().unlockWhen(ShippingEvents.ShipmentConfirmed.class)
                .cardCapture().guarantorConfirmCaptureLockedOp()
                .cardCapture().guarantorConfirmCaptureUnlockedOp()
                .buildDocument();

        assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/payerChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/payeeChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/guarantorChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                document.getAsText("/contracts/shipmentCompanyChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                document.getAsText("/contracts/allParticipantsChannel/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED,
                document.getAsText("/contracts/onInitLockCardCapture/steps/0/event/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED,
                document.getAsText("/contracts/unlockCardCaptureWhenEvent/steps/0/event/type/value"));
    }

    @Test
    void cardCaptureDefaultPathFailsFastWithoutAttachedCardRail() {
        IllegalStateException missingRail = assertThrows(IllegalStateException.class, () ->
                PayNotes.payNote("Missing rail")
                        .currency(IsoCurrency.USD)
                        .amountTotalMinor(500)
                        .cardCapture().lockOnInit());
        assertTrue(missingRail.getMessage().contains("No card transaction rail attached"));

        Node explicitPath = PayNotes.payNote("Explicit path")
                .currency(IsoCurrency.USD)
                .amountTotalMinor(500)
                .cardCapture().lockOnInit(DocPath.of("/customCardDetails"))
                .buildDocument();
        assertEquals("${document('/customCardDetails')}",
                explicitPath.getAsText("/contracts/onInitLockCardCapture/steps/0/event/cardTransactionDetails/value"));
    }

    @Test
    void separatesDocumentDefinitionFromMyOsBindings() {
        Node document = PayNotes.payNote("Binding separation")
                .currency(IsoCurrency.EUR)
                .amountTotalMinor(20000)
                .buildDocument();
        assertThrows(IllegalArgumentException.class, () -> document.getAsText("/channelBindings/payerChannel/email/value"));

        Node bootstrap = PayNotes.payNote("Binding separation")
                .currency(IsoCurrency.EUR)
                .amountTotalMinor(20000)
                .bootstrap()
                .bindRole("payer").email("alice@gmail.com")
                .bindRole("payee").accountId("acc_bob_1234")
                .bindRole("guarantor").accountId("acc_bank_1")
                .build();
        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals("alice@gmail.com", bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
    }
}
