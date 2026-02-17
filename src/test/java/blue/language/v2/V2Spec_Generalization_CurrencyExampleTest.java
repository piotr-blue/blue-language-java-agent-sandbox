package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.PatchReport;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V2Spec_Generalization_CurrencyExampleTest {

    @Test
    void patchingCurrencyMustTriggerTypeGeneralization() {
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
                "price:\n" +
                        "  type:\n" +
                        "    blueId: " + priceInEURBlueId + "\n" +
                        "  currency: EUR\n" +
                        "  amount: 10\n"
        );

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        PatchReport report = workingDocument.applyPatch(JsonPatch.replace("/price/currency", new Node().value("USD")));

        assertTrue(report.generalizationReport().hasGeneralizations());
        List<String> generalizations = report.generalizationReport().generalizations();
        assertFalse(generalizations.isEmpty());
        assertTrue(generalizations.get(0).startsWith("/price"));

        ResolvedSnapshotV2 committed = workingDocument.commit();
        assertEquals(priceBlueId, committed.resolvedRoot().toNode().getAsText("/price/type/blueId"));
    }

    @Test
    void patchAndCommitDoNotRequireProviderLookupsForResolvedSnapshots() {
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

        Blue bootstrapBlue = new Blue(provider);
        Node doc = bootstrapBlue.yamlToNode(
                "price:\n" +
                        "  type:\n" +
                        "    blueId: " + priceInEURBlueId + "\n" +
                        "  currency: EUR\n" +
                        "  amount: 10\n"
        );
        ResolvedSnapshotV2 snapshot = bootstrapBlue.resolveToSnapshotV2(doc);

        Blue noLookupBlue = new Blue(blueId -> {
            throw new AssertionError("Unexpected provider lookup for blueId: " + blueId);
        });
        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(noLookupBlue, snapshot);

        PatchReport report = workingDocument.applyPatch(JsonPatch.replace("/price/currency", new Node().value("USD")));
        assertTrue(report.generalizationReport().hasGeneralizations());

        ResolvedSnapshotV2 committed = workingDocument.commit();
        assertEquals(priceBlueId, committed.resolvedRoot().toNode().getAsText("/price/type/blueId"));
    }
}
