package blue.language.samples.paynote.dsl;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.samples.paynote.types.common.CommonTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class StepsBuilder {

    private static final Blue BLUE = new Blue();
    private final List<Node> steps = new ArrayList<Node>();

    public StepsBuilder js(String name, JsProgram program) {
        Node step = new Node().type(TypeAliases.CONVERSATION_JAVASCRIPT_CODE);
        if (name != null) {
            step.name(name);
        }
        step.properties("code", new Node().value(program.code()));
        steps.add(step);
        return this;
    }

    public StepsBuilder jsRaw(String name, String code) {
        Node step = new Node().type(TypeAliases.CONVERSATION_JAVASCRIPT_CODE);
        if (name != null) {
            step.name(name);
        }
        step.properties("code", new Node().value(code));
        steps.add(step);
        return this;
    }

    public StepsBuilder jsTemplate(String name, String codeTemplate) {
        return jsRaw(name, JsTemplateResolver.resolveDefaults(codeTemplate));
    }

    public StepsBuilder jsTemplate(String name, String codeTemplate, Map<String, String> tokens) {
        return jsRaw(name, JsTemplateResolver.resolve(codeTemplate, tokens));
    }

    public StepsBuilder updateDocument(String name, Consumer<ChangesetBuilder> customizer) {
        ChangesetBuilder changesetBuilder = new ChangesetBuilder();
        customizer.accept(changesetBuilder);

        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().items(changesetBuilder.build()));
        steps.add(step);
        return this;
    }

    public StepsBuilder updateDocumentFromExpression(String name, String expression) {
        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().value(BlueDocDsl.expr(expression)));
        steps.add(step);
        return this;
    }

    public StepsBuilder triggerEvent(String name, Node event) {
        Node step = new Node().type(TypeAliases.CONVERSATION_TRIGGER_EVENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("event", event);
        steps.add(step);
        return this;
    }

    public StepsBuilder emit(String name, Object typedEvent) {
        if (typedEvent == null) {
            throw new IllegalArgumentException("typedEvent cannot be null");
        }
        return triggerEvent(name, BLUE.objectToNode(typedEvent));
    }

    public StepsBuilder emit(Object typedEvent) {
        return emit(null, typedEvent);
    }

    public StepsBuilder emitType(String name, Class<?> eventTypeClass, Consumer<NodeObjectBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(eventTypeClass).asTypeNode());
        if (payloadCustomizer != null) {
            NodeObjectBuilder builder = NodeObjectBuilder.create();
            payloadCustomizer.accept(builder);
            Node payload = builder.build();
            if (payload.getProperties() != null) {
                for (String key : payload.getProperties().keySet()) {
                    event.properties(key, payload.getProperties().get(key));
                }
            }
        }
        return triggerEvent(name, event);
    }

    public StepsBuilder emitType(Class<?> eventTypeClass, Consumer<NodeObjectBuilder> payloadCustomizer) {
        return emitType(null, eventTypeClass, payloadCustomizer);
    }

    public StepsBuilder emitType(Class<?> eventTypeClass) {
        return emitType(null, eventTypeClass, null);
    }

    public StepsBuilder emitAdHocEvent(String name, String eventName, Consumer<NodeObjectBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(CommonTypes.NamedEvent.class).asTypeNode());
        event.properties("name", new Node().value(eventName));
        if (payloadCustomizer != null) {
            NodeObjectBuilder payloadBuilder = NodeObjectBuilder.create();
            payloadCustomizer.accept(payloadBuilder);
            event.properties("payload", payloadBuilder.build());
        }
        return triggerEvent(name, event);
    }

    public StepsBuilder namedEvent(String name, String eventName, Consumer<NodeObjectBuilder> payloadCustomizer) {
        return emitAdHocEvent(name, eventName, payloadCustomizer);
    }

    public StepsBuilder namedEvent(String name, String eventName) {
        return emitAdHocEvent(name, eventName, null);
    }

    public StepsBuilder triggerPayment(String name,
                                       Class<?> paymentEventTypeClass,
                                       Consumer<NodeObjectBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(paymentEventTypeClass).asTypeNode());
        if (payloadCustomizer != null) {
            NodeObjectBuilder builder = NodeObjectBuilder.create();
            payloadCustomizer.accept(builder);
            Node payload = builder.build();
            if (payload.getProperties() != null) {
                for (Map.Entry<String, Node> entry : payload.getProperties().entrySet()) {
                    event.properties(entry.getKey(), entry.getValue());
                }
            }
        }
        Node processorNode = event.getProperties() != null ? event.getProperties().get("processor") : null;
        String processor = processorNode != null && processorNode.getValue() != null
                ? String.valueOf(processorNode.getValue())
                : null;
        if (processor == null || processor.trim().isEmpty()) {
            throw new IllegalArgumentException("triggerPayment requires non-empty processor field");
        }
        return triggerEvent(name, event);
    }

    public StepsBuilder triggerPayment(Class<?> paymentEventTypeClass,
                                       Consumer<NodeObjectBuilder> payloadCustomizer) {
        return triggerPayment(null, paymentEventTypeClass, payloadCustomizer);
    }

    public StepsBuilder replaceValue(String name, String path, Object value) {
        return updateDocument(name, changeset -> changeset.replaceValue(path, value));
    }

    public StepsBuilder replaceExpression(String name, String path, String expression) {
        return updateDocument(name, changeset -> changeset.replaceExpression(path, expression));
    }

    public StepsBuilder raw(Node step) {
        steps.add(step);
        return this;
    }

    public CaptureStepBuilder capture() {
        return new CaptureStepBuilder(this);
    }

    List<Node> build() {
        return steps;
    }

    public static final class CaptureStepBuilder {
        private final StepsBuilder parent;

        private CaptureStepBuilder(StepsBuilder parent) {
            this.parent = parent;
        }

        public StepsBuilder lock() {
            return parent.triggerEvent("RequestCaptureLock", PayNoteEvents.captureLockRequested());
        }

        public StepsBuilder unlock() {
            return parent.triggerEvent("RequestCaptureUnlock", PayNoteEvents.captureUnlockRequested());
        }

        public StepsBuilder markLocked() {
            return parent.triggerEvent("CaptureLocked", PayNoteEvents.captureLocked());
        }

        public StepsBuilder markUnlocked() {
            return parent.triggerEvent("CaptureUnlocked", PayNoteEvents.captureUnlocked());
        }

        public StepsBuilder requestNow() {
            return parent.triggerEvent("RequestCapture", PayNoteEvents.captureFundsRequested(
                    new Node().value(BlueDocDsl.expr("document('/amount/total')"))));
        }

        public StepsBuilder requestPartial(String amountExpression) {
            return parent.triggerEvent("RequestCapture", PayNoteEvents.captureFundsRequested(
                    new Node().value(BlueDocDsl.expr(amountExpression))));
        }

        public StepsBuilder refundFull() {
            return parent.triggerEvent("RequestRefund", PayNoteEvents.reservationReleaseRequested(
                    new Node().value(BlueDocDsl.expr("document('/amount/total')"))));
        }
    }
}
