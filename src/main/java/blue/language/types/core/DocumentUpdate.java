package blue.language.types.core;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("Core/Document Update")
@TypeBlueId("7htwgHAXA9FjUGRytXFfwYMUZz4R3BDMfmeHeGvpscLP")
public class DocumentUpdate {
    public String op;
    public String path;
    public Node before;
    public Node after;

    public DocumentUpdate op(String op) {
        this.op = op;
        return this;
    }

    public DocumentUpdate path(String path) {
        this.path = path;
        return this;
    }

    public DocumentUpdate before(Node before) {
        this.before = before;
        return this;
    }

    public DocumentUpdate after(Node after) {
        this.after = after;
        return this;
    }
}
