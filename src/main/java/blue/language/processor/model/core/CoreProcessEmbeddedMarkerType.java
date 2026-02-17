package blue.language.processor.model.core;

import blue.language.model.BlueType;

import java.util.ArrayList;
import java.util.List;

@BlueType("Core.ProcessEmbedded")
public class CoreProcessEmbeddedMarkerType extends CoreMarkerType {

    private List<String> paths = new ArrayList<String>();

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths != null ? paths : new ArrayList<String>();
    }
}
