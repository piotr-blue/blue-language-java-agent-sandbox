package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.Map;
import java.util.function.Consumer;

public final class ContractsBuilder {

    private final Map<String, Node> contracts;

    public ContractsBuilder(Map<String, Node> contracts) {
        this.contracts = contracts;
    }

    public ContractsBuilder putRaw(String key, Node contract) {
        contracts.put(key, contract);
        return this;
    }

    public ContractsBuilder timelineChannel(String key) {
        contracts.put(key, new Node().type("MyOS/MyOS Timeline Channel"));
        return this;
    }

    public ContractsBuilder triggeredEventChannel(String key) {
        contracts.put(key, new Node().type("Core/Triggered Event Channel"));
        return this;
    }

    public ContractsBuilder lifecycleEventChannel(String key, String eventTypeAlias) {
        Node channel = new Node().type("Core/Lifecycle Event Channel");
        if (eventTypeAlias != null) {
            channel.properties("event", new Node().type(eventTypeAlias));
        }
        contracts.put(key, channel);
        return this;
    }

    public ContractsBuilder documentLinks(String key, Consumer<DocumentLinksBuilder> customizer) {
        DocumentLinksBuilder linksBuilder = new DocumentLinksBuilder();
        customizer.accept(linksBuilder);
        contracts.put(key, linksBuilder.build());
        return this;
    }

    public ContractsBuilder operation(String key,
                                      String channel,
                                      String description,
                                      Consumer<NodeObjectBuilder> requestCustomizer) {
        Node operation = new Node().type("Conversation/Operation");
        if (description != null) {
            operation.properties("description", new Node().value(description));
        }
        operation.properties("channel", new Node().value(channel));
        if (requestCustomizer != null) {
            NodeObjectBuilder requestBuilder = NodeObjectBuilder.create();
            requestCustomizer.accept(requestBuilder);
            operation.properties("request", requestBuilder.build());
        }
        contracts.put(key, operation);
        return this;
    }

    public ContractsBuilder operation(String key, String channel, String description) {
        return operation(key, channel, description, null);
    }

    public ContractsBuilder sequentialWorkflowOperation(String key,
                                                        String operationName,
                                                        Consumer<StepsBuilder> customizer) {
        StepsBuilder stepsBuilder = new StepsBuilder();
        customizer.accept(stepsBuilder);

        Node workflow = new Node().type("Conversation/Sequential Workflow Operation");
        workflow.properties("operation", new Node().value(operationName));
        workflow.properties("steps", new Node().items(stepsBuilder.build()));
        contracts.put(key, workflow);
        return this;
    }

    public ContractsBuilder sequentialWorkflow(String key,
                                               String channel,
                                               Node event,
                                               Consumer<StepsBuilder> customizer) {
        StepsBuilder stepsBuilder = new StepsBuilder();
        customizer.accept(stepsBuilder);

        Node workflow = new Node().type("Conversation/Sequential Workflow");
        workflow.properties("channel", new Node().value(channel));
        if (event != null) {
            workflow.properties("event", event);
        }
        workflow.properties("steps", new Node().items(stepsBuilder.build()));
        contracts.put(key, workflow);
        return this;
    }
}
