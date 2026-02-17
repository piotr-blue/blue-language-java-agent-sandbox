package blue.language.v2;

import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class V2Spec_ListHashingDoesNotCollapseSingletonTest {

    @Test
    void singletonListAndScalarMustHashDifferently() {
        Node scalar = new Node().value("x");
        Node singletonList = new Node().items(new Node().value("x"));

        String scalarId = BlueIdCalculatorV2.calculateSemanticBlueId(scalar);
        String singletonListId = BlueIdCalculatorV2.calculateSemanticBlueId(singletonList);

        assertNotEquals(scalarId, singletonListId);
    }

    @Test
    void emptyListMustNotEqualMissingField() {
        Node withEmptyList = new Node().properties("values", new Node().items());
        Node missingList = new Node();

        String emptyListId = BlueIdCalculatorV2.calculateSemanticBlueId(withEmptyList);
        String missingListId = BlueIdCalculatorV2.calculateSemanticBlueId(missingList);

        assertNotEquals(emptyListId, missingListId);
    }
}
