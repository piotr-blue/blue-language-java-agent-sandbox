package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeGeneralizerV2PointerSemanticsTest {

    @Test
    void doesNotTreatLeadingZeroPointerSegmentAsArrayIndex() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: Price\n" +
                        "amount:\n" +
                        "  type: Integer\n" +
                        "currency:\n" +
                        "  type: Text\n"
        );
        String priceBlueId = provider.getBlueIdByName("Price");
        provider.addSingleDocs(
                "name: PriceInEUR\n" +
                        "type:\n" +
                        "  blueId: " + priceBlueId + "\n" +
                        "currency: EUR\n"
        );
        String priceInEURBlueId = provider.getBlueIdByName("PriceInEUR");

        Blue blue = new Blue(provider);
        Node doc = blue.yamlToNode(
                "list:\n" +
                        "  - type:\n" +
                        "      blueId: " + priceInEURBlueId + "\n" +
                        "    currency: EUR\n" +
                        "    amount: 1\n" +
                        "  - type:\n" +
                        "      blueId: " + priceInEURBlueId + "\n" +
                        "    currency: EUR\n" +
                        "    amount: 2\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        Node mutable = snapshot.resolvedRoot().toNode();
        mutable.getAsNode("/list/1").properties("currency", new Node().value("USD"));

        TypeGeneralizerV2 generalizer = new TypeGeneralizerV2();
        GeneralizationReport invalidPathReport = generalizer.generalizeToSoundness(blue, mutable, "/list/01/currency");
        assertFalse(invalidPathReport.hasGeneralizations());
        assertEquals(priceInEURBlueId, mutable.getAsText("/list/1/type/blueId"));

        GeneralizationReport validPathReport = generalizer.generalizeToSoundness(blue, mutable, "/list/1/currency");
        assertTrue(validPathReport.hasGeneralizations());
        assertEquals(priceBlueId, mutable.getAsText("/list/1/type/blueId"));
    }

    @Test
    void rejectsMalformedEscapesInParentPointerSegments() {
        Blue blue = new Blue();
        Node resolved = blue.yamlToNode(
                "list:\n" +
                        "  - type: Text\n" +
                        "    value: ok\n"
        );

        TypeGeneralizerV2 generalizer = new TypeGeneralizerV2();
        assertThrows(IllegalArgumentException.class,
                () -> generalizer.generalizeToSoundness(blue, resolved, "/list/~2bad/value"));
    }

    @Test
    void supportsLeadingZeroPropertySegmentsWhenParentIsObject() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: Price\n" +
                        "amount:\n" +
                        "  type: Integer\n" +
                        "currency:\n" +
                        "  type: Text\n"
        );
        String priceBlueId = provider.getBlueIdByName("Price");
        provider.addSingleDocs(
                "name: PriceInEUR\n" +
                        "type:\n" +
                        "  blueId: " + priceBlueId + "\n" +
                        "currency: EUR\n"
        );
        String priceInEURBlueId = provider.getBlueIdByName("PriceInEUR");

        Blue blue = new Blue(provider);
        Node doc = blue.yamlToNode(
                "prices:\n" +
                        "  \"01\":\n" +
                        "    type:\n" +
                        "      blueId: " + priceInEURBlueId + "\n" +
                        "    currency: EUR\n" +
                        "    amount: 1\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        Node mutable = snapshot.resolvedRoot().toNode();
        Node priceNode = mutable.getProperties().get("prices").getProperties().get("01");
        priceNode.properties("currency", new Node().value("USD"));

        TypeGeneralizerV2 generalizer = new TypeGeneralizerV2();
        GeneralizationReport report = generalizer.generalizeToSoundness(blue, mutable, "/prices/01/currency");
        assertTrue(report.hasGeneralizations());
        assertEquals(priceBlueId, priceNode.getType().getBlueId());
    }

    @Test
    void supportsEscapedPropertySegmentsWhenParentIsObject() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: Price\n" +
                        "amount:\n" +
                        "  type: Integer\n" +
                        "currency:\n" +
                        "  type: Text\n"
        );
        String priceBlueId = provider.getBlueIdByName("Price");
        provider.addSingleDocs(
                "name: PriceInEUR\n" +
                        "type:\n" +
                        "  blueId: " + priceBlueId + "\n" +
                        "currency: EUR\n"
        );
        String priceInEURBlueId = provider.getBlueIdByName("PriceInEUR");

        Blue blue = new Blue(provider);
        Node doc = blue.yamlToNode(
                "prices:\n" +
                        "  \"a/b\":\n" +
                        "    type:\n" +
                        "      blueId: " + priceInEURBlueId + "\n" +
                        "    currency: EUR\n" +
                        "    amount: 1\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        Node mutable = snapshot.resolvedRoot().toNode();
        Node priceNode = mutable.getProperties().get("prices").getProperties().get("a/b");
        priceNode.properties("currency", new Node().value("USD"));

        TypeGeneralizerV2 generalizer = new TypeGeneralizerV2();
        GeneralizationReport report = generalizer.generalizeToSoundness(blue, mutable, "/prices/a~1b/currency");
        assertTrue(report.hasGeneralizations());
        assertEquals(priceBlueId, priceNode.getType().getBlueId());
    }

    @Test
    void supportsTrailingEmptyPropertySegmentsWhenParentIsObject() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: Price\n" +
                        "amount:\n" +
                        "  type: Integer\n" +
                        "currency:\n" +
                        "  type: Text\n"
        );
        String priceBlueId = provider.getBlueIdByName("Price");
        provider.addSingleDocs(
                "name: PriceInEUR\n" +
                        "type:\n" +
                        "  blueId: " + priceBlueId + "\n" +
                        "currency: EUR\n"
        );
        String priceInEURBlueId = provider.getBlueIdByName("PriceInEUR");

        Blue blue = new Blue(provider);
        Node doc = blue.yamlToNode(
                "prices:\n" +
                        "  \"\":\n" +
                        "    type:\n" +
                        "      blueId: " + priceInEURBlueId + "\n" +
                        "    currency: EUR\n" +
                        "    amount: 1\n"
        );
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        Node mutable = snapshot.resolvedRoot().toNode();
        Node priceNode = mutable.getProperties().get("prices").getProperties().get("");
        priceNode.properties("currency", new Node().value("USD"));

        TypeGeneralizerV2 generalizer = new TypeGeneralizerV2();
        GeneralizationReport report = generalizer.generalizeToSoundness(blue, mutable, "/prices//currency");
        assertTrue(report.hasGeneralizations());
        assertEquals(priceBlueId, priceNode.getType().getBlueId());
    }
}
