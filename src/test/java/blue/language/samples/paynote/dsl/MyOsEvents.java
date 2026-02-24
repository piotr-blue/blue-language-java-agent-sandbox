package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.ArrayList;

public final class MyOsEvents {

    private MyOsEvents() {
    }

    public static SinglePermissionGrantRequestedBuilder singlePermissionGrantRequested() {
        return new SinglePermissionGrantRequestedBuilder();
    }

    public static LinkedPermissionsGrantRequestedBuilder linkedPermissionsGrantRequested() {
        return new LinkedPermissionsGrantRequestedBuilder();
    }

    public static Node singlePermissionGrantedFilter() {
        return new Node().type(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANTED);
    }

    public static SubscriptionInitiatedFilterBuilder subscriptionInitiated(String subscriptionId) {
        return new SubscriptionInitiatedFilterBuilder(subscriptionId);
    }

    public static SubscriptionUpdateFilterBuilder subscriptionUpdate(String subscriptionId) {
        return new SubscriptionUpdateFilterBuilder(subscriptionId);
    }

    public static ResponseUpdateFilterBuilder providerResponseUpdate(String subscriptionId) {
        return new ResponseUpdateFilterBuilder(subscriptionId);
    }

    public static SubscribeToSessionRequestedBuilder subscribeToSessionRequested() {
        return new SubscribeToSessionRequestedBuilder();
    }

    public static CallOperationRequestedBuilder callOperationRequested() {
        return new CallOperationRequestedBuilder();
    }

    public static final class SinglePermissionGrantRequestedBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED);

        public SinglePermissionGrantRequestedBuilder onBehalfOf(String channel) {
            event.properties("onBehalfOf", new Node().value(channel));
            return this;
        }

        public SinglePermissionGrantRequestedBuilder requestId(String requestId) {
            event.properties("requestId", new Node().value(requestId));
            return this;
        }

        public SinglePermissionGrantRequestedBuilder targetSessionIdExpression(String expression) {
            event.properties("targetSessionId", new Node().value(BlueDocDsl.expr(expression)));
            return this;
        }

        public SinglePermissionGrantRequestedBuilder readPermission(boolean read) {
            Node permissions = event.getProperties() != null ? event.getProperties().get("permissions") : null;
            if (permissions == null) {
                permissions = new Node();
                event.properties("permissions", permissions);
            }
            permissions.properties("read", new Node().value(read));
            return this;
        }

        public SinglePermissionGrantRequestedBuilder singleOperation(String operation) {
            Node permissions = event.getProperties() != null ? event.getProperties().get("permissions") : null;
            if (permissions == null) {
                permissions = new Node();
                event.properties("permissions", permissions);
            }
            Node singleOps = permissions.getProperties() != null ? permissions.getProperties().get("singleOps") : null;
            if (singleOps == null) {
                singleOps = new Node().items(new ArrayList<Node>());
                permissions.properties("singleOps", singleOps);
            }
            singleOps.getItems().add(new Node().value(operation));
            return this;
        }

        public Node build() {
            return event;
        }
    }

    public static final class LinkedPermissionsGrantRequestedBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_LINKED_DOCUMENTS_PERMISSION_GRANT_REQUESTED);

        public LinkedPermissionsGrantRequestedBuilder onBehalfOf(String channel) {
            event.properties("onBehalfOf", new Node().value(channel));
            return this;
        }

        public LinkedPermissionsGrantRequestedBuilder requestId(String requestId) {
            event.properties("requestId", new Node().value(requestId));
            return this;
        }

        public LinkedPermissionsGrantRequestedBuilder targetSessionIdExpression(String expression) {
            event.properties("targetSessionId", new Node().value(BlueDocDsl.expr(expression)));
            return this;
        }

        public LinkedPermissionsGrantRequestedBuilder linkPermissions(String linkKey, boolean read, boolean allOps) {
            Node links = event.getProperties() != null ? event.getProperties().get("links") : null;
            if (links == null) {
                links = new Node();
                event.properties("links", links);
            }
            links.properties(linkKey, new Node()
                    .properties("read", new Node().value(read))
                    .properties("allOps", new Node().value(allOps)));
            return this;
        }

        public Node build() {
            return event;
        }
    }

    public static final class SubscriptionInitiatedFilterBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_SUBSCRIPTION_TO_SESSION_INITIATED);

        private SubscriptionInitiatedFilterBuilder(String subscriptionId) {
            event.properties("subscriptionId", new Node().value(subscriptionId));
        }

        public Node build() {
            return event;
        }
    }

    public static final class SubscriptionUpdateFilterBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_SUBSCRIPTION_UPDATE);

        private SubscriptionUpdateFilterBuilder(String subscriptionId) {
            event.properties("subscriptionId", new Node().value(subscriptionId));
        }

        public SubscriptionUpdateFilterBuilder updateType(String typeAlias) {
            event.properties("update", new Node().type(typeAlias));
            return this;
        }

        public Node build() {
            return event;
        }
    }

    public static final class ResponseUpdateFilterBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_SUBSCRIPTION_UPDATE);

        private ResponseUpdateFilterBuilder(String subscriptionId) {
            event.properties("subscriptionId", new Node().value(subscriptionId));
            event.properties("update", new Node().type(TypeAliases.CONVERSATION_RESPONSE));
        }

        public ResponseUpdateFilterBuilder requester(String requester) {
            Node update = event.getProperties().get("update");
            update.properties("inResponseTo", new Node()
                    .properties("incomingEvent", new Node()
                            .properties("requester", new Node().value(requester))));
            return this;
        }

        public Node build() {
            return event;
        }
    }

    public static final class SubscribeToSessionRequestedBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_SUBSCRIBE_TO_SESSION_REQUESTED);

        public SubscribeToSessionRequestedBuilder targetSessionIdRaw(String expressionOrValue) {
            event.properties("targetSessionId", new Node().value(expressionOrValue));
            return this;
        }

        public SubscribeToSessionRequestedBuilder subscription(String id, Node events) {
            event.properties("subscription", new Node()
                    .properties("id", new Node().value(id))
                    .properties("events", events));
            return this;
        }

        public Node build() {
            return event;
        }
    }

    public static final class CallOperationRequestedBuilder {
        private final Node event = new Node().type(TypeAliases.MYOS_CALL_OPERATION_REQUESTED);

        public CallOperationRequestedBuilder onBehalfOf(String channel) {
            event.properties("onBehalfOf", new Node().value(channel));
            return this;
        }

        public CallOperationRequestedBuilder targetSessionIdRaw(String valueOrExpression) {
            event.properties("targetSessionId", new Node().value(valueOrExpression));
            return this;
        }

        public CallOperationRequestedBuilder operation(String operation) {
            event.properties("operation", new Node().value(operation));
            return this;
        }

        public CallOperationRequestedBuilder request(Node request) {
            event.properties("request", request);
            return this;
        }

        public Node build() {
            return event;
        }
    }
}
