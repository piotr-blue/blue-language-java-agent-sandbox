package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocSpecializer {

    private final List<JsonPatch> patches = new ArrayList<JsonPatch>();
    private final Map<String, Node> bindings = new LinkedHashMap<String, Node>();
    private final Map<String, String> roleAliases = new LinkedHashMap<String, String>();

    DocSpecializer() {
        roleAliases.put("payer", "payerChannel");
        roleAliases.put("payee", "payeeChannel");
        roleAliases.put("guarantor", "guarantorChannel");
        roleAliases.put("shipper", "shipmentCompanyChannel");
        roleAliases.put("shipmentCompany", "shipmentCompanyChannel");
    }

    public DocSpecializer role(String role, String channelKey) {
        roleAliases.put(role, channelKey);
        return this;
    }

    public DocSpecializer setName(String name) {
        return set("/name", name);
    }

    public DocSpecializer setCurrency(String currency) {
        return set("/currency", currency);
    }

    public DocSpecializer setAmountTotal(int amountTotalMinor) {
        return set("/amount/total", amountTotalMinor);
    }

    public DocSpecializer set(String pointer, Object value) {
        String path = normalizeDocumentPath(pointer);
        patches.add(JsonPatch.replace(path, valueNode(value)));
        return this;
    }

    public ChannelBindingEditor bindRole(String role) {
        String channelKey = roleAliases.get(role);
        if (channelKey == null) {
            channelKey = role != null && role.endsWith("Channel") ? role : role + "Channel";
        }
        return bindChannel(channelKey);
    }

    public ChannelBindingEditor bindChannel(String channelKey) {
        return new ChannelBindingEditor(this, channelKey);
    }

    List<JsonPatch> patches() {
        return patches;
    }

    Node applyBindings(Node bootstrap) {
        Node next = bootstrap.clone();
        if (bindings.isEmpty()) {
            return next;
        }
        return DocTemplates.extend(next, mutator -> {
            for (Map.Entry<String, Node> entry : bindings.entrySet()) {
                Node binding = entry.getValue();
                if (binding.getProperties() == null) {
                    continue;
                }
                if (binding.getProperties().containsKey("accountId")) {
                    mutator.bindAccount(entry.getKey(), binding.getAsText("/accountId/value"));
                }
                if (binding.getProperties().containsKey("email")) {
                    mutator.bindEmail(entry.getKey(), binding.getAsText("/email/value"));
                }
            }
        });
    }

    private void setBindingValue(String channelKey, String key, String value) {
        Node channel = bindings.get(channelKey);
        if (channel == null) {
            channel = new Node().type(TypeAliases.MYOS_TIMELINE_CHANNEL);
            bindings.put(channelKey, channel);
        }
        channel.properties(key, new Node().value(value));
    }

    private String normalizeDocumentPath(String pointer) {
        if (pointer == null || pointer.trim().isEmpty()) {
            throw new IllegalArgumentException("Document pointer cannot be empty");
        }
        String trimmed = pointer.trim();
        if (trimmed.startsWith("/document/")) {
            return trimmed;
        }
        if (trimmed.equals("/document")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return "/document" + trimmed;
        }
        return "/document/" + trimmed;
    }

    private Node valueNode(Object value) {
        if (value instanceof Node) {
            return (Node) value;
        }
        return new Node().value(value);
    }

    public static final class ChannelBindingEditor {
        private final DocSpecializer parent;
        private final String channelKey;

        private ChannelBindingEditor(DocSpecializer parent, String channelKey) {
            this.parent = parent;
            this.channelKey = channelKey;
        }

        public DocSpecializer accountId(String accountId) {
            parent.setBindingValue(channelKey, "accountId", accountId);
            return parent;
        }

        public DocSpecializer email(String email) {
            parent.setBindingValue(channelKey, "email", email);
            return parent;
        }
    }
}
