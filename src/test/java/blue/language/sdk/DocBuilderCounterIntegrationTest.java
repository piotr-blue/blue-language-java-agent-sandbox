package blue.language.sdk;

import blue.language.model.Node;
import blue.language.processor.DocumentProcessingResult;
import blue.language.processor.DocumentProcessor;
import blue.language.sdk.internal.TypeAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocBuilderCounterIntegrationTest {

    @Test
    void buildsCounterDocumentAndProcessesIncrementOperation() {
        Node ownerChannel = new Node()
                .type(TypeAliases.CONVERSATION_TIMELINE_CHANNEL)
                .properties("timelineId", new Node().value("{{PASTE_TIMELINE_ID_HERE}}"));

        Node built = DocBuilder.doc()
                .name("Counter")
                .set("/counter", 0)
                .channel("ownerChannel", null, ownerChannel)
                .operation("increment")
                    .channel("ownerChannel")
                    .description("Increment the counter by the given number")
                    .requestType(Integer.class)
                    .steps(steps -> steps.replaceExpression(
                            "IncrementCounter",
                            "/counter",
                            "event.message.request + document('/counter')"))
                    .done()
                .operation("decrement")
                    .channel("ownerChannel")
                    .description("Decrement the counter by the given number")
                    .requestType(Integer.class)
                    .steps(steps -> steps.replaceExpression(
                            "DecrementCounter",
                            "/counter",
                            "document('/counter') - event.message.request"))
                    .done()
                .set("/contracts/increment/request/description",
                        "Represents a value by which counter will be incremented")
                .set("/contracts/decrement/request/description", "Value to subtract")
                .buildDocument();

        assertEquals("Counter", built.getName());
        assertEquals(0, built.getAsInteger("/counter/value").intValue());
        assertEquals("Conversation/Timeline Channel", built.getAsText("/contracts/ownerChannel/type/value"));
        assertEquals("{{PASTE_TIMELINE_ID_HERE}}", built.getAsText("/contracts/ownerChannel/timelineId/value"));

        assertEquals("Conversation/Operation", built.getAsText("/contracts/increment/type/value"));
        assertEquals("ownerChannel", built.getAsText("/contracts/increment/channel/value"));
        assertEquals("Increment the counter by the given number", built.getAsText("/contracts/increment/description/value"));
        assertEquals("Represents a value by which counter will be incremented",
                built.getAsText("/contracts/increment/request/description/value"));
        assertEquals("Integer", built.getAsText("/contracts/increment/request/type/value"));

        assertEquals("Conversation/Sequential Workflow Operation", built.getAsText("/contracts/incrementImpl/type/value"));
        assertEquals("increment", built.getAsText("/contracts/incrementImpl/operation/value"));
        assertEquals("${event.message.request + document('/counter')}",
                built.getAsText("/contracts/incrementImpl/steps/0/changeset/0/val/value"));

        assertEquals("Conversation/Operation", built.getAsText("/contracts/decrement/type/value"));
        assertEquals("ownerChannel", built.getAsText("/contracts/decrement/channel/value"));
        assertEquals("Decrement the counter by the given number", built.getAsText("/contracts/decrement/description/value"));
        assertEquals("Value to subtract", built.getAsText("/contracts/decrement/request/description/value"));
        assertEquals("Integer", built.getAsText("/contracts/decrement/request/type/value"));

        assertEquals("Conversation/Sequential Workflow Operation", built.getAsText("/contracts/decrementImpl/type/value"));
        assertEquals("decrement", built.getAsText("/contracts/decrementImpl/operation/value"));
        assertEquals("${document('/counter') - event.message.request}",
                built.getAsText("/contracts/decrementImpl/steps/0/changeset/0/val/value"));

        DocumentProcessor processor = new DocumentProcessor();

        DocumentProcessingResult initialized = processor.initializeDocument(built);

        Node incrementRequestEvent = new Node()
                .type(new Node().blueId("Conversation/Timeline Entry"))
                .properties("eventId", new Node().value("evt-op-1"))
                .properties("timeline", new Node()
                        .properties("timelineId", new Node().value("{{PASTE_TIMELINE_ID_HERE}}")))
                .properties("message", new Node()
                        .type(new Node().blueId("Conversation/Operation Request"))
                        .properties("operation", new Node().value("increment"))
                        .properties("request", new Node().value(10)));

        DocumentProcessingResult processed = processor.processDocument(
                initialized.document(),
                incrementRequestEvent);

        assertEquals(10, processed.document().getAsInteger("/counter/value").intValue());
    }
}
