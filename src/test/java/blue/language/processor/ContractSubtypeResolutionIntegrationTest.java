package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.AdvancedSetProperty;
import blue.language.processor.model.DeepAdvancedSetProperty;
import blue.language.processor.model.ExtendedProcessingFailureMarker;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.JsonPatch;
import blue.language.processor.model.MarkerContract;
import blue.language.processor.model.TestEvent;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractSubtypeResolutionIntegrationTest {

    @Test
    void deepHandlerSubtypeIsExecutedByRegisteredBaseProcessor() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new SetPropertyContractProcessor());

        Node document = blue.yamlToNode(
                "name: Handler Subtype Doc\n" +
                        "contracts:\n" +
                        "  lifecycleChannel:\n" +
                        "    type:\n" +
                        "      blueId: LifecycleChannel\n" +
                        "  setX:\n" +
                        "    channel: lifecycleChannel\n" +
                        "    type:\n" +
                        "      blueId: DeepAdvancedSetProperty\n" +
                        "    event:\n" +
                        "      type:\n" +
                        "        blueId: DocumentProcessingInitiated\n" +
                        "    propertyKey: /x\n" +
                        "    propertyValue: 42\n"
        );

        DocumentProcessingResult init = blue.initializeDocument(document);
        assertEquals(new BigInteger("42"), init.document().getProperties().get("x").getValue());
    }

    @Test
    void deepChannelSubtypeIsMatchedByRegisteredBaseChannelProcessor() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());

        Node document = blue.yamlToNode(
                "name: Channel Subtype Doc\n" +
                        "contracts:\n" +
                        "  external:\n" +
                        "    type:\n" +
                        "      blueId: DeepAdvancedTestEventChannel\n" +
                        "    eventType: TestEvent\n" +
                        "  setX:\n" +
                        "    channel: external\n" +
                        "    type:\n" +
                        "      blueId: SetProperty\n" +
                        "    propertyKey: /x\n" +
                        "    propertyValue: 11\n"
        );

        Node initialized = blue.initializeDocument(document).document();
        Node event = blue.objectToNode(new TestEvent().eventId("evt-1"));
        DocumentProcessingResult result = blue.processDocument(initialized, event);

        assertEquals(new BigInteger("11"), result.document().getProperties().get("x").getValue());
    }

    @Test
    void registrySelectsMostSpecificRegisteredHandlerForSubtype() {
        ContractProcessorRegistry registry = ContractProcessorRegistryBuilder.create()
                .registerDefaults()
                .register(new SetPropertyContractProcessor())
                .register(new AdvancedSetPropertyOverrideProcessor())
                .build();

        Blue blue = new Blue().documentProcessor(new DocumentProcessor(registry));
        Node document = blue.yamlToNode(
                "name: Specificity Doc\n" +
                        "contracts:\n" +
                        "  lifecycleChannel:\n" +
                        "    type:\n" +
                        "      blueId: LifecycleChannel\n" +
                        "  setX:\n" +
                        "    channel: lifecycleChannel\n" +
                        "    type:\n" +
                        "      blueId: DeepAdvancedSetProperty\n" +
                        "    event:\n" +
                        "      type:\n" +
                        "        blueId: DocumentProcessingInitiated\n" +
                        "    propertyKey: /x\n" +
                        "    propertyValue: 1\n"
        );

        DocumentProcessingResult init = blue.initializeDocument(document);
        assertEquals(new BigInteger("1001"), init.document().getProperties().get("x").getValue());
    }

    @Test
    void markerSubtypeIsLoadedAsMarkerContract() {
        Blue blue = new Blue();
        DocumentProcessor processor = new DocumentProcessor();
        Node document = blue.yamlToNode(
                "name: Marker Subtype Doc\n" +
                        "contracts:\n" +
                        "  failure:\n" +
                        "    type:\n" +
                        "      blueId: ExtendedProcessingFailureMarker\n" +
                        "    code: RuntimeFatal\n"
        );

        Map<String, MarkerContract> markers = processor.markersFor(document, "/");
        assertTrue(markers.containsKey("failure"));
        assertInstanceOf(ExtendedProcessingFailureMarker.class, markers.get("failure"));
    }

    private static final class AdvancedSetPropertyOverrideProcessor
            implements HandlerProcessor<AdvancedSetProperty> {

        @Override
        public Class<AdvancedSetProperty> contractType() {
            return AdvancedSetProperty.class;
        }

        @Override
        public void execute(AdvancedSetProperty contract, ProcessorExecutionContext context) {
            String key = contract.getPropertyKey() != null ? contract.getPropertyKey() : "/x";
            String target = context.resolvePointer(key);
            int source = contract.getPropertyValue();
            Node value = new Node().value(source + 1000);
            JsonPatch patch = context.documentContains(target)
                    ? JsonPatch.replace(target, value)
                    : JsonPatch.add(target, value);
            context.applyPatch(patch);
        }
    }
}
