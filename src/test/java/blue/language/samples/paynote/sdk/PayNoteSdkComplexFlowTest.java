package blue.language.samples.paynote.sdk;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import blue.language.processor.contracts.EmitEventsContractProcessor;
import blue.language.processor.contracts.SetPropertyContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.TestEvent;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteSdkComplexFlowTest {

    @Test
    void routesEventsThroughParentAndChildPayNotesWithChildSpecificLogic() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());
        blue.registerContractProcessor(new EmitEventsContractProcessor());

        PayNoteDraft flightChild = PayNoteBuilder.create("Flight Child PayNote", 50000, "USD")
                .captureWhenEventArrives("childTriggerChannel")
                .build();
        PayNoteDraft hotelChild = PayNoteBuilder.create("Hotel Child PayNote", 80000, "USD")
                .flagOnEvent("childTriggerChannel", "hotelRefundFlag", "/refundRequested", 1)
                .build();

        PayNoteDraft parent = PayNoteBuilder.create("Parent Travel Budget", 200000, "USD")
                .withStandardParties("payer-parent", "payee-parent", "guarantor-parent")
                .addChild("flight", flightChild)
                .addChild("hotel", hotelChild)
                .processEmbeddedChildren()
                .flagOnEvent("parentTriggerChannel", "markChildrenIssued", "/childrenIssued", 1)
                .emitChildIssuedEvents("parentTriggerChannel", "emitChildIssued")
                .build();

        Node parentNode = blue.objectToNode(parent);
        DocumentProcessingResult initialized = blue.initializeDocument(parentNode);
        DocumentProcessingResult processed = blue.processDocument(
                initialized.document(),
                blue.objectToNode(new TestEvent().eventId("evt-complex-1"))
        );

        Node processedDoc = processed.document();
        assertEquals(BigInteger.ONE, processedDoc.getProperties().get("childrenIssued").getValue());
        assertEquals(BigInteger.ONE, processedDoc.getProperties()
                .get("children")
                .getProperties()
                .get("flight")
                .getProperties()
                .get("captureRequested")
                .getValue());
        assertEquals(BigInteger.ONE, processedDoc.getProperties()
                .get("children")
                .getProperties()
                .get("hotel")
                .getProperties()
                .get("refundRequested")
                .getValue());

        int issuedEvents = 0;
        for (Node event : processed.triggeredEvents()) {
            if (event.getType() != null
                    && "FAZCx2s5eq9zPV64LdHNFYbjjxD3ci1ZqyTcQk5WhXAs".equals(event.getType().getBlueId())) {
                issuedEvents++;
            }
        }
        assertTrue(issuedEvents >= 2);
    }
}
