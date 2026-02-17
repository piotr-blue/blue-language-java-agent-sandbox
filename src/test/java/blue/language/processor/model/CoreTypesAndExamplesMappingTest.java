package blue.language.processor.model;

import blue.language.Blue;
import blue.language.mapping.NodeToObjectConverter;
import blue.language.model.BlueAnnotationsBeanSerializerModifier;
import blue.language.model.Node;
import blue.language.processor.model.core.CoreDocumentUpdateChannelType;
import blue.language.processor.model.examples.ExampleChannelBase;
import blue.language.processor.model.examples.ExampleDocumentBase;
import blue.language.processor.model.examples.ExamplePremiumOrderDocument;
import blue.language.processor.model.examples.ExampleSecureWebhookChannel;
import blue.language.utils.TypeClassResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CoreTypesAndExamplesMappingTest {

    @Test
    void coreTypeBeansSerializeWithTypeBlueIdMetadata() throws Exception {
        ObjectMapper mapper = mapperWithBlueAnnotations();
        CoreDocumentUpdateChannelType channel = new CoreDocumentUpdateChannelType();
        channel.setPath("/orders");

        Node node = mapper.readValue(mapper.writeValueAsString(channel), Node.class);
        assertNotNull(node.getType());
        assertEquals("Core.DocumentUpdateChannel", node.getType().getBlueId());
        assertEquals("/orders", node.getProperties().get("path").getValue());
    }

    @Test
    void converterLoadsMultiLevelContractSubtype() {
        Blue blue = new Blue();
        Node node = blue.yamlToNode(
                "type:\n" +
                        "  blueId: Example.Channel.Webhook.Secure\n" +
                        "eventType: OrderPlaced\n" +
                        "route: /events/orders\n" +
                        "signatureAlgorithm: HMAC-SHA256\n"
        );

        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.processor.model.examples")
        );
        ExampleChannelBase converted = converter.convert(node, ExampleChannelBase.class);

        assertInstanceOf(ExampleSecureWebhookChannel.class, converted);
        ExampleSecureWebhookChannel secure = (ExampleSecureWebhookChannel) converted;
        assertEquals("OrderPlaced", secure.eventType);
        assertEquals("/events/orders", secure.route);
        assertEquals("HMAC-SHA256", secure.signatureAlgorithm);
    }

    @Test
    void documentExamplesSupportBlueFieldAnnotationsAndSubtypeResolution() {
        Blue blue = new Blue();
        ExamplePremiumOrderDocument input = new ExamplePremiumOrderDocument();
        input.template = "Template-BlueId";
        input.itemsName = "Order items";
        input.itemsDescription = "Items selected by customer";
        input.lineItems = Arrays.asList("sku-1", "sku-2");
        input.customer = "alice";
        input.amount = 3;
        input.priority = Boolean.TRUE;

        Node node = blue.objectToNode(input);
        assertNotNull(node.getProperties());
        assertEquals("Example.Document.Order.Premium", node.getType().getBlueId());
        Node templateNode = node.getProperties().get("template");
        assertNotNull(templateNode);
        assertEquals("Template-BlueId", templateNode.getBlueId());
        Node itemsNode = node.getProperties().get("lineItems");
        assertNotNull(itemsNode);
        assertEquals("Order items", itemsNode.getName());
        assertEquals("Items selected by customer", itemsNode.getDescription());
        assertNotNull(itemsNode.getItems());
        assertEquals("sku-1", itemsNode.getItems().get(0).getValue());
        assertEquals("sku-2", itemsNode.getItems().get(1).getValue());

        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.processor.model.examples")
        );
        ExampleDocumentBase converted = converter.convert(node, ExampleDocumentBase.class);

        assertInstanceOf(ExamplePremiumOrderDocument.class, converted);
        ExamplePremiumOrderDocument premium = (ExamplePremiumOrderDocument) converted;
        assertEquals("Template-BlueId", premium.template);
        assertEquals("Order items", premium.itemsName);
        assertEquals("Items selected by customer", premium.itemsDescription);
        assertEquals("alice", premium.customer);
        assertEquals(Integer.valueOf(3), premium.amount);
        assertEquals(Boolean.TRUE, premium.priority);
    }

    @Test
    void typeClassResolverDiscoversCoreTypeBeans() {
        TypeClassResolver resolver = new TypeClassResolver("blue.language.processor.model.core");
        assertEquals(CoreDocumentUpdateChannelType.class,
                resolver.getBlueIdMap().get("Core.DocumentUpdateChannel"));

        Node typed = new Node().type(new Node().blueId("Core.DocumentUpdateChannel"));
        assertEquals(CoreDocumentUpdateChannelType.class, resolver.resolveClass(typed));
    }

    private ObjectMapper mapperWithBlueAnnotations() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new BlueAnnotationsBeanSerializerModifier());
        mapper.registerModule(module);
        return mapper;
    }
}
