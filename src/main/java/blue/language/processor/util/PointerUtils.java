package blue.language.processor.util;

/**
 * Utility helpers for normalising and composing JSON Pointer / scope strings.
 */
public final class PointerUtils {

    private PointerUtils() {
    }

    public static String normalizeScope(String scopePath) {
        if (scopePath == null || scopePath.isEmpty()) {
            return "/";
        }
        if (scopePath.charAt(0) != '/') {
            throw new IllegalArgumentException("Invalid JSON pointer: " + scopePath);
        }
        validatePointerEscapes(scopePath);
        return scopePath;
    }

    public static String normalizePointer(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return "/";
        }
        if (pointer.charAt(0) != '/') {
            throw new IllegalArgumentException("Invalid JSON pointer: " + pointer);
        }
        validatePointerEscapes(pointer);
        return pointer;
    }

    public static String normalizeRequiredPointer(String pointer, String argumentName) {
        if (pointer == null || pointer.isEmpty()) {
            throw new IllegalArgumentException(argumentName + " must be a JSON pointer starting with '/': " + pointer);
        }
        return normalizePointer(pointer);
    }

    public static String[] splitPointerSegments(String pointer) {
        String normalized = normalizePointer(pointer);
        if ("/".equals(normalized)) {
            return new String[0];
        }
        String[] rawSegments = normalized.substring(1).split("/", -1);
        String[] decoded = new String[rawSegments.length];
        for (int i = 0; i < rawSegments.length; i++) {
            decoded[i] = unescapePointerSegment(rawSegments[i]);
        }
        return decoded;
    }

    public static String unescapePointerSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return segment;
        }
        StringBuilder decoded = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c != '~') {
                decoded.append(c);
                continue;
            }
            if (i + 1 >= segment.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in segment: " + segment);
            }
            char next = segment.charAt(++i);
            if (next == '0') {
                decoded.append('~');
            } else if (next == '1') {
                decoded.append('/');
            } else {
                throw new IllegalArgumentException("Invalid JSON pointer escape in segment: " + segment);
            }
        }
        return decoded.toString();
    }

    public static String escapePointerSegment(String segment) {
        if (segment == null) {
            throw new IllegalArgumentException("JSON pointer segment cannot be null");
        }
        return segment.replace("~", "~0").replace("/", "~1");
    }

    public static boolean isArrayIndexSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return "0".equals(segment) || segment.charAt(0) != '0';
    }

    public static int parseArrayIndex(String segment) {
        if (!isArrayIndexSegment(segment)) {
            return -1;
        }
        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public static int parseArrayIndexOrThrow(String segment, String path) {
        int index = parseArrayIndex(segment);
        if (index < 0) {
            throw new IllegalStateException("Expected numeric array index in path: " + path);
        }
        return index;
    }

    public static String stripSlashes(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String stripped = value.trim();
        while (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        while (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }

    public static String joinRelativePointers(String base, String tail) {
        String basePart = stripSlashes(base);
        String tailPart = stripSlashes(tail);
        if (basePart.isEmpty() && tailPart.isEmpty()) {
            return "/";
        }
        if (basePart.isEmpty()) {
            return "/" + tailPart;
        }
        if (tailPart.isEmpty()) {
            return "/" + basePart;
        }
        return "/" + basePart + "/" + tailPart;
    }

    public static String resolvePointer(String scopePath, String relativePointer) {
        String normalizedScope = normalizeScope(scopePath);
        String normalizedPointer = normalizePointer(relativePointer);
        if ("/".equals(normalizedScope)) {
            return normalizedPointer;
        }
        if ("/".equals(normalizedPointer)) {
            return normalizedScope;
        }
        if (normalizedPointer.length() == 1) { // "/"
            return normalizedScope;
        }
        return normalizedScope + normalizedPointer;
    }

    public static String relativizePointer(String scopePath, String absolutePath) {
        String normalizedScope = normalizeScope(scopePath);
        String normalizedAbsolute = normalizePointer(absolutePath);
        if ("/".equals(normalizedScope)) {
            return normalizedAbsolute;
        }
        if (normalizedAbsolute.equals(normalizedScope)) {
            return "/";
        }
        String prefix = normalizedScope + "/";
        if (!normalizedAbsolute.startsWith(prefix)) {
            return normalizedAbsolute;
        }
        String remainder = normalizedAbsolute.substring(normalizedScope.length());
        if (remainder.isEmpty()) {
            return "/";
        }
        if ("/".equals(remainder)) {
            // Relative "/" already denotes scope root in processor APIs.
            // Preserve absolute path so trailing-empty descendants are not
            // collapsed into ambiguous root markers.
            return normalizedAbsolute;
        }
        return remainder.startsWith("/") ? remainder : "/" + remainder;
    }

    public static void validatePointerEscapes(String pointer) {
        if (pointer == null || pointer.isEmpty()) {
            return;
        }
        int start = pointer.charAt(0) == '/' ? 1 : 0;
        for (int i = start; i < pointer.length(); i++) {
            char c = pointer.charAt(i);
            if (c != '~') {
                continue;
            }
            if (i + 1 >= pointer.length()) {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
            char next = pointer.charAt(++i);
            if (next != '0' && next != '1') {
                throw new IllegalArgumentException("Invalid JSON pointer escape in: " + pointer);
            }
        }
    }
}
