package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DocumentLinksBuilder {

    private final Map<String, Node> links = new LinkedHashMap<String, Node>();

    public DocumentLinksBuilder sessionLink(String key, String anchor, String sessionId) {
        Node sessionLink = new Node().type("MyOS/MyOS Session Link");
        if (anchor != null) {
            sessionLink.properties("anchor", new Node().value(anchor));
        }
        if (sessionId != null) {
            sessionLink.properties("sessionId", new Node().value(sessionId));
        }
        links.put(key, sessionLink);
        return this;
    }

    public Node build() {
        return new Node()
                .type("MyOS/Document Links")
                .properties(links);
    }
}
