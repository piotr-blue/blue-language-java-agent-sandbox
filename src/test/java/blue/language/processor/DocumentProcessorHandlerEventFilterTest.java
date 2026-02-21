package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.TestEvent;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentProcessorHandlerEventFilterTest {

    @Test
    void handlerEventFilterRestrictsExecutionToMatchingEvents() {
        String yaml = "name: Handler Filter Doc\n" +
                "contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  onTestEvent:\n" +
                "    channel: testChannel\n" +
                "    type:\n" +
                "      blueId: SetProperty\n" +
                "    event:\n" +
                "      type:\n" +
                "        blueId: TestEvent\n" +
                "    propertyKey: /matched\n" +
                "    propertyValue: 1\n" +
                "  onOtherEvent:\n" +
                "    order: 1\n" +
                "    channel: testChannel\n" +
                "    type:\n" +
                "      blueId: SetProperty\n" +
                "    event:\n" +
                "      type:\n" +
                "        blueId: OtherEvent\n" +
                "    propertyKey: /unmatched\n" +
                "    propertyValue: 1\n";

        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());

        Node initialized = blue.initializeDocument(blue.yamlToNode(yaml)).document().clone();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-1"));

        DocumentProcessingResult result = blue.processDocument(initialized, event);
        Node processed = result.document();

        assertEquals(BigInteger.ONE, processed.getProperties().get("matched").getValue());
        assertNull(processed.getProperties().get("unmatched"));
    }
}
