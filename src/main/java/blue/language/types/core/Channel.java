package blue.language.types.core;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("Core/Channel")
@TypeBlueId("DcoJyCh7XXxy1nR5xjy7qfkUgQ1GiZnKKSxh8DJusBSr")
public class Channel {
    public Node event;

    public Channel event(Node event) {
        this.event = event;
        return this;
    }
}
