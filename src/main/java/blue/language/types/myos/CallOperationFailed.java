package blue.language.types.myos;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("MyOS/Call Operation Failed")
@TypeBlueId("MyOS-Call-Operation-Failed-Placeholder-BlueId")
public class CallOperationFailed {
    public String reason;
    public Node inResponseTo;

    public CallOperationFailed reason(String reason) {
        this.reason = reason;
        return this;
    }

    public CallOperationFailed inResponseTo(Node inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }
}
