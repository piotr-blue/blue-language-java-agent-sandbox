package blue.language.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.SnapshotTrustV2;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V2Spec_RehashPathMatchesIndexTest {

    private static final Node AUTHORING = new Node()
            .name("Root")
            .type(new Node().name("CoreType"))
            .items(
                    new Node().name("First"),
                    new Node().name("Second")
            )
            .properties("a/b", new Node().value("slash"))
            .properties("a~b", new Node().value("tilde"))
            .properties("a~/b", new Node().value("tilde-and-slash"))
            .properties("type", new Node().value("property-overrides-type-segment"));

    @Test
    void rehashPathMatchesSnapshotPointerIndexForAllPointers() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        for (Map.Entry<String, String> entry : snapshot.blueIdsByPointer().asMap().entrySet()) {
            assertEquals(
                    entry.getValue(),
                    BlueIdCalculatorV2.rehashPath(canonicalRoot, entry.getKey()),
                    "Pointer mismatch for " + entry.getKey()
            );
        }
    }

    @Test
    void rehashPathTreatsEmptyPointerAsRoot() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertEquals(
                snapshot.rootBlueId(),
                BlueIdCalculatorV2.rehashPath(canonicalRoot, "")
        );
    }

    @Test
    void rehashPathTreatsNullPointerAsRoot() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertEquals(
                snapshot.rootBlueId(),
                BlueIdCalculatorV2.rehashPath(canonicalRoot, null)
        );
    }

    @Test
    void rehashPathRejectsInvalidOrMissingPointers() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "type"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/does-not-exist"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/a~2b"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/a~"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/~2bad"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/type/"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/01"));
        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/999999999999999999999999"));
    }

    @Test
    void rehashPathUsesPropertyWhenSegmentCollidesWithBuiltInTypePointer() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String propertyTypeHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("type"));
        String builtinTypeHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getType());

        assertEquals(propertyTypeHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/type"));
        assertEquals(propertyTypeHash, snapshot.blueIdsByPointer().blueIdAt("/type"));
        assertNotEquals(builtinTypeHash, propertyTypeHash);
    }

    @Test
    void rehashPathResolvesEscapedPropertySegments() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String escapedPropertyHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("a/b"));
        assertEquals(escapedPropertyHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/a~1b"));
    }

    @Test
    void rehashPathResolvesEscapedTildeSegments() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String escapedPropertyHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("a~b"));
        assertEquals(escapedPropertyHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/a~0b"));
    }

    @Test
    void rehashPathResolvesMixedEscapedSegments() {
        Blue blue = new Blue();
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(AUTHORING);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String escapedPropertyHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("a~/b"));
        assertEquals(escapedPropertyHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/a~0~1b"));
    }

    @Test
    void rehashPathPrefersPropertyWhenNumericSegmentCollidesWithListIndex() {
        Blue blue = new Blue();
        Node withNumericProperty = new Node()
                .name("Root")
                .items(
                        new Node().name("Item0"),
                        new Node().name("Item1")
                )
                .properties("0", new Node().value("zero-property"));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(withNumericProperty);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String propertyHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("0"));
        String listItemHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getItems().get(0));

        assertEquals(propertyHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/0"));
        assertEquals(propertyHash, snapshot.blueIdsByPointer().blueIdAt("/0"));
        assertNotEquals(listItemHash, propertyHash);
    }

    @Test
    void rehashPathAllowsLeadingZeroNumericPropertyKeys() {
        Blue blue = new Blue();
        Node withLeadingZeroProperty = new Node()
                .name("Root")
                .items(
                        new Node().name("Item0"),
                        new Node().name("Item1")
                )
                .properties("01", new Node().value("leading-zero-property"));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(withLeadingZeroProperty);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String propertyHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getProperties().get("01"));
        assertEquals(propertyHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/01"));
        assertEquals(propertyHash, snapshot.blueIdsByPointer().blueIdAt("/01"));
    }

    @Test
    void rehashPathUsesBuiltInTypeSegmentWhenNoPropertyCollisionExists() {
        Blue blue = new Blue();
        Node withoutTypePropertyCollision = new Node()
                .name("Root")
                .type(new Node().name("CoreType"));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(withoutTypePropertyCollision);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String typeHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getType());
        assertEquals(typeHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/type"));
        assertEquals(typeHash, snapshot.blueIdsByPointer().blueIdAt("/type"));
    }

    @Test
    void rehashPathRejectsOutOfBoundsListIndex() {
        Blue blue = new Blue();
        Node withItems = new Node()
                .name("Root")
                .items(new Node().name("only-item"));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(withItems);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        assertThrows(IllegalArgumentException.class, () -> BlueIdCalculatorV2.rehashPath(canonicalRoot, "/1"));
    }

    @Test
    void rehashPathSupportsBuiltInChildSegmentsWithoutPropertyOverrides() {
        Blue blue = new Blue();
        Node resolved = new Node()
                .name("Root")
                .type(new Node().name("TypeNode"))
                .properties("listCarrier", new Node()
                        .type("List")
                        .itemType(new Node().name("ItemTypeNode")))
                .properties("dictCarrier", new Node()
                        .type("Dictionary")
                        .keyType(new Node().name("KeyTypeNode"))
                        .valueType(new Node().name("ValueTypeNode")));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(resolved, SnapshotTrustV2.BLIND_TRUST_RESOLVED);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        for (String pointer : Arrays.asList(
                "/type",
                "/listCarrier/type",
                "/listCarrier/itemType",
                "/dictCarrier/type",
                "/dictCarrier/keyType",
                "/dictCarrier/valueType"
        )) {
            String indexedBlueId = snapshot.blueIdsByPointer().blueIdAt(pointer);
            assertNotNull(indexedBlueId, "Expected pointer in index: " + pointer);
            assertEquals(indexedBlueId, BlueIdCalculatorV2.rehashPath(canonicalRoot, pointer));
        }
    }

    @Test
    void rehashPathSupportsBuiltInBlueSegmentWhenPresent() {
        Node canonicalRoot = new Node()
                .name("Root")
                .blue(new Node().name("BlueNode"));

        String blueHash = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalRoot.getBlue());
        assertEquals(blueHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/blue"));
    }

    @Test
    void rehashPathSupportsTrailingEmptySegmentWhenPropertyExists() {
        Blue blue = new Blue();
        Node withEmptyPropertySegment = new Node()
                .name("Root")
                .properties("scope", new Node()
                        .properties("", new Node().value("empty-key")));

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(withEmptyPropertySegment);
        Node canonicalRoot = snapshot.canonicalRoot().toNode();

        String nestedHash = BlueIdCalculatorV2.calculateSemanticBlueId(
                canonicalRoot.getProperties().get("scope").getProperties().get(""));
        assertEquals(nestedHash, BlueIdCalculatorV2.rehashPath(canonicalRoot, "/scope/"));
        assertEquals(nestedHash, snapshot.blueIdsByPointer().blueIdAt("/scope/"));
    }
}
