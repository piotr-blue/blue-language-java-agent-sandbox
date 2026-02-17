package blue.language.processor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointerUtilsTest {

    @Test
    void relativizePointerRequiresSegmentBoundaryForScopePrefix() {
        assertEquals("/foobar/value", PointerUtils.relativizePointer("/foo", "/foobar/value"));
    }

    @Test
    void relativizePointerReturnsDescendantSuffixWhenInsideScope() {
        assertEquals("/bar/baz", PointerUtils.relativizePointer("/foo", "/foo/bar/baz"));
        assertEquals("/", PointerUtils.relativizePointer("/foo", "/foo"));
    }

    @Test
    void relativizePointerKeepsAbsoluteWhenScopeIsSameLengthButDifferent() {
        assertEquals("/bar", PointerUtils.relativizePointer("/foo", "/bar"));
    }

    @Test
    void relativizePointerPreservesTrailingEmptyDescendantAbsolutePath() {
        assertEquals("/foo/", PointerUtils.relativizePointer("/foo", "/foo/"));
    }

    @Test
    void relativizePointerKeepsNestedEmptySegmentsRelative() {
        assertEquals("//", PointerUtils.relativizePointer("/scope", "/scope//"));
        assertEquals("/a//", PointerUtils.relativizePointer("/scope", "/scope/a//"));
    }

    @Test
    void normalizePointerRejectsMalformedEscapes() {
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.normalizePointer("/x~"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.normalizePointer("/x~2"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.normalizePointer("x~"));
    }

    @Test
    void normalizePointerRejectsNonPointerPaths() {
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.normalizePointer("x"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.normalizeScope("scope"));
    }

    @Test
    void resolvePointerRejectsMalformedEscapes() {
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.resolvePointer("/scope", "/x~"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.resolvePointer("/scope", "/x~2"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.resolvePointer("scope", "/x"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.resolvePointer("/scope", "x"));
    }

    @Test
    void resolvePointerTreatsNullAndEmptyAsRootWithinScope() {
        assertEquals("/scope", PointerUtils.resolvePointer("/scope", null));
        assertEquals("/scope", PointerUtils.resolvePointer("/scope", ""));
        assertEquals("/x", PointerUtils.resolvePointer(null, "/x"));
    }

    @Test
    void relativizePointerRejectsMalformedEscapes() {
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.relativizePointer("/scope", "/x~"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.relativizePointer("/scope", "/x~2"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.relativizePointer("/scope~", "/scope/value"));
    }
}
