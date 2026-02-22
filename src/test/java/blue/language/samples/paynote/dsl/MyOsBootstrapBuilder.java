package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MyOsBootstrapBuilder {

    private final Node root = new Node().type(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP);
    private final Map<String, Node> channelBindings = new LinkedHashMap<String, Node>();
    private final Map<String, String> roleAliases = new HashMap<String, String>();

    MyOsBootstrapBuilder(Node document) {
        root.properties("document", document);
        roleAliases.put("payer", "payerChannel");
        roleAliases.put("payee", "payeeChannel");
        roleAliases.put("guarantor", "guarantorChannel");
        roleAliases.put("shipper", "shipmentCompanyChannel");
        roleAliases.put("shipmentCompany", "shipmentCompanyChannel");
    }

    public MyOsBootstrapBuilder bind(String channelKey, Node channel) {
        channelBindings.put(channelKey, channel);
        return this;
    }

    public ChannelBindingEditor bind(String channelKey) {
        return new ChannelBindingEditor(this, channelKey);
    }

    public ChannelBindingEditor bindRole(String role) {
        String channelKey = roleAliases.get(role);
        if (channelKey == null) {
            channelKey = role != null && role.endsWith("Channel") ? role : role + "Channel";
        }
        return bind(channelKey);
    }

    public MyOsBootstrapBuilder role(String role, String channelKey) {
        roleAliases.put(role, channelKey);
        return this;
    }

    public MyOsBootstrapBuilder bindTimeline(String channelKey, MyOsTimeline.Binding binding) {
        return bind(channelKey, binding.asNode());
    }

    public Map<String, String> roleAliases() {
        return Collections.unmodifiableMap(roleAliases);
    }

    public Node build() {
        if (!channelBindings.isEmpty()) {
            root.properties("channelBindings", new Node().properties(channelBindings));
        }
        return root;
    }

    public static final class ChannelBindingEditor {
        private final MyOsBootstrapBuilder parent;
        private final String channelKey;

        private ChannelBindingEditor(MyOsBootstrapBuilder parent, String channelKey) {
            this.parent = parent;
            this.channelKey = channelKey;
        }

        public MyOsBootstrapBuilder accountId(String accountId) {
            Node binding = parent.channelBindings.get(channelKey);
            if (binding == null) {
                binding = new Node().type(TypeAliases.MYOS_TIMELINE_CHANNEL);
                parent.channelBindings.put(channelKey, binding);
            }
            binding.properties("accountId", new Node().value(accountId));
            return parent;
        }

        public MyOsBootstrapBuilder email(String email) {
            Node binding = parent.channelBindings.get(channelKey);
            if (binding == null) {
                binding = new Node().type(TypeAliases.MYOS_TIMELINE_CHANNEL);
                parent.channelBindings.put(channelKey, binding);
            }
            binding.properties("email", new Node().value(email));
            return parent;
        }
    }
}
