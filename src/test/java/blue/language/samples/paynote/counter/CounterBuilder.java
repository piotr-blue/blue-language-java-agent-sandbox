package blue.language.samples.paynote.counter;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.ContractsBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.dsl.JsProgram;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CounterBuilder {

    private CounterBuilder() {
    }

    public static CounterDocument baseline(String timelineId) {
        return authoring(timelineId)
                .withIncrement()
                .withDecrement()
                .build();
    }

    public static CounterAuthoring authoring(String timelineId) {
        return new CounterAuthoring(timelineId);
    }

    public static final class CounterAuthoring {
        private final CounterDocument document = new CounterDocument();
        private final Map<String, Node> contractMap = new LinkedHashMap<String, Node>();
        private final ContractsBuilder contractsBuilder = new ContractsBuilder(contractMap);

        private CounterAuthoring(String timelineId) {
            document.name = "Counter";
            document.counter = 0;
            contractsBuilder.timelineChannel("ownerChannel");
            contractMap.get("ownerChannel").properties("timelineId", new Node().value(timelineId));
        }

        public CounterAuthoring withIncrement() {
            contractsBuilder.operation(
                    "increment",
                    "ownerChannel",
                    "Increment the counter by the given number",
                    request -> request.type(TypeAliases.INTEGER).put("description", "Represents a value by which counter will be incremented")
            );
            contractsBuilder.sequentialWorkflowOperation(
                    "incrementImpl",
                    "increment",
                    steps -> steps.updateDocument(changeName("ApplyIncrement"), changeset -> changeset
                            .replaceExpression("/counter", "event.message.request + document('/counter')"))
            );
            return this;
        }

        public CounterAuthoring withDecrement() {
            contractsBuilder.operation(
                    "decrement",
                    "ownerChannel",
                    "Decrement the counter by the given number",
                    request -> request.type(TypeAliases.INTEGER).put("description", "Value to subtract")
            );
            contractsBuilder.sequentialWorkflowOperation(
                    "decrementImpl",
                    "decrement",
                    steps -> steps.updateDocument(changeName("ApplyDecrement"), changeset -> changeset
                            .replaceExpression("/counter", "document('/counter') - event.message.request"))
            );
            return this;
        }

        public CounterAuthoring withCounterChangeNotification() {
            contractsBuilder.putRaw("counterChanged",
                    new Node()
                            .type(TypeAliases.CORE_DOCUMENT_UPDATE_CHANNEL)
                            .properties("path", new Node().value("/counter")));

            contractsBuilder.sequentialWorkflow("onCounterChanged",
                    "counterChanged",
                    new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE),
                    steps -> steps.js("EmitCounterChangeNotification", counterChangeProgram()));
            return this;
        }

        public CounterAuthoring withSayOperation(String channelKey) {
            contractsBuilder.operation(
                    "say",
                    channelKey,
                    "Say something and store it in the counter document",
                    request -> request.type(TypeAliases.TEXT)
            );
            contractsBuilder.sequentialWorkflowOperation("sayImpl", "say", steps -> steps
                    .js("EmitSayEvent", sayEventProgram())
                    .updateDocument("PersistLastSpoken", changeset -> changeset
                            .replaceExpression("/lastSpoken", "event.message.request")));
            return this;
        }

        public CounterDocument build() {
            document.contracts = new Node().properties(contractMap);
            return document;
        }
    }

    public static CounterDocument withExtensions(CounterDocument document, String sayChannel) {
        CounterAuthoring authoring = authoring("timeline-extended");
        authoring.document.name = document.name;
        authoring.document.counter = document.counter;
        authoring.contractMap.clear();
        authoring.contractMap.putAll(document.contracts.getProperties());
        authoring.withCounterChangeNotification().withSayOperation(sayChannel);
        return authoring.build();
    }

    private static JsProgram counterChangeProgram() {
        return BlueDocDsl.js(js -> js.returnObject(
                JsObjectBuilder.object().propArrayRaw("events",
                        "[{ type: 'Counter Changed Notification', value: document('/counter') }]")));
    }

    private static JsProgram sayEventProgram() {
        return BlueDocDsl.js(js -> js.returnObject(
                JsObjectBuilder.object().propArrayRaw("events",
                        "[{ type: 'Counter Said', text: event.message.request }]")));
    }

    private static String changeName(String name) {
        return name;
    }
}
