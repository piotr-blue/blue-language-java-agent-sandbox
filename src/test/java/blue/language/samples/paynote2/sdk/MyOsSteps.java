package blue.language.samples.paynote2.sdk;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.samples.paynote.dsl.NodeObjectBuilder;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.types.myos.MyOsTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MyOsSteps {

    private static final Blue BLUE = new Blue();

    private final StepsBuilder parent;

    public MyOsSteps(StepsBuilder parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null");
        }
        this.parent = parent;
    }

    public StepsBuilder requestSingleDocPermission(String onBehalfOf,
                                                   String requestId,
                                                   Object targetSessionId,
                                                   Object permissions) {
        MyOsTypes.SingleDocumentPermissionGrantRequested event =
                new MyOsTypes.SingleDocumentPermissionGrantRequested();
        event.onBehalfOf = requireText(onBehalfOf, "onBehalfOf is required");
        event.requestId = requireText(requestId, "requestId is required");
        event.targetSessionId = asText(targetSessionId, "targetSessionId is required");
        event.permissions = toNode(permissions, true);
        return emitBean("RequestSingleDocumentPermission",
                MyOsTypes.SingleDocumentPermissionGrantRequested.class,
                event);
    }

    public StepsBuilder requestLinkedDocsPermission(String onBehalfOf,
                                                    String requestId,
                                                    Object targetSessionId,
                                                    Map<String, ?> links) {
        MyOsTypes.LinkedDocumentsPermissionGrantRequested event =
                new MyOsTypes.LinkedDocumentsPermissionGrantRequested();
        event.onBehalfOf = requireText(onBehalfOf, "onBehalfOf is required");
        event.requestId = requireText(requestId, "requestId is required");
        event.targetSessionId = asText(targetSessionId, "targetSessionId is required");
        event.links = toLinksNode(links);
        return emitBean("RequestLinkedDocumentsPermission",
                MyOsTypes.LinkedDocumentsPermissionGrantRequested.class,
                event);
    }

    public StepsBuilder addParticipant(String channelKey, String email) {
        MyOsTypes.AddingParticipantRequested event = new MyOsTypes.AddingParticipantRequested();
        event.channelKey = requireText(channelKey, "channelKey is required");
        event.email = requireText(email, "email is required");
        return emitBean("AddParticipant", MyOsTypes.AddingParticipantRequested.class, event);
    }

    public StepsBuilder removeParticipant(String channelKey) {
        MyOsTypes.RemovingParticipantRequested event = new MyOsTypes.RemovingParticipantRequested();
        event.channelKey = requireText(channelKey, "channelKey is required");
        return emitBean("RemoveParticipant", MyOsTypes.RemovingParticipantRequested.class, event);
    }

    public StepsBuilder callOperation(String onBehalfOf,
                                      Object targetSessionId,
                                      String operation,
                                      Object request) {
        MyOsTypes.CallOperationRequested event = new MyOsTypes.CallOperationRequested();
        event.onBehalfOf = requireText(onBehalfOf, "onBehalfOf is required");
        event.targetSessionId = asText(targetSessionId, "targetSessionId is required");
        event.operation = requireText(operation, "operation is required");
        if (request != null) {
            event.request = toNode(request, false);
        }
        return emitBean("CallOperation", MyOsTypes.CallOperationRequested.class, event);
    }

    public StepsBuilder subscribeToSession(Object targetSessionId, String subscriptionId) {
        MyOsTypes.SubscribeToSessionRequested event = new MyOsTypes.SubscribeToSessionRequested();
        event.targetSessionId = asText(targetSessionId, "targetSessionId is required");

        SubscriptionSpec subscription = new SubscriptionSpec();
        subscription.id = requireText(subscriptionId, "subscriptionId is required");
        subscription.events = new ArrayList<Object>();
        event.subscription = BLUE.objectToNode(subscription);
        return emitBean("SubscribeToSession", MyOsTypes.SubscribeToSessionRequested.class, event);
    }

    public StepsBuilder startWorkerSession(String agentChannelKey, Node config) {
        MyOsTypes.StartWorkerSessionRequested event = new MyOsTypes.StartWorkerSessionRequested();
        event.agentChannelKey = requireText(agentChannelKey, "agentChannelKey is required");
        event.config = config;
        return emitBean("StartWorkerSession", MyOsTypes.StartWorkerSessionRequested.class, event);
    }

    private static Node toLinksNode(Map<String, ?> links) {
        Map<String, Object> normalized = new LinkedHashMap<String, Object>();
        if (links != null) {
            for (Map.Entry<String, ?> entry : links.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                normalized.put(key.trim(), normalizePermissionValue(entry.getValue()));
            }
        }
        return BLUE.objectToNode(normalized);
    }

    private static Object normalizePermissionValue(Object value) {
        if (value == null) {
            return MyOsPermissions.create();
        }
        if (value instanceof MyOsPermissions) {
            return value;
        }
        if (value instanceof blue.language.samples.paynote.sdk.MyOsPermissions) {
            return ((blue.language.samples.paynote.sdk.MyOsPermissions) value).build();
        }
        if (value instanceof Node) {
            return value;
        }
        return value;
    }

    private static Node toNode(Object value, boolean defaultToEmptyPermissions) {
        if (value == null) {
            if (defaultToEmptyPermissions) {
                return MyOsPermissions.create().build();
            }
            return null;
        }
        if (value instanceof Node) {
            return (Node) value;
        }
        if (value instanceof MyOsPermissions) {
            return ((MyOsPermissions) value).build();
        }
        if (value instanceof blue.language.samples.paynote.sdk.MyOsPermissions) {
            return ((blue.language.samples.paynote.sdk.MyOsPermissions) value).build();
        }
        return BLUE.objectToNode(value);
    }

    private static String asText(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value instanceof Node) {
            Object nodeValue = ((Node) value).getValue();
            if (nodeValue == null) {
                throw new IllegalArgumentException(message);
            }
            return String.valueOf(nodeValue);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return text;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private StepsBuilder emitBean(String stepName, Class<?> eventTypeClass, Object eventBean) {
        Node payload = BLUE.objectToNode(eventBean);
        return parent.emitType(stepName, eventTypeClass, target -> copyProperties(payload, target));
    }

    private static void copyProperties(Node source, NodeObjectBuilder target) {
        if (source == null || source.getProperties() == null) {
            return;
        }
        for (Map.Entry<String, Node> entry : source.getProperties().entrySet()) {
            target.putNode(entry.getKey(), entry.getValue());
        }
    }

    private static final class SubscriptionSpec {
        public String id;
        public java.util.List<Object> events;
    }
}
