package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ChannelSourceBindingBuilder {

    private final Map<String, Node> bindings = new LinkedHashMap<String, Node>();

    public ChannelSourceBindingBuilder bind(String abstractChannelKey, String sourceChannelKey) {
        bindings.put(abstractChannelKey, new Node().value(sourceChannelKey));
        return this;
    }

    public Node build() {
        return new Node()
                .type(TypeAliases.CONVERSATION_CHANNEL_SOURCE_BINDING)
                .properties("bindings", new Node().properties(bindings));
    }
}
