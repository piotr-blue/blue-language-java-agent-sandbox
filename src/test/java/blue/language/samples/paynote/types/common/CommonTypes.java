package blue.language.samples.paynote.types.common;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class CommonTypes {

    private CommonTypes() {
    }

    @TypeAlias("Common/Named Event")
    @TypeBlueId("Common-Named-Event-Demo-BlueId")
    public static class NamedEvent {
        public String name;
        public Node payload;
    }
}
