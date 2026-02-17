package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.v2.PatchReport;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.snapshot.v2.WorkingDocumentV2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Enabled in Phase 5 when dynamic type generalization is implemented")
class V2Spec_Generalization_CurrencyExampleTest {

    @Test
    void patchingCurrencyMustTriggerTypeGeneralization() {
        Blue blue = new Blue();
        Node doc = blue.yamlToNode(
                "price:\n" +
                        "  type:\n" +
                        "    name: PriceInEUR\n" +
                        "  currency: EUR\n" +
                        "  amount: 10\n"
        );

        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(doc);
        WorkingDocumentV2 workingDocument = WorkingDocumentV2.forSnapshot(blue, snapshot);
        PatchReport report = workingDocument.applyPatch(JsonPatch.replace("/price/currency", new Node().value("USD")));

        assertTrue(report.generalizationReport().hasGeneralizations());
    }
}
