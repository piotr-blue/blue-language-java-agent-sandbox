package blue.language.v2;

import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Disabled("Enabled in Phase 2 when V2 pure-reference hashing is implemented")
class V2Spec_BlueIdReferenceOnlyShortCircuitTest {

    @Test
    void mixedBlueIdObjectMustNotShortCircuit() {
        Node pureReference = new Node().blueId("RefBlueId");
        Node mixed = new Node()
                .blueId("RefBlueId")
                .properties("value", new Node().value(1));

        String referenceId = BlueIdCalculatorV2.calculateSemanticBlueId(pureReference);
        String mixedId = BlueIdCalculatorV2.calculateSemanticBlueId(mixed);

        assertNotEquals(referenceId, mixedId);
    }
}
