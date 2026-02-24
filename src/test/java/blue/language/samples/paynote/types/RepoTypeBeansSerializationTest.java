package blue.language.samples.paynote.types;

import blue.language.Blue;
import blue.language.mapping.NodeToObjectConverter;
import blue.language.model.Node;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.core.CoreTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.utils.TypeClassResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepoTypeBeansSerializationTest {

    private final Blue blue = new Blue();

    @Test
    void serializesCoreTypeBlueId() {
        CoreTypes.DocumentUpdateChannel channel = new CoreTypes.DocumentUpdateChannel();
        channel.path = "/counter";

        Node node = blue.objectToNode(channel);

        assertEquals("6H1iGrDAcqtFE1qv3iyMTj79jCZsMUMxsNUzqYSJNbyR", node.getType().getBlueId());
        assertEquals("/counter", node.getProperties().get("path").getValue());
    }

    @Test
    void serializesAndMapsBlueNameAndDescriptionConvention() {
        PayNoteTypes.CaptureFundsRequested event = new PayNoteTypes.CaptureFundsRequested();
        event.amountName = "Requested amount";
        event.amountDescription = "Minor currency units to capture";
        event.amount = 1200;

        Node serialized = blue.objectToNode(event);
        assertEquals("DvxKVEFsDmgA1hcBDfh7t42NgTRLaxXjCrB48DufP3i3", serialized.getType().getBlueId());
        assertEquals("Requested amount", serialized.getAsText("/amount/name"));
        assertEquals("Minor currency units to capture", serialized.getAsText("/amount/description"));
        assertEquals(Integer.valueOf(1200), serialized.getAsInteger("/amount/value"));

        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.samples.paynote.types.paynote"));
        PayNoteTypes.CaptureFundsRequested mapped = converter.convert(serialized, PayNoteTypes.CaptureFundsRequested.class);
        assertEquals("Requested amount", mapped.amountName);
        assertEquals("Minor currency units to capture", mapped.amountDescription);
        assertEquals(Integer.valueOf(1200), mapped.amount);
    }

    @Test
    void serializesConversationOperationWithTypedRequestPayload() {
        ConversationTypes.Operation operation = new ConversationTypes.Operation();
        operation.channel = "ownerChannel";
        operation.request = new Node().value(1);

        Node node = blue.objectToNode(operation);

        assertEquals("BoAiqVUZv9Fum3wFqaX2JnQMBHJLxJSo2V9U2UBmCfsC", node.getType().getBlueId());
        assertEquals("ownerChannel", node.getProperties().get("channel").getValue());
        assertEquals(Integer.valueOf(1), node.getAsInteger("/request"));
    }
}
