package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.payments.PaymentRequests;
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
                        .put("processor", "guarantorChannel")
                        .put("currency", "EUR")
                        .put("amount", 1999)
                        .put("sourceIban", "DE111")
                        .put("destinationIban", "DE222"));

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
                payload -> payload.put("amount", 500)));
        assertEquals("triggerPayment requires non-empty processor field", ex.getMessage());
    }
}
