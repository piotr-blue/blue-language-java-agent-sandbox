package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.ArrayList;
import java.util.List;

public final class ChangesetBuilder {

    private final List<Node> entries = new ArrayList<Node>();

    public ChangesetBuilder replaceValue(String path, Object value) {
        entries.add(patchEntry("replace", path, new Node().value(value)));
        return this;
    }

    public ChangesetBuilder replaceExpression(String path, String expression) {
        entries.add(patchEntry("replace", path, new Node().value(BlueDocDsl.expr(expression))));
        return this;
    }

    public ChangesetBuilder addValue(String path, Object value) {
        entries.add(patchEntry("add", path, new Node().value(value)));
        return this;
    }

    public ChangesetBuilder remove(String path) {
        Node entry = new Node()
                .properties("op", new Node().value("remove"))
                .properties("path", new Node().value(path));
        entries.add(entry);
        return this;
    }

    public List<Node> build() {
        return entries;
    }

    private Node patchEntry(String op, String path, Node value) {
        return new Node()
                .properties("op", new Node().value(op))
                .properties("path", new Node().value(path))
                .properties("val", value);
    }
}
