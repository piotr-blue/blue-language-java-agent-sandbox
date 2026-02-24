package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.ArrayList;
import java.util.List;
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
        contracts.put(key, new Node().type(TypeAliases.CONVERSATION_TIMELINE_CHANNEL));
        return this;
    }

    public ContractsBuilder timelineChannels(String... keys) {
        if (keys == null) {
            return this;
        }
        for (String key : keys) {
            timelineChannel(key);
        }
        return this;
    }

    public ContractsBuilder myOsTimelineChannel(String key) {
        contracts.put(key, new Node().type(TypeAliases.MYOS_TIMELINE_CHANNEL));
        return this;
    }

    public ContractsBuilder myOsTimelineChannels(String... keys) {
        if (keys == null) {
            return this;
        }
        for (String key : keys) {
            myOsTimelineChannel(key);
        }
        return this;
    }

    public ContractsBuilder compositeTimelineChannel(String key, String... channelKeys) {
        Node composite = new Node().type(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL);
        if (channelKeys != null) {
            List<Node> items = new ArrayList<Node>();
            for (String channelKey : channelKeys) {
                items.add(new Node().value(channelKey));
            }
            composite.properties("channels", new Node().items(items));
        }
        contracts.put(key, composite);
        return this;
    }

    public ContractsBuilder channelSourceBinding(String key, Consumer<ChannelSourceBindingBuilder> customizer) {
        ChannelSourceBindingBuilder builder = new ChannelSourceBindingBuilder();
        customizer.accept(builder);
        contracts.put(key, builder.build());
        return this;
    }

    public ContractsBuilder channel(String key, Class<?> channelTypeClass) {
        contracts.put(key, new Node().type(TypeRef.of(channelTypeClass).asTypeNode()));
        return this;
    }

    public ContractsBuilder channel(String key,
                                    Class<?> channelTypeClass,
                                    Consumer<NodeObjectBuilder> customizer) {
        Node channel = new Node().type(TypeRef.of(channelTypeClass).asTypeNode());
        if (customizer != null) {
            NodeObjectBuilder channelBuilder = NodeObjectBuilder.create();
            customizer.accept(channelBuilder);
            Node configured = channelBuilder.build();
            if (configured.getProperties() != null) {
                for (Map.Entry<String, Node> entry : configured.getProperties().entrySet()) {
                    channel.properties(entry.getKey(), entry.getValue());
                }
            }
        }
        contracts.put(key, channel);
        return this;
    }

    public ContractsBuilder triggeredEventChannel(String key) {
        contracts.put(key, new Node().type(TypeAliases.CORE_TRIGGERED_EVENT_CHANNEL));
        return this;
    }

    public ContractsBuilder lifecycleEventChannel(String key, String eventTypeAlias) {
        Node channel = new Node().type(TypeAliases.CORE_LIFECYCLE_EVENT_CHANNEL);
        if (eventTypeAlias != null) {
            channel.properties("event", new Node().type(eventTypeAlias));
        }
        contracts.put(key, channel);
        return this;
    }

    public ContractsBuilder lifecycleEventChannel(String key, Class<?> eventTypeClass) {
        Node channel = new Node().type(TypeAliases.CORE_LIFECYCLE_EVENT_CHANNEL);
        if (eventTypeClass != null) {
            channel.properties("event", new Node().type(TypeRef.of(eventTypeClass).asTypeNode()));
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
        Node operation = new Node().type(TypeAliases.CONVERSATION_OPERATION);
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

    public ContractsBuilder changeOperation(String key,
                                            String channel,
                                            String description,
                                            Consumer<NodeObjectBuilder> requestCustomizer) {
        Node operation = new Node().type(TypeAliases.CONVERSATION_CHANGE_OPERATION);
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

    public ContractsBuilder changeOperation(String key, String channel, String description) {
        return changeOperation(key, channel, description, request -> {
        });
    }

    public ContractsBuilder operation(String key, String channel, String description) {
        return operation(key, channel, description, request -> {
        });
    }

    public ContractsBuilder operation(String key,
                                      String channel,
                                      Class<?> requestTypeClass,
                                      String description) {
        return operation(key, channel, description, request -> request.type(requestTypeClass));
    }

    public ContractsBuilder withMyOsAdminDefaults() {
        timelineChannel("myOsAdminChannel");
        operation("myOsAdminUpdate", "myOsAdminChannel", null);
        implementOperation("myOsAdminUpdateImpl", "myOsAdminUpdate", steps -> steps
                .js("EmitAdminEvents", BlueDocDsl.js(js -> js.returnOutput(
                        JsOutputBuilder.output().eventsRaw("event.message.request")))));
        return this;
    }

    public ContractsBuilder sequentialWorkflowOperation(String key,
                                                        String operationName,
                                                        Consumer<StepsBuilder> customizer) {
        StepsBuilder stepsBuilder = new StepsBuilder();
        customizer.accept(stepsBuilder);

        Node workflow = new Node().type(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION);
        workflow.properties("operation", new Node().value(operationName));
        workflow.properties("steps", new Node().items(stepsBuilder.build()));
        contracts.put(key, workflow);
        return this;
    }

    public ContractsBuilder changeWorkflowOperation(String key,
                                                    String operationName,
                                                    Consumer<StepsBuilder> customizer) {
        StepsBuilder stepsBuilder = new StepsBuilder();
        customizer.accept(stepsBuilder);

        Node workflow = new Node().type(TypeAliases.CONVERSATION_CHANGE_WORKFLOW);
        workflow.properties("operation", new Node().value(operationName));
        workflow.properties("steps", new Node().items(stepsBuilder.build()));
        contracts.put(key, workflow);
        return this;
    }

    public ContractsBuilder implementOperation(String key,
                                               String operationName,
                                               Consumer<StepsBuilder> customizer) {
        return sequentialWorkflowOperation(key, operationName, customizer);
    }

    public ContractsBuilder sequentialWorkflow(String key,
                                               String channel,
                                               Node event,
                                               Consumer<StepsBuilder> customizer) {
        StepsBuilder stepsBuilder = new StepsBuilder();
        customizer.accept(stepsBuilder);

        Node workflow = new Node().type(TypeAliases.CONVERSATION_SEQUENTIAL_WORKFLOW);
        workflow.properties("channel", new Node().value(channel));
        if (event != null) {
            workflow.properties("event", event);
        }
        workflow.properties("steps", new Node().items(stepsBuilder.build()));
        contracts.put(key, workflow);
        return this;
    }

    public ContractsBuilder onTriggered(String key,
                                        Node event,
                                        Consumer<StepsBuilder> customizer) {
        triggeredEventChannel("triggeredEventChannel");
        return sequentialWorkflow(key, "triggeredEventChannel", event, customizer);
    }

    public ContractsBuilder onTriggered(String key,
                                        Class<?> eventTypeClass,
                                        Consumer<StepsBuilder> customizer) {
        triggeredEventChannel("triggeredEventChannel");
        return sequentialWorkflow(key,
                "triggeredEventChannel",
                new Node().type(TypeRef.of(eventTypeClass).asTypeNode()),
                customizer);
    }

    public ContractsBuilder onLifecycle(String key,
                                        String lifecycleChannelKey,
                                        Consumer<StepsBuilder> customizer) {
        return sequentialWorkflow(key, lifecycleChannelKey, null, customizer);
    }

    public ContractsBuilder onEvent(String key,
                                    String channel,
                                    Class<?> eventTypeClass,
                                    Consumer<StepsBuilder> customizer) {
        return sequentialWorkflow(key, channel, new Node().type(TypeRef.of(eventTypeClass).asTypeNode()), customizer);
    }

    public ContractsBuilder documentUpdateChannel(String key, String path) {
        contracts.put(key, new Node()
                .type(TypeAliases.CORE_DOCUMENT_UPDATE_CHANNEL)
                .properties("path", new Node().value(path)));
        return this;
    }
}
