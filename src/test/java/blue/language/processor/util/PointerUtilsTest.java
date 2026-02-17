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

    @Test
    void splitPointerSegmentsUnescapesAndPreservesEmptySegments() {
        assertEquals(0, PointerUtils.splitPointerSegments("/").length);
        assertEquals("", PointerUtils.splitPointerSegments("//")[0]);
        assertEquals("a/b", PointerUtils.splitPointerSegments("/a~1b//")[0]);
        assertEquals("", PointerUtils.splitPointerSegments("/a~1b//")[1]);
        assertEquals("", PointerUtils.splitPointerSegments("/a~1b//")[2]);
    }

    @Test
    void arrayIndexHelpersApplyStrictJsonPointerArrayRules() {
        assertEquals(0, PointerUtils.parseArrayIndex("0"));
        assertEquals(12, PointerUtils.parseArrayIndex("12"));
        assertEquals(-1, PointerUtils.parseArrayIndex("01"));
        assertEquals(-1, PointerUtils.parseArrayIndex("x"));
        assertEquals(-1, PointerUtils.parseArrayIndex("999999999999999999999"));
    }

    @Test
    void validatePointerEscapesSupportsRelativeSegmentsAndRejectsMalformedOnes() {
        PointerUtils.validatePointerEscapes("a~1b");
        PointerUtils.validatePointerEscapes("/a~1b");
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.validatePointerEscapes("a~"));
        assertThrows(IllegalArgumentException.class, () -> PointerUtils.validatePointerEscapes("/a~2"));
    }
}
