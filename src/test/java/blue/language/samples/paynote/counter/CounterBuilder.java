package blue.language.samples.paynote.counter;

import blue.language.model.Node;
import blue.language.samples.paynote.types.core.CoreTypes;

import java.util.Arrays;

public final class CounterBuilder {

    private CounterBuilder() {
    }

    public static CounterDocument baseline(String timelineId) {
        CounterDocument document = new CounterDocument();
        document.name = "Counter";
        document.counter = 0;
        document.contracts = new CounterDocument.CounterContracts();

        CounterDocument.CounterTimelineChannel ownerChannel = new CounterDocument.CounterTimelineChannel();
        ownerChannel.timelineId = timelineId;
        document.contracts.ownerChannel = ownerChannel;

        CounterDocument.CounterOperation increment = new CounterDocument.CounterOperation();
        increment.description = "Increment the counter by the given number";
        increment.channel = "ownerChannel";
        increment.request = new Node().type("Integer");
        document.contracts.increment = increment;

        CounterDocument.CounterSequentialWorkflowOperation incrementImpl = new CounterDocument.CounterSequentialWorkflowOperation();
        incrementImpl.operation = "increment";
        incrementImpl.steps = Arrays.asList(updateCounterStep("${event.message.request + document('/counter')}"));
        document.contracts.incrementImpl = incrementImpl;

        CounterDocument.CounterOperation decrement = new CounterDocument.CounterOperation();
        decrement.description = "Decrement the counter by the given number";
        decrement.channel = "ownerChannel";
        decrement.request = new Node().type("Integer");
        document.contracts.decrement = decrement;

        CounterDocument.CounterSequentialWorkflowOperation decrementImpl = new CounterDocument.CounterSequentialWorkflowOperation();
        decrementImpl.operation = "decrement";
        decrementImpl.steps = Arrays.asList(updateCounterStep("${document('/counter') - event.message.request}"));
        document.contracts.decrementImpl = decrementImpl;

        return document;
    }

    public static CounterDocument withExtensions(CounterDocument document, String sayChannel) {
        CoreTypes.DocumentUpdateChannel counterChanged = new CoreTypes.DocumentUpdateChannel();
        counterChanged.path = "/counter";
        document.contracts.counterChanged = counterChanged;

        CounterDocument.CounterSequentialWorkflow onCounterChanged = new CounterDocument.CounterSequentialWorkflow();
        onCounterChanged.channel = "counterChanged";
        onCounterChanged.event = new Node().type("Core/Document Update");
        onCounterChanged.steps = Arrays.asList(
                jsStep("return { events: [{ type: 'Counter Changed Notification', value: document('/counter') }] };")
        );
        document.contracts.onCounterChanged = onCounterChanged;

        CounterDocument.CounterOperation say = new CounterDocument.CounterOperation();
        say.description = "Say something and store it in the counter document";
        say.channel = sayChannel;
        say.request = new Node().type("Text");
        document.contracts.say = say;

        CounterDocument.CounterSequentialWorkflowOperation sayImpl = new CounterDocument.CounterSequentialWorkflowOperation();
        sayImpl.operation = "say";
        sayImpl.steps = Arrays.asList(
                jsStep("return { events: [{ type: 'Counter Said', text: event.message.request }] };"),
                updatePathStep("/lastSpoken", "${event.message.request}")
        );
        document.contracts.sayImpl = sayImpl;

        return document;
    }

    private static Node updateCounterStep(String expression) {
        return updatePathStep("/counter", expression);
    }

    private static Node updatePathStep(String path, String expression) {
        return new Node()
                .type("Conversation/Update Document")
                .properties("changeset", new Node().items(
                        new Node()
                                .properties("op", new Node().value("replace"))
                                .properties("path", new Node().value(path))
                                .properties("val", new Node().value(expression))
                ));
    }

    private static Node jsStep(String code) {
        return new Node()
                .type("Conversation/JavaScript Code")
                .properties("code", new Node().value(code));
    }
}
