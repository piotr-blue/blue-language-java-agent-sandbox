package blue.language.processor.model.core;

import blue.language.model.TypeBlueId;

@TypeBlueId("Core.DocumentUpdateChannel")
public class CoreDocumentUpdateChannelType extends CoreChannelType {

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
