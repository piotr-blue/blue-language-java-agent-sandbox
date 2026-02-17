package blue.language.processor.model;

import blue.language.model.TypeBlueId;
import blue.language.processor.util.PointerUtils;

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
        List<String> normalizedPaths = new ArrayList<>();
        if (newPaths != null) {
            for (String path : newPaths) {
                String normalized = normalizePath(path);
                if (normalized != null) {
                    normalizedPaths.add(normalized);
                }
            }
        }
        paths.clear();
        paths.addAll(normalizedPaths);
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
        try {
            return PointerUtils.normalizePointer(trimmed);
        } catch (IllegalArgumentException ex) {
            if (!trimmed.startsWith("/")) {
                throw new IllegalArgumentException("ProcessEmbedded path must start with '/': " + path);
            }
            throw ex;
        }
    }
}
