package blue.language.processor.model.core;

import blue.language.model.BlueType;
import blue.language.model.Node;

@BlueType("Core.JsonPatchEntry")
public class CoreJsonPatchEntryType {

    private String op;
    private String path;
    private Node val;

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Node getVal() {
        return val;
    }

    public void setVal(Node val) {
        this.val = val;
    }
}
