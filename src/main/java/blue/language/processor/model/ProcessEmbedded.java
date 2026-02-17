package blue.language.processor.model;

import blue.language.model.TypeBlueId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@TypeBlueId("ProcessEmbedded")
public class ProcessEmbedded extends MarkerContract {

    private final List<String> paths = new ArrayList<>();

    public List<String> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public void setPaths(List<String> newPaths) {
        paths.clear();
        if (newPaths != null) {
            for (String path : newPaths) {
                String normalized = normalizePath(path);
                if (normalized != null) {
                    paths.add(normalized);
                }
            }
        }
    }

    public ProcessEmbedded addPath(String path) {
        String normalized = normalizePath(path);
        if (normalized != null) {
            paths.add(normalized);
        }
        return this;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
