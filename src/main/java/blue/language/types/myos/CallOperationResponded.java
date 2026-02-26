package blue.language.types.myos;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("MyOS/Call Operation Responded")
@TypeBlueId("MyOS-Call-Operation-Responded-Placeholder-BlueId")
public class CallOperationResponded {
    public Node result;
    public Node inResponseTo;

    public CallOperationResponded result(Node result) {
        this.result = result;
        return this;
    }

    public CallOperationResponded inResponseTo(Node inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }
}
