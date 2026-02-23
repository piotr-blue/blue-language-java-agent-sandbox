package blue.language.samples.paynote.examples.shipment;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipmentPayNoteChainTest {

    @Test
    void classBasedTemplateSpecializeInstantiateFlowBuildsExpectedShapes() {
        Node baseTemplate = ShipmentPayNote.template("2026-02-23T10:00:00Z").build();
        Node specialized = DHLShipmentPayNote.template("2026-02-23T10:00:00Z").build();
        Node finalInstance = AliceBobShipmentPayNote.build("2026-02-23T10:00:00Z");

        assertEquals(PayNoteAliases.PAYNOTE, baseTemplate.getAsText("/document/type/value"));
        assertEquals(80000, baseTemplate.getAsInteger("/document/amount/total/value").intValue());
        assertThrows(IllegalArgumentException.class,
                () -> baseTemplate.getAsText("/channelBindings/payerChannel/email/value"));

        assertEquals("EUR", specialized.getAsText("/document/currency/value"));
        assertEquals(20000, specialized.getAsInteger("/document/amount/total/value").intValue());
        assertEquals("CHF", specialized.getAsText("/document/funding/sourceCurrency/value"));
        assertEquals("acc_dhl_001",
                specialized.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));
        assertThrows(IllegalArgumentException.class,
                () -> specialized.getAsText("/channelBindings/payerChannel/email/value"));

        assertEquals("alice@gmail.com", finalInstance.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("acc_bob_1234", finalInstance.getAsText("/channelBindings/payeeChannel/accountId/value"));
        assertEquals("acc_bank_1", finalInstance.getAsText("/channelBindings/guarantorChannel/accountId/value"));
        assertEquals(TypeAliases.SHIPPING_SHIPMENT_CONFIRMED,
                finalInstance.getAsText("/document/contracts/confirmShipmentImpl/steps/0/event/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION,
                finalInstance.getAsText("/document/contracts/cancel/type/value"));
    }

    @Test
    void supportsNodeBasedChainingFromBaseTemplateNode() {
        Node baseTemplateNode = ShipmentPayNote.template("2026-02-23T11:00:00Z").build();
        Node specializedNode = ShipmentPayNoteNodeChaining.specializeEur200ChfDhl(baseTemplateNode);
        Node instanceNode = ShipmentPayNoteNodeChaining.instantiateAliceBob(specializedNode);

        assertEquals(80000, baseTemplateNode.getAsInteger("/document/amount/total/value").intValue());
        assertEquals(20000, specializedNode.getAsInteger("/document/amount/total/value").intValue());
        assertEquals("CHF", specializedNode.getAsText("/document/funding/sourceCurrency/value"));
        assertEquals("acc_dhl_001",
                specializedNode.getAsText("/channelBindings/shipmentCompanyChannel/accountId/value"));
        assertEquals("alice@gmail.com", instanceNode.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("acc_bob_1234", instanceNode.getAsText("/channelBindings/payeeChannel/accountId/value"));
    }
}
