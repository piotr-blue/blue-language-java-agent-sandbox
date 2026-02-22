package blue.language.processor;

import blue.language.Blue;
import blue.language.NodeProvider;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.processor.model.DocumentUpdateChannel;
import blue.language.processor.model.InitializationMarker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractLoaderParityTest {

    @Test
    void loadsBuiltInContractsWithoutRegistryProcessors() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  update:\n" +
                "    type:\n" +
                "      blueId: Core/Document Update Channel\n" +
                "    path: /document\n" +
                "  embedded:\n" +
                "    type:\n" +
                "      blueId: Core/Process Embedded\n" +
                "    paths:\n" +
                "      - /children\n" +
                "  init:\n" +
                "    type:\n" +
                "      blueId: Core/Processing Initialized Marker\n" +
                "    documentId: doc-123\n" +
                "  checkpoint:\n" +
                "    type:\n" +
                "      blueId: Core/Channel Event Checkpoint\n" +
                "    lastEvents: {}\n" +
                "    lastSignatures: {}\n");

        ContractBundle bundle = loader.load(scope, "/");

        ContractBundle.ChannelBinding channelBinding = bundle.channel("update");
        assertNotNull(channelBinding);
        assertTrue(channelBinding.contract() instanceof DocumentUpdateChannel);
        assertEquals("/document", ((DocumentUpdateChannel) channelBinding.contract()).getPath());
        assertEquals(1, bundle.embeddedPaths().size());
        assertEquals("/children", bundle.embeddedPaths().get(0));

        assertTrue(bundle.marker("init") instanceof InitializationMarker);
        assertEquals("doc-123", ((InitializationMarker) bundle.marker("init")).getDocumentId());
        assertTrue(bundle.hasCheckpoint());
    }

    @Test
    void throwsMustUnderstandForUnsupportedContracts() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  unsupported:\n" +
                "    type:\n" +
                "      blueId: Custom.Channel\n");

        assertThrows(MustUnderstandFailureException.class, () -> loader.load(scope, "/"));
    }

    @Test
    void rejectsCheckpointMarkerWithIncorrectKey() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  wrongCheckpoint:\n" +
                "    type:\n" +
                "      blueId: Core/Channel Event Checkpoint\n" +
                "    lastEvents: {}\n" +
                "    lastSignatures: {}\n");

        assertThrows(IllegalStateException.class, () -> loader.load(scope, "/"));
    }

    @Test
    void loadsRegisteredCustomHandlerContracts() {
        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  handler:\n" +
                "    type:\n" +
                "      blueId: SetProperty\n" +
                "    channel: testChannel\n" +
                "    propertyKey: /x\n" +
                "    propertyValue: 1\n");

        ContractBundle bundle = loader.load(scope, "/");

        assertEquals(1, bundle.handlersFor("testChannel").size());
        assertEquals("handler", bundle.handlersFor("testChannel").get(0).key());
    }

    @Test
    void loadsProviderDerivedHandlerContractsUsingTypeChainLookup() {
        NodeProvider provider = new NodeProvider() {
            @Override
            public java.util.List<Node> fetchByBlueId(String blueId) {
                if (!"Derived/SetProperty".equals(blueId)) {
                    return java.util.Collections.emptyList();
                }
                Node definition = new Node().type(new Node().blueId("SetProperty"));
                return java.util.Collections.singletonList(definition);
            }
        };
        Blue blue = new Blue(provider);
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyContractProcessor());
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  testChannel:\n" +
                "    type:\n" +
                "      blueId: TestEventChannel\n" +
                "  handler:\n" +
                "    type:\n" +
                "      blueId: Derived/SetProperty\n" +
                "    channel: testChannel\n" +
                "    propertyKey: /x\n" +
                "    propertyValue: 1\n");

        ContractBundle bundle = loader.load(scope, "/");

        assertEquals(1, bundle.handlersFor("testChannel").size());
        assertEquals("handler", bundle.handlersFor("testChannel").get(0).key());
    }

    @Test
    void failsWhenSequentialWorkflowOmitsChannelAndCannotDerive() {
        Blue blue = new Blue();
        ContractLoader loader = blue.getDocumentProcessor().contractLoader();
        Node scope = blue.yamlToNode("contracts:\n" +
                "  handler:\n" +
                "    type:\n" +
                "      blueId: Conversation/Sequential Workflow\n" +
                "    steps: []\n");

        assertThrows(IllegalStateException.class, () -> loader.load(scope, "/"));
    }
}
