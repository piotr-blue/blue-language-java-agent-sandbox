package blue.language.blueid;

import blue.language.Blue;
import blue.language.model.Constraints;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SemanticBlueIdSpecTest {

    @Test
    void pureReferenceShortCircuitsToReferencedBlueId() {
        Node pureReference = new Node().blueId("ReferenceBlueId");
        assertEquals("ReferenceBlueId", BlueIdCalculator.calculateSemanticBlueId(pureReference));
    }

    @Test
    void mixedBlueIdPayloadDoesNotShortCircuit() {
        Node pureReference = new Node().blueId("ReferenceBlueId");
        Node mixedPayload = new Node().blueId("ReferenceBlueId").properties("x", new Node().value(1));

        assertNotEquals(
                BlueIdCalculator.calculateSemanticBlueId(pureReference),
                BlueIdCalculator.calculateSemanticBlueId(mixedPayload)
        );
    }

    @Test
    void inferredScalarTypeMatchesExplicitScalarTypeInSemanticPipeline() {
        Blue blue = new Blue();
        Node inferred = blue.yamlToNode("value: 1\n");
        Node explicit = blue.yamlToNode(
                "type: Integer\n" +
                        "value: 1\n"
        );

        assertEquals(
                blue.calculateSemanticBlueId(inferred),
                blue.calculateSemanticBlueId(explicit)
        );
    }

    @Test
    void emptyConstraintsDoNotAffectSemanticHash() {
        Node withoutConstraints = new Node().properties("x", new Node().value(1));
        Node withEmptyConstraints = new Node().properties("x", new Node().value(1).constraints(new Constraints()));

        assertEquals(
                BlueIdCalculator.calculateSemanticBlueId(withoutConstraints),
                BlueIdCalculator.calculateSemanticBlueId(withEmptyConstraints)
        );
    }

    @Test
    void singletonListHashDiffersFromScalarHash() {
        Node scalar = new Node().value("x");
        Node singletonList = new Node().items(new Node().value("x"));

        assertNotEquals(
                BlueIdCalculator.calculateSemanticBlueId(scalar),
                BlueIdCalculator.calculateSemanticBlueId(singletonList)
        );
    }

    @Test
    void nullsAndEmptyMapsAreRemovedRecursivelyBeforeHashing() {
        Node compact = new Node().properties("a", new Node().value(1));

        Node noisy = new Node()
                .properties("a", new Node().value(1))
                .properties("ignoredNull", new Node().value(null))
                .properties("ignoredEmptyMap", new Node())
                .properties("nested", new Node()
                        .properties("alsoIgnoredNull", new Node().value(null))
                        .properties("alsoIgnoredEmptyMap", new Node()));

        assertEquals(
                BlueIdCalculator.calculateSemanticBlueId(compact),
                BlueIdCalculator.calculateSemanticBlueId(noisy)
        );
    }

    @Test
    void emptyListsArePreservedDuringCleaning() {
        Node withoutList = new Node().properties("a", new Node().value(1));
        Node withEmptyList = new Node()
                .properties("a", new Node().value(1))
                .properties("items", new Node().items(new ArrayList<Node>()));

        assertNotEquals(
                BlueIdCalculator.calculateSemanticBlueId(withoutList),
                BlueIdCalculator.calculateSemanticBlueId(withEmptyList)
        );
    }

    @Test
    void emptyMapsInsideListsAreRemovedDuringCleaning() {
        Node compact = new Node().items(new Node().value("x"));
        Node noisy = new Node().items(new Node().value("x"), new Node());

        assertEquals(
                BlueIdCalculator.calculateSemanticBlueId(compact),
                BlueIdCalculator.calculateSemanticBlueId(noisy)
        );
    }

    @Test
    void nonContentPreviousControlFormIsIgnoredForHashing() {
        Node compact = new Node().items(new Node().value("x"));
        Node withPreviousControl = new Node().items(
                new Node().properties("$previous", new Node().value(true)),
                new Node().value("x")
        );

        assertEquals(
                BlueIdCalculator.calculateSemanticBlueId(compact),
                BlueIdCalculator.calculateSemanticBlueId(withPreviousControl)
        );
    }

    @Test
    void nonContentPosControlIsRemovedWhileKeepingItemContent() {
        Node compact = new Node().items(new Node().properties("name", new Node().value("x")));
        Node withPosControl = new Node().items(
                new Node()
                        .properties("$pos", new Node().value(0))
                        .properties("name", new Node().value("x"))
        );

        assertEquals(
                BlueIdCalculator.calculateSemanticBlueId(compact),
                BlueIdCalculator.calculateSemanticBlueId(withPosControl)
        );
    }

    @Test
    void wrappedListReferenceInsideItemsIsDistinctFromExpandedItems() {
        BasicNodeProvider provider = new BasicNodeProvider();
        Blue blue = new Blue(provider);

        Node a = blue.yamlToNode("value: A\n");
        Node b = blue.yamlToNode("value: B\n");
        provider.addSingleNodes(a, b);
        provider.addListAndItsItems(Arrays.asList(a, b));

        String wrappedListBlueId = BlueIdCalculator.calculateSemanticBlueId(Arrays.asList(a, b));

        Node expandedAuthoring = new Node().items(a.clone(), b.clone());
        Node wrappedAuthoring = new Node().items(new Node().blueId(wrappedListBlueId));

        assertNotEquals(
                blue.calculateSemanticBlueId(expandedAuthoring),
                blue.calculateSemanticBlueId(wrappedAuthoring)
        );
    }
}
