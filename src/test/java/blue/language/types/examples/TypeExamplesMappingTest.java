package blue.language.types.examples;

import blue.language.Blue;
import blue.language.mapping.NodeToObjectConverter;
import blue.language.model.Node;
import blue.language.utils.TypeClassResolver;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TypeExamplesMappingTest {

    @Test
    void deepChannelAndOperationChainsResolveToMostSpecificTypes() {
        Blue blue = new Blue();
        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.types.examples")
        );

        Node channelNode = blue.yamlToNode(
                "type:\n" +
                        "  blueId: DemoMyOsTimelineChannel\n" +
                        "order: 1\n" +
                        "channelName: timeline\n" +
                        "timeline: events\n" +
                        "os: linux\n"
        );
        DemoContract contractChannel = converter.convert(channelNode, DemoContract.class);
        assertInstanceOf(DemoMyOsTimelineChannel.class, contractChannel);
        DemoMyOsTimelineChannel typedChannel = (DemoMyOsTimelineChannel) contractChannel;
        assertEquals(Integer.valueOf(1), typedChannel.order);
        assertEquals("timeline", typedChannel.channelName);
        assertEquals("events", typedChannel.timeline);
        assertEquals("linux", typedChannel.os);

        Node operationNode = blue.yamlToNode(
                "type:\n" +
                        "  blueId: DemoMyOsOperation\n" +
                        "order: 2\n" +
                        "markerKind: operation\n" +
                        "operationName: sync\n" +
                        "executionMode: safe\n"
        );
        DemoContract contractOperation = converter.convert(operationNode, DemoContract.class);
        assertInstanceOf(DemoMyOsOperation.class, contractOperation);
        DemoMyOsOperation typedOperation = (DemoMyOsOperation) contractOperation;
        assertEquals(Integer.valueOf(2), typedOperation.order);
        assertEquals("operation", typedOperation.markerKind);
        assertEquals("sync", typedOperation.operationName);
        assertEquals("safe", typedOperation.executionMode);
    }

    @Test
    void simpleDocumentMapsNestedContracts() {
        Blue blue = new Blue();
        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.types.examples")
        );

        DemoSimpleDocument simple = new DemoSimpleDocument();
        simple.counter = 7;
        simple.channel = new DemoMyOsTimelineChannel();
        simple.channel.order = 11;
        simple.channel.channelName = "timeline";
        simple.channel.timeline = "entries";
        simple.channel.os = "macos";
        simple.operation = new DemoMyOsOperation();
        simple.operation.order = 12;
        simple.operation.markerKind = "op";
        simple.operation.operationName = "reindex";
        simple.operation.executionMode = "fast";

        Node node = blue.objectToNode(simple);
        assertEquals("DemoSimpleDocument", node.getType().getBlueId());
        DemoSimpleDocument converted = converter.convert(node, DemoSimpleDocument.class);
        assertNotNull(converted);
        assertEquals(Integer.valueOf(7), converted.counter);
        assertNotNull(converted.channel);
        assertEquals("entries", converted.channel.timeline);
        assertEquals("macos", converted.channel.os);
        assertNotNull(converted.operation);
        assertEquals("reindex", converted.operation.operationName);
        assertEquals("fast", converted.operation.executionMode);
    }

    @Test
    void complexDocumentSupportsNestedContractNodesAndConstrainedChain() {
        Blue blue = new Blue();
        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.types.examples")
        );

        DemoComplexDocument doc = new DemoComplexDocument();
        doc.root = new DemoSimpleDocument();
        doc.root.counter = 3;
        doc.root.channel = new DemoMyOsTimelineChannel();
        doc.root.channel.order = 1;
        doc.root.channel.channelName = "timeline";
        doc.root.channel.timeline = "events";
        doc.root.channel.os = "linux";
        doc.root.operation = new DemoMyOsOperation();
        doc.root.operation.order = 2;
        doc.root.operation.markerKind = "operation";
        doc.root.operation.operationName = "snapshot";
        doc.root.operation.executionMode = "safe";

        doc.constrained = new DemoConstrainedC();
        doc.constrained.x = 1;
        doc.constrained.label = "base";
        doc.constrained.y = 2;
        doc.constrained.extra = "deep";

        doc.contracts = Arrays.<DemoContract>asList(doc.root.channel, doc.root.operation);

        Node node = blue.objectToNode(doc);
        assertEquals("DemoComplexDocument", node.getType().getBlueId());
        DemoComplexDocument converted = converter.convert(node, DemoComplexDocument.class);
        assertNotNull(converted.root);
        assertNotNull(converted.constrained);
        assertEquals(Integer.valueOf(1), converted.constrained.x);
        assertEquals(Integer.valueOf(2), converted.constrained.y);
        assertEquals("deep", converted.constrained.extra);
        assertNotNull(converted.contracts);
        assertEquals(2, converted.contracts.size());
        assertInstanceOf(DemoMyOsTimelineChannel.class, converted.contracts.get(0));
        assertInstanceOf(DemoMyOsOperation.class, converted.contracts.get(1));
    }
}
