package blue.language.provider;

import blue.language.model.Node;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BasicNodeProviderV2Test {

    @Test
    void addSingleDocsV2StoresCanonicalSnapshot() {
        BasicNodeProvider provider = new BasicNodeProvider();

        String semanticBlueId = provider.addSingleDocsV2(
                "name: Counter\n" +
                        "counter: 0\n"
        );

        assertNotNull(semanticBlueId);
        Optional<ResolvedSnapshotV2> snapshot = provider.findSnapshotBySemanticBlueId(semanticBlueId);
        assertTrue(snapshot.isPresent());
        assertEquals(semanticBlueId, snapshot.get().rootBlueId());

        List<Node> fetched = provider.fetchByBlueId(semanticBlueId);
        assertNotNull(fetched);
        assertEquals(1, fetched.size());
        assertEquals("Counter", fetched.get(0).getName());
    }

    @Test
    void addMultipleDocsV2StoresListBySemanticBlueId() {
        BasicNodeProvider provider = new BasicNodeProvider();

        String listBlueId = provider.addMultipleDocsV2(
                "- name: A\n" +
                        "  v: 1\n" +
                        "- name: B\n" +
                        "  v: 2\n"
        );

        assertNotNull(listBlueId);
        List<Node> list = provider.fetchByBlueId(listBlueId);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("A", list.get(0).getName());
        assertEquals("B", list.get(1).getName());

        List<Node> second = provider.fetchByBlueId(listBlueId + "#1");
        assertNotNull(second);
        assertEquals(1, second.size());
        assertEquals("B", second.get(0).getName());
    }
}
