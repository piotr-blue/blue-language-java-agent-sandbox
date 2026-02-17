package blue.language.utils.limits;

import blue.language.model.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Supported features:
 * 1. Exact path matching (e.g., "/a/b/c")
 * 2. Single-level wildcards (e.g., "/a/{wildcard}/c")
 * 3. Maximum depth limitation
 */
public class PathLimits implements Limits {
    private final Set<String> allowedPaths;
    private final int maxDepth;
    private final Stack<String> currentPath;

    public PathLimits(Set<String> allowedPaths, int maxDepth) {
        this.allowedPaths = allowedPaths;
        this.maxDepth = maxDepth;
        this.currentPath = new Stack<>();
    }

    @Override
    public boolean shouldExtendPathSegment(String pathSegment, Node node) {
        if (currentPath.size() >= maxDepth) {
            return false;
        }

        String potentialPath = buildPotentialPath(pathSegment);
        return isAllowedPath(potentialPath);
    }

    @Override
    public boolean shouldMergePathSegment(String pathSegment, Node currentNode) {
        return shouldExtendPathSegment(pathSegment, currentNode);
    }

    private boolean isAllowedPath(String path) {
        for (String allowedPath : allowedPaths) {
            if (matchesAllowedPath(allowedPath, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllowedPath(String allowedPath, String path) {
        String[] allowedParts = splitPointerSegments(allowedPath);
        String[] pathParts = splitPointerSegments(path);

        if (pathParts.length > allowedParts.length) {
            return false;
        }

        for (int i = 0; i < pathParts.length; i++) {
            if (!allowedParts[i].equals("*") && !allowedParts[i].equals(pathParts[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void enterPathSegment(String pathSegment, Node noe) {
        String segment = pathSegment == null ? "" : pathSegment;
        if (segment.startsWith("/")) {
            validatePointerEscapes(segment);
            currentPath.push(segment.replaceAll("^/+", ""));
            return;
        }
        currentPath.push(escapeJsonPointerSegment(segment));
    }

    @Override
    public void exitPathSegment() {
        if (!currentPath.isEmpty()) {
            currentPath.pop();
        }
    }

    private String getCurrentFullPath() {
        return "/" + String.join("/", currentPath);
    }

    private String buildPotentialPath(String pathSegment) {
        String segment = pathSegment == null ? "" : pathSegment;
        if (segment.startsWith("/")) {
            return normalizePath(getCurrentFullPath() + segment);
        }
        String escapedSegment = escapeJsonPointerSegment(segment);
        return normalizePath(getCurrentFullPath() + "/" + escapedSegment);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path;
        while (normalized.startsWith("//")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private String escapeJsonPointerSegment(String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }

    private String[] splitPointerSegments(String pointerPath) {
        if (pointerPath == null || pointerPath.isEmpty() || "/".equals(pointerPath)) {
            return new String[0];
        }
        validatePointerEscapes(pointerPath);
        String normalized = pointerPath.startsWith("/") ? pointerPath.substring(1) : pointerPath;
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return normalized.split("/", -1);
    }

    private void validatePointerEscapes(String pointer) {
        int start = pointer.startsWith("/") ? 1 : 0;
        for (int i = start; i < pointer.length(); i++) {
            char c = pointer.charAt(i);
            if (c != '~') {
                continue;
            }
            if (i + 1 >= pointer.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in path: " + pointer);
            }
            char next = pointer.charAt(++i);
            if (next != '0' && next != '1') {
                throw new IllegalArgumentException("Invalid JSON pointer escape in path: " + pointer);
            }
        }
    }

    public static class Builder {
        private Set<String> allowedPaths = new HashSet<>();
        private int maxDepth = Integer.MAX_VALUE;

        public Builder addPath(String path) {
            allowedPaths.add(normalizeAndValidateAllowedPath(path));
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            if (maxDepth < 0) {
                throw new IllegalArgumentException("Max depth cannot be negative: " + maxDepth);
            }
            this.maxDepth = maxDepth;
            return this;
        }

        public PathLimits build() {
            return new PathLimits(allowedPaths, maxDepth);
        }

        private String normalizeAndValidateAllowedPath(String path) {
            if (path == null) {
                throw new IllegalArgumentException("Allowed path cannot be null");
            }
            String normalized = path.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Allowed path cannot be empty");
            }
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            while (normalized.startsWith("//")) {
                normalized = normalized.substring(1);
            }
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            validatePointerEscapes(normalized);
            return normalized;
        }

        private void validatePointerEscapes(String pointer) {
            for (int i = 1; i < pointer.length(); i++) {
                char c = pointer.charAt(i);
                if (c != '~') {
                    continue;
                }
                if (i + 1 >= pointer.length()) {
                    throw new IllegalArgumentException("Invalid JSON pointer escape in allowed path: " + pointer);
                }
                char next = pointer.charAt(++i);
                if (next != '0' && next != '1') {
                    throw new IllegalArgumentException("Invalid JSON pointer escape in allowed path: " + pointer);
                }
            }
        }
    }

    public static PathLimits withMaxDepth(int maxDepth) {
        Builder builder = new PathLimits.Builder().setMaxDepth(maxDepth);
        if (maxDepth <= 0) {
            return builder.addPath("/").build();
        }
        StringBuilder wildcardPath = new StringBuilder();
        for (int i = 0; i < maxDepth; i++) {
            wildcardPath.append("/*");
        }
        return builder.addPath(wildcardPath.toString()).build();
    }

    public static PathLimits withSinglePath(String path) {
        return new PathLimits.Builder().addPath(path).build();
    }

    public static PathLimits fromNode(Node node) {
        return NodeToPathLimitsConverter.convert(node);
    }
}