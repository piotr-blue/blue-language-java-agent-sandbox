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
        Node template = ShipmentCapturePayNoteTemplates.captureOnShipmentTemplate("2026-02-22T12:00:00Z");
        Node specialized = ShipmentCapturePayNoteTemplates.eur200FromChfWithDhl(template, "acc-dhl");
        Node instance = ShipmentCapturePayNoteTemplates.instantiateForAliceBob(
                specialized,
                "acc-alice",
                "acc-bob-merchant",
                "acc-bank");

        assertEquals(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE, template.getAsText("/document/type/value"));
        assertEquals(0, template.getAsInteger("/document/amount/total/value").intValue());
        assertThrows(IllegalArgumentException.class,
                () -> template.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));

        assertEquals(20000, specialized.getAsInteger("/document/amount/total/value").intValue());
        assertEquals("CHF", specialized.getAsText("/document/funding/sourceCurrency/value"));
        assertEquals("acc-dhl", specialized.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));

        assertEquals("acc-alice", instance.getAsText("/channelBindings/payerChannel/accountId/value"));
        assertEquals("acc-bob-merchant", instance.getAsText("/channelBindings/payeeChannel/accountId/value"));
        assertEquals("acc-bank", instance.getAsText("/channelBindings/guarantorChannel/accountId/value"));

        String reserveJs = instance.getAsText("/document/contracts/onInitReserveFunds/steps/0/code/value");
        String captureJs = instance.getAsText("/document/contracts/confirmShipmentImpl/steps/1/code/value");
        assertTrue(reserveJs.contains(PayNoteAliases.RESERVE_FUNDS_REQUESTED));
        assertTrue(captureJs.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                instance.getAsText("/document/contracts/shipmentCompanyChannel/type/value"));
    }
}
