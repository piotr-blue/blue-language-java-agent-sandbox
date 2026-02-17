package blue.language.v2;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import blue.language.utils.BlueIdCalculator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

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

        String legacyLeanId = BlueIdCalculator.calculateBlueId(leanAuthoring);
        String legacyNoisyId = BlueIdCalculator.calculateBlueId(noisyAuthoring);
        assertNotEquals(legacyLeanId, legacyNoisyId);
    }

    @Test
    void publicCalculateBlueIdDelegatesToSemanticV2ForNodeAndObject() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: BaseType\n" +
                        "x: 1\n"
        );

        Blue blue = new Blue(provider);
        String baseTypeBlueId = provider.getBlueIdByName("BaseType");

        Node noisyAuthoring = blue.yamlToNode(
                "name: Child\n" +
                        "type:\n" +
                        "  blueId: " + baseTypeBlueId + "\n" +
                        "x: 1\n"
        );

        String semanticId = blue.calculateSemanticBlueIdV2(noisyAuthoring);
        assertEquals(semanticId, blue.calculateBlueId(noisyAuthoring));

        Map<String, Object> objectAuthoring = new LinkedHashMap<String, Object>();
        objectAuthoring.put("name", "Child");
        Map<String, Object> type = new LinkedHashMap<String, Object>();
        type.put("blueId", baseTypeBlueId);
        objectAuthoring.put("type", type);
        objectAuthoring.put("x", 1);

        assertEquals(semanticId, blue.calculateBlueId(objectAuthoring));
    }
}
