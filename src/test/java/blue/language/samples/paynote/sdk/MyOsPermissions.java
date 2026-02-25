package blue.language.samples.paynote.sdk;

import blue.language.model.Node;

import java.util.ArrayList;
import java.util.List;

public final class MyOsPermissions {

    private final Node permissions = new Node();

    private MyOsPermissions() {
    }

    public static MyOsPermissions create() {
        return new MyOsPermissions();
    }

    public MyOsPermissions read(boolean value) {
        permissions.properties("read", new Node().value(value));
        return this;
    }

    public MyOsPermissions write(boolean value) {
        permissions.properties("write", new Node().value(value));
        return this;
    }

    public MyOsPermissions allOps(boolean value) {
        permissions.properties("allOps", new Node().value(value));
        return this;
    }

    public MyOsPermissions singleOps(String... operationNames) {
        List<Node> items = new ArrayList<Node>();
        if (operationNames != null) {
            for (String operationName : operationNames) {
                if (operationName == null || operationName.trim().isEmpty()) {
                    continue;
                }
                items.add(new Node().value(operationName.trim()));
            }
        }
        permissions.properties("singleOps", new Node().items(items));
        return this;
    }

    public Node build() {
        return permissions.clone();
    }
}
