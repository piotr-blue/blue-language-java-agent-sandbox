package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MyOsBootstrapBuilder {

    private final Node root = new Node().type(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP);
    private final Map<String, Node> channelBindings = new LinkedHashMap<String, Node>();

    MyOsBootstrapBuilder(Node document) {
        root.properties("document", document);
    }

    public MyOsBootstrapBuilder bind(String channelKey, Node channel) {
        channelBindings.put(channelKey, channel);
        return this;
    }

    public MyOsBootstrapBuilder bindTimeline(String channelKey, MyOsTimeline.Binding binding) {
        return bind(channelKey, binding.asNode());
    }

    public Node build() {
        if (!channelBindings.isEmpty()) {
            root.properties("channelBindings", new Node().properties(channelBindings));
        }
        return root;
    }
}
