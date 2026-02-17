package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class V2Spec_SemanticBlueIdStabilityTest {

    @Test
    void redundantInheritedOverrideDoesNotChangeSemanticBlueId() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: BaseType\n" +
                        "x: 1\n"
        );

        Blue blue = new Blue(provider);
        String baseTypeBlueId = provider.getBlueIdByName("BaseType");

        Node leanAuthoring = blue.yamlToNode(
                "name: Child\n" +
                        "type:\n" +
                        "  blueId: " + baseTypeBlueId + "\n"
        );

        Node noisyAuthoring = blue.yamlToNode(
                "name: Child\n" +
                        "type:\n" +
                        "  blueId: " + baseTypeBlueId + "\n" +
                        "x: 1\n"
        );

        String leanId = blue.calculateSemanticBlueIdV2(leanAuthoring);
        String noisyId = blue.calculateSemanticBlueIdV2(noisyAuthoring);

        assertEquals(leanId, noisyId);

        String legacyLeanId = blue.calculateBlueId(leanAuthoring);
        String legacyNoisyId = blue.calculateBlueId(noisyAuthoring);
        assertNotEquals(legacyLeanId, legacyNoisyId);
    }
}
