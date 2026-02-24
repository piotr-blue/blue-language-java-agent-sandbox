package blue.language.processor.harness;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves raw document or bootstrap envelope input into runtime document + participants.
 */
final class BootstrapResolver {

    private static final String CONVERSATION_TIMELINE_CHANNEL = "Conversation/Timeline Channel";
    private static final String MYOS_TIMELINE_CHANNEL = "MyOS/MyOS Timeline Channel";

    private BootstrapResolver() {
    }

    static ResolvedInput resolve(Node input) {
        Node source = Objects.requireNonNull(input, "input").clone();
        Node document = source;
        Node channelBindings = null;
        if (source.getProperties() != null && source.getProperties().get("document") != null) {
            document = source.getProperties().get("document").clone();
            channelBindings = source.getProperties().get("channelBindings");
        }

        Map<String, Participant> participants = resolveParticipants(document, channelBindings);
        return new ResolvedInput(document, participants);
    }

    private static Map<String, Participant> resolveParticipants(Node document, Node channelBindings) {
        Map<String, Participant> participants = new LinkedHashMap<String, Participant>();
        Map<String, Node> bindingsByKey = channelBindings != null ? channelBindings.getProperties() : null;

        if (document.getProperties() == null || document.getProperties().get("contracts") == null) {
            return participants;
        }
        Node contracts = document.getProperties().get("contracts");
        if (contracts.getProperties() == null) {
            return participants;
        }

        for (Map.Entry<String, Node> entry : contracts.getProperties().entrySet()) {
            String contractKey = entry.getKey();
            Node contractNode = entry.getValue();
            if (!isTimelineChannel(contractNode)) {
                continue;
            }
            String timelineId = readTimelineId(contractNode);
            Node binding = bindingsByKey != null ? bindingsByKey.get(contractKey) : null;
            if (timelineId == null) {
                timelineId = timelineIdFromBinding(binding);
            }
            if (timelineId == null) {
                timelineId = contractKey + "-timeline";
            }
            contractNode.properties("timelineId", new Node().value(timelineId));
            registerParticipant(participants, contractKey, timelineId);
        }

        if (bindingsByKey != null) {
            for (Map.Entry<String, Node> entry : bindingsByKey.entrySet()) {
                if (participants.containsKey(entry.getKey())) {
                    continue;
                }
                String timelineId = timelineIdFromBinding(entry.getValue());
                if (timelineId == null) {
                    timelineId = entry.getKey() + "-timeline";
                }
                registerParticipant(participants, entry.getKey(), timelineId);
            }
        }
        return participants;
    }

    private static void registerParticipant(Map<String, Participant> participants, String key, String timelineId) {
        Participant participant = new Participant(key, timelineId);
        participants.put(participant.key(), participant);
        if (key != null && key.endsWith("Channel") && key.length() > "Channel".length()) {
            String alias = key.substring(0, key.length() - "Channel".length());
            if (!participants.containsKey(alias)) {
                participants.put(alias, new Participant(alias, timelineId));
            }
        }
    }

    private static boolean isTimelineChannel(Node contractNode) {
        String typeBlueId = typeBlueId(contractNode);
        if (typeBlueId == null) {
            return false;
        }
        return CONVERSATION_TIMELINE_CHANNEL.equals(typeBlueId) || MYOS_TIMELINE_CHANNEL.equals(typeBlueId);
    }

    private static String typeBlueId(Node node) {
        if (node == null || node.getType() == null) {
            return null;
        }
        if (node.getType().getBlueId() != null && !node.getType().getBlueId().trim().isEmpty()) {
            return node.getType().getBlueId().trim();
        }
        if (node.getType().getValue() instanceof String) {
            String value = ((String) node.getType().getValue()).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        if (node.getType().getProperties() != null && node.getType().getProperties().get("blueId") != null) {
            Node blueIdNode = node.getType().getProperties().get("blueId");
            if (blueIdNode != null && blueIdNode.getValue() != null) {
                String value = String.valueOf(blueIdNode.getValue()).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private static String readTimelineId(Node channel) {
        if (channel == null || channel.getProperties() == null) {
            return null;
        }
        Node timelineId = channel.getProperties().get("timelineId");
        if (timelineId == null || timelineId.getValue() == null) {
            return null;
        }
        String value = String.valueOf(timelineId.getValue()).trim();
        return value.isEmpty() ? null : value;
    }

    private static String timelineIdFromBinding(Node binding) {
        if (binding == null || binding.getProperties() == null) {
            return null;
        }
        String explicit = valueText(binding.getProperties().get("timelineId"));
        if (explicit != null) {
            return explicit;
        }
        String accountId = valueText(binding.getProperties().get("accountId"));
        if (accountId != null) {
            return accountId;
        }
        String email = valueText(binding.getProperties().get("email"));
        if (email != null) {
            return email;
        }
        return null;
    }

    private static String valueText(Node node) {
        if (node == null || node.getValue() == null) {
            return null;
        }
        String value = String.valueOf(node.getValue()).trim();
        return value.isEmpty() ? null : value;
    }

    static final class ResolvedInput {
        private final Node document;
        private final Map<String, Participant> participants;

        private ResolvedInput(Node document, Map<String, Participant> participants) {
            this.document = document.clone();
            this.participants = new LinkedHashMap<String, Participant>(participants);
        }

        Node document() {
            return document.clone();
        }

        Map<String, Participant> participants() {
            return new LinkedHashMap<String, Participant>(participants);
        }
    }
}
