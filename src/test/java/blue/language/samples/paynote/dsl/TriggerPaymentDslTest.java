package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.types.payments.PaymentRequests;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TriggerPaymentDslTest {

    @Test
    void triggerPaymentBuildsTypedEventWithProcessorAndPayload() {
        StepsBuilder steps = new StepsBuilder();

        steps.triggerPayment("TriggerAch",
                PaymentRequests.AchTransferRequested.class,
                payload -> payload
                        .processor("guarantorChannel")
                        .currency("EUR")
                        .amountMinor(1999)
                        .routingNumber("DE111")
                        .accountNumber("DE222"));

        Node root = new Node().items(steps.build());
        assertEquals(TypeAliases.CONVERSATION_TRIGGER_EVENT, root.getAsText("/0/type/value"));
        assertEquals(TypeAliases.PAYMENTS_ACH_TRANSFER_REQUESTED, root.getAsText("/0/event/type/value"));
        assertEquals("guarantorChannel", root.getAsText("/0/event/processor/value"));
        assertEquals("EUR", root.getAsText("/0/event/currency/value"));
    }

    @Test
    void triggerPaymentRequiresProcessor() {
        StepsBuilder steps = new StepsBuilder();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> steps.triggerPayment(
                PaymentRequests.InternalLedgerTransferRequested.class,
                payload -> payload.amountMinor(500)));
        assertEquals("triggerPayment requires non-empty processor field", ex.getMessage());
    }

    @Test
    void triggerPaymentRejectsCustomProcessorOverride() {
        StepsBuilder steps = new StepsBuilder();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> steps.triggerPayment(
                PaymentRequests.InternalLedgerTransferRequested.class,
                payload -> payload
                        .putCustom("processor", "guarantorChannel")
                        .amountMinor(500)));
        assertEquals("Use processor(...) to set processor", ex.getMessage());
    }
}
