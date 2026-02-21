package blue.language.samples.paynote.sdk;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import blue.language.processor.contracts.SetPropertyContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.TestEvent;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PayNoteSdkSimpleFlowTest {

    @Test
    void capturesWhenEventArrivesOnConfiguredChannel() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());

        PayNoteDraft payNote = PayNoteBuilder.create("Simple Capture PayNote", 1200, "USD")
                .withStandardParties("payer-timeline", "payee-timeline", "guarantor-timeline")
                .captureWhenEventArrives("captureTriggerChannel")
                .build();

        Node document = blue.objectToNode(payNote);
        assertNotNull(document.getProperties().get("contracts").getProperties().get("captureTriggerChannel"));

        DocumentProcessingResult initialized = blue.initializeDocument(document);
        DocumentProcessingResult processed = blue.processDocument(
                initialized.document(),
                blue.objectToNode(new TestEvent().eventId("evt-simple-1"))
        );

        Node processedDoc = processed.document();
        assertNotNull(processedDoc.getProperties().get("captureRequested"));
        assertEquals(BigInteger.ONE, processedDoc.getProperties().get("captureRequested").getValue());
    }
}
