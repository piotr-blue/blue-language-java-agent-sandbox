package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.myos.MyOsTypes;

public final class MyOsTimeline {

    private MyOsTimeline() {
    }

    public static Binding email(String email) {
        return new Binding().email(email);
    }

    public static Binding accountId(String accountId) {
        return new Binding().accountId(accountId);
    }

    public static final class Binding {
        private final Node node = new Node().type(TypeRef.of(MyOsTypes.MyOsTimelineChannel.class).asTypeNode());

        public Binding accountId(String accountId) {
            node.properties("accountId", new Node().value(accountId));
            return this;
        }

        public Binding email(String email) {
            node.properties("email", new Node().value(email));
            return this;
        }

        public Node asNode() {
            return node;
        }
    }
}
