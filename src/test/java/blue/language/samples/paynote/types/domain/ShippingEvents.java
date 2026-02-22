package blue.language.samples.paynote.types.domain;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class ShippingEvents {

    private ShippingEvents() {
    }

    @TypeAlias("Shipping/Shipment Confirmed")
    @TypeBlueId("Shipping-Shipment-Confirmed-Demo-BlueId")
    public static class ShipmentConfirmed {
        public String source;
    }

    @TypeAlias("Shipping/Delivery Reported")
    @TypeBlueId("Shipping-Delivery-Reported-Demo-BlueId")
    public static class DeliveryReported {
        public String shipmentId;
        public String status;
    }
}
