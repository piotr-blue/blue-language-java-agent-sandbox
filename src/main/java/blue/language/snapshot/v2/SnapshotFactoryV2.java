package blue.language.snapshot.v2;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.blueid.v2.BlueIdIndex;
import blue.language.blueid.v2.MapBlueIdIndex;
import blue.language.model.Node;
import blue.language.processor.util.PointerUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SnapshotFactoryV2 {

    public ResolvedSnapshotV2 fromAuthoring(Blue blue, Node authoring) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(authoring, "authoring");

        Node preprocessed = blue.preprocess(authoring.clone());
        Node resolved = blue.resolve(preprocessed);
        Node canonical = blue.reverse(resolved.clone());
        return buildSnapshot(canonical, resolved);
    }

    public ResolvedSnapshotV2 fromResolved(Blue blue, Node resolved, SnapshotTrustV2 trust) {
        Objects.requireNonNull(blue, "blue");
        Objects.requireNonNull(resolved, "resolved");
        Objects.requireNonNull(trust, "trust");

        Node resolvedNode = trust == SnapshotTrustV2.BLIND_TRUST_RESOLVED
                ? resolved.clone()
                : blue.resolve(resolved.clone());
        Node canonical = blue.reverse(resolvedNode.clone());
        return buildSnapshot(canonical, resolvedNode);
    }

    private ResolvedSnapshotV2 buildSnapshot(Node canonical, Node resolved) {
        String rootBlueId = BlueIdCalculatorV2.calculateSemanticBlueId(canonical);
        BlueIdIndex index = buildBlueIdIndex(canonical);
        return new ResolvedSnapshotV2(
                FrozenNode.fromNode(canonical),
                FrozenNode.fromNode(resolved),
                rootBlueId,
                index
        );
    }

    private BlueIdIndex buildBlueIdIndex(Node canonicalRoot) {
        Map<String, String> ids = new LinkedHashMap<String, String>();
        indexNode("/", canonicalRoot, ids);
        return MapBlueIdIndex.from(ids);
    }

    private void indexNode(String pointer, Node node, Map<String, String> ids) {
        if (node == null) {
            return;
        }
        ids.put(pointer, BlueIdCalculatorV2.calculateSemanticBlueId(node));

        indexSingle(pointer, "type", node.getType(), ids);
        indexSingle(pointer, "itemType", node.getItemType(), ids);
        indexSingle(pointer, "keyType", node.getKeyType(), ids);
        indexSingle(pointer, "valueType", node.getValueType(), ids);
        indexSingle(pointer, "blue", node.getBlue(), ids);

        List<Node> items = node.getItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                indexNode(append(pointer, String.valueOf(i)), items.get(i), ids);
            }
        }

        Map<String, Node> properties = node.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Node> entry : properties.entrySet()) {
                indexNode(append(pointer, PointerUtils.escapePointerSegment(entry.getKey())), entry.getValue(), ids);
            }
        }
    }

    private void indexSingle(String parentPointer, String segment, Node child, Map<String, String> ids) {
        if (child != null) {
            indexNode(append(parentPointer, segment), child, ids);
        }
    }

    private String append(String pointer, String segment) {
        if ("/".equals(pointer)) {
            return "/" + segment;
        }
        return pointer + "/" + segment;
    }

}
