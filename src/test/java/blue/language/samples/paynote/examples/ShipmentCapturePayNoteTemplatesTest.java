package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipmentCapturePayNoteTemplatesTest {

    @Test
    void supportsTemplateSpecializeAndInstantiateStages() {
        Node templateNode = ShipmentCapturePayNoteTemplates.shipmentEscrowTemplate("2026-02-22T12:00:00Z");
        Node specializedNode = ShipmentCapturePayNoteTemplates.eur200FromChfWithDhl(templateNode);
        Node instance = ShipmentCapturePayNoteTemplates.instantiateForAliceBob(
                specializedNode,
                "acc-dhl",
                "acc-alice",
                "acc-bob-merchant",
                "acc-bank");

        assertEquals(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE, templateNode.getAsText("/type/value"));
        assertEquals(0, templateNode.getAsInteger("/amount/total/value").intValue());
        assertThrows(IllegalArgumentException.class,
                () -> templateNode.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));

        assertEquals(20000, specializedNode.getAsInteger("/amount/total/value").intValue());
        assertEquals("CHF", specializedNode.getAsText("/funding/sourceCurrency/value"));
        assertThrows(IllegalArgumentException.class,
                () -> specializedNode.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));

        assertEquals("acc-dhl", instance.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));
        assertEquals("acc-alice", instance.getAsText("/channelBindings/payerChannel/accountId/value"));
        assertEquals("acc-bob-merchant", instance.getAsText("/channelBindings/payeeChannel/accountId/value"));
        assertEquals("acc-bank", instance.getAsText("/channelBindings/guarantorChannel/accountId/value"));

        String reserveJs = instance.getAsText("/document/contracts/onInitReserveFunds/steps/0/code/value");
        String captureJs = instance.getAsText("/document/contracts/confirmShipmentImpl/steps/1/code/value");
        assertTrue(reserveJs.contains(PayNoteAliases.RESERVE_FUNDS_REQUESTED));
        assertTrue(captureJs.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                instance.getAsText("/document/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                instance.getAsText("/document/contracts/shipmentCompanyChannel/type/value"));
    }
}
