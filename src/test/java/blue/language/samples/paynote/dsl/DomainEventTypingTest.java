package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainEventTypingTest {

    @Test
    void emitsTypedShipmentEventsWithoutConversationKindFallback() {
        Node step = new StepsBuilder()
                .emitType("EmitShipmentConfirmed", ShippingEvents.ShipmentConfirmed.class,
                        payload -> payload.put("source", "shipmentCompanyChannel"))
                .build()
                .get(0);

        assertEquals(TypeAliases.CONVERSATION_TRIGGER_EVENT, step.getAsText("/type/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED, step.getAsText("/event/type/value"));
        assertEquals("Shipping-Shipment-Confirmed-Demo-BlueId", step.getAsText("/event/type/blueId"));
        assertEquals("shipmentCompanyChannel", step.getAsText("/event/source/value"));
    }
}
