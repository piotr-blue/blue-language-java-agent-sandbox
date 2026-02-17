package blue.language.processor.model.core;

import blue.language.model.TypeBlueId;

@TypeBlueId("Core.EmbeddedNodeChannel")
public class CoreEmbeddedNodeChannelType extends CoreChannelType {

    private String childPath;

    public String getChildPath() {
        return childPath;
    }

    public void setChildPath(String childPath) {
        this.childPath = childPath;
    }
}
