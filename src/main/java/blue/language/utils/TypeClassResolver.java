package blue.language.utils;

import blue.language.NodeProvider;
import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.blueid.BlueIdCalculator;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeClassResolver {

    private final Map<String, Class<?>> blueIdMap = new HashMap<>();
    private final NodeProvider nodeProvider;

    public TypeClassResolver(String... packagesToScan) {
        this(null, packagesToScan);
    }

    public TypeClassResolver(NodeProvider nodeProvider, String... packagesToScan) {
        this.nodeProvider = nodeProvider;
        for (String packageName : packagesToScan) {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(packageName))
                    .filterInputsBy(new FilterBuilder().includePackage(packageName))
                    .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes));

            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(TypeBlueId.class);

            for (Class<?> clazz : annotatedClasses) {
                TypeBlueId annotation = clazz.getAnnotation(TypeBlueId.class);
                registerClass(clazz, annotation);
            }
        }
    }

    private void registerClass(Class<?> clazz, TypeBlueId annotation) {
        List<String> blueIds = resolveBlueIds(clazz, annotation);
        for (String blueId : blueIds) {
            Class<?> existing = blueIdMap.get(blueId);
            if (existing != null && !existing.equals(clazz)) {
                throw new IllegalStateException("Duplicate BlueId value: " + blueId);
            }
            blueIdMap.put(blueId, clazz);
        }
    }

    private List<String> resolveBlueIds(Class<?> clazz, TypeBlueId annotation) {
        List<String> result = new ArrayList<>();
        if (annotation == null) {
            return result;
        }

        String[] values = annotation.value();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    result.add(value.trim());
                }
            }
        }
        if (!annotation.defaultValue().isEmpty()) {
            result.add(annotation.defaultValue());
        }
        if (result.isEmpty()) {
            String repositoryBlueId = BlueIdResolver.resolveBlueId(clazz);
            if (repositoryBlueId != null && !repositoryBlueId.trim().isEmpty()) {
                result.add(repositoryBlueId.trim());
            }
        }
        return result;
    }

    public Class<?> resolveClass(Node node) {
        if (node == null) {
            return null;
        }
        String blueId = getEffectiveBlueId(node);
        if (blueId == null) {
            return resolveClassByTypeChain(node.getType(), new LinkedHashSet<String>());
        }

        Class<?> exact = blueIdMap.get(blueId);
        if (exact != null) {
            return exact;
        }
        return resolveClassByTypeChain(node.getType(), new LinkedHashSet<String>());
    }

    private String getEffectiveBlueId(Node node) {
        if (node.getType() != null && node.getType().getBlueId() != null) {
            return node.getType().getBlueId();
        } else if (node.getType() != null) {
            return BlueIdCalculator.calculateSemanticBlueId(node.getType());
        }
        return null;
    }

    private Class<?> resolveClassByTypeChain(Node typeNode, Set<String> visitedBlueIds) {
        if (typeNode == null) {
            return null;
        }

        String currentBlueId = typeNode.getBlueId();
        if (currentBlueId != null && !currentBlueId.trim().isEmpty()) {
            String normalized = currentBlueId.trim();
            Class<?> match = blueIdMap.get(normalized);
            if (match != null) {
                return match;
            }
            if (!visitedBlueIds.add(normalized)) {
                return null;
            }
            Class<?> resolvedFromProvider = resolveClassFromProviderType(normalized, visitedBlueIds);
            if (resolvedFromProvider != null) {
                return resolvedFromProvider;
            }
        }
        return resolveClassByTypeChain(typeNode.getType(), visitedBlueIds);
    }

    private Class<?> resolveClassFromProviderType(String blueId, Set<String> visitedBlueIds) {
        if (nodeProvider == null || blueId == null || blueId.trim().isEmpty()) {
            return null;
        }
        Node typeDefinition;
        try {
            typeDefinition = nodeProvider.fetchFirstByBlueId(blueId);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (typeDefinition == null || typeDefinition.getType() == null) {
            return null;
        }
        return resolveClassByTypeChain(typeDefinition.getType(), visitedBlueIds);
    }

    public Map<String, Class<?>> getBlueIdMap() {
        return Collections.unmodifiableMap(blueIdMap);
    }

    public NodeProvider nodeProvider() {
        return nodeProvider;
    }
}