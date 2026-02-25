package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.sdk.MyOsPermissions;
import blue.language.samples.paynote.types.myos.MyOsTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MyOsSteps {

    private final StepsBuilder parent;

    public MyOsSteps(StepsBuilder parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null");
        }
        this.parent = parent;
    }

    public StepsBuilder requestSingleDocPermission(String onBehalfOf,
                                                   String requestId,
                                                   String targetSessionId,
                                                   MyOsPermissions permissions) {
        return parent.emitType("RequestSingleDocumentPermission",
                MyOsTypes.SingleDocumentPermissionGrantRequested.class,
                payload -> {
                    payload.put("onBehalfOf", requireText(onBehalfOf, "onBehalfOf is required"));
                    payload.put("requestId", requireText(requestId, "requestId is required"));
                    payload.put("targetSessionId", requireText(targetSessionId, "targetSessionId is required"));
                    payload.putNode("permissions",
                            permissions == null ? MyOsPermissions.create().build() : permissions.build());
                });
    }

    public StepsBuilder requestLinkedDocsPermission(String onBehalfOf,
                                                    String requestId,
                                                    String targetSessionId,
                                                    Map<String, MyOsPermissions> links) {
        return parent.emitType("RequestLinkedDocumentsPermission",
                MyOsTypes.LinkedDocumentsPermissionGrantRequested.class,
                payload -> {
                    payload.put("onBehalfOf", requireText(onBehalfOf, "onBehalfOf is required"));
                    payload.put("requestId", requireText(requestId, "requestId is required"));
                    payload.put("targetSessionId", requireText(targetSessionId, "targetSessionId is required"));

                    Node linksNode = new Node().properties(new LinkedHashMap<String, Node>());
                    if (links != null) {
                        for (Map.Entry<String, MyOsPermissions> entry : links.entrySet()) {
                            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                                continue;
                            }
                            MyOsPermissions permissions = entry.getValue() == null
                                    ? MyOsPermissions.create()
                                    : entry.getValue();
                            linksNode.properties(entry.getKey().trim(), permissions.build());
                        }
                    }
                    payload.putNode("links", linksNode);
                });
    }

    public StepsBuilder addParticipant(String channelKey, String email) {
        return parent.emitType("AddParticipant",
                MyOsTypes.AddingParticipantRequested.class,
                payload -> {
                    payload.put("channelKey", requireText(channelKey, "channelKey is required"));
                    payload.put("email", requireText(email, "email is required"));
                });
    }

    public StepsBuilder removeParticipant(String channelKey) {
        return parent.emitType("RemoveParticipant",
                MyOsTypes.RemovingParticipantRequested.class,
                payload -> payload.put("channelKey", requireText(channelKey, "channelKey is required")));
    }

    public StepsBuilder callOperation(String onBehalfOf,
                                      String targetSessionId,
                                      String operation,
                                      Node request) {
        return parent.emitType("CallOperation",
                MyOsTypes.CallOperationRequested.class,
                payload -> {
                    payload.put("onBehalfOf", requireText(onBehalfOf, "onBehalfOf is required"));
                    payload.put("targetSessionId", requireText(targetSessionId, "targetSessionId is required"));
                    payload.put("operation", requireText(operation, "operation is required"));
                    if (request != null) {
                        payload.putNode("request", request);
                    }
                });
    }

    public StepsBuilder subscribeToSession(String targetSessionId, String subscriptionId) {
        return parent.emitType("SubscribeToSession",
                MyOsTypes.SubscribeToSessionRequested.class,
                payload -> {
                    payload.put("targetSessionId", requireText(targetSessionId, "targetSessionId is required"));
                    payload.putNode("subscription", new Node()
                            .properties("id", new Node().value(requireText(subscriptionId, "subscriptionId is required")))
                            .properties("events", new Node().items(new ArrayList<Node>())));
                });
    }

    public StepsBuilder startWorkerSession(String agentChannelKey, Node config) {
        return parent.emitType("StartWorkerSession",
                MyOsTypes.StartWorkerSessionRequested.class,
                payload -> {
                    payload.put("agentChannelKey", requireText(agentChannelKey, "agentChannelKey is required"));
                    if (config != null) {
                        payload.putNode("config", config);
                    }
                });
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
