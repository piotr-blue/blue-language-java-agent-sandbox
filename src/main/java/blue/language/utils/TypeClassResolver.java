package blue.language.utils;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.blueid.BlueIdCalculator;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeClassResolver {

    private final Map<String, Class<?>> blueIdMap = new HashMap<>();

    public TypeClassResolver(String... packagesToScan) {
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
        boolean registered = false;

        String[] declaredBlueIds = annotation.value();
        if (declaredBlueIds != null && declaredBlueIds.length > 0) {
            for (String declaredBlueId : declaredBlueIds) {
                registerBlueId(clazz, declaredBlueId);
                registered = true;
            }
        }

        if (!registered && annotation.defaultValue() != null && !annotation.defaultValue().trim().isEmpty()) {
            registerBlueId(clazz, annotation.defaultValue());
            registered = true;
        }

        if (!registered) {
            registerBlueId(clazz, BlueIdResolver.resolveBlueId(clazz));
        }
    }

    private void registerBlueId(Class<?> clazz, String blueId) {
        if (blueId == null || blueId.trim().isEmpty()) {
            return;
        }
        String normalizedBlueId = blueId.trim();
        Class<?> existing = blueIdMap.get(normalizedBlueId);
        if (existing != null && !existing.equals(clazz)) {
            throw new IllegalStateException("Duplicate BlueId value: " + normalizedBlueId);
        }
        blueIdMap.put(normalizedBlueId, clazz);
    }

    public Class<?> resolveClass(Node node) {
        String blueId = getEffectiveBlueId(node);
        if (blueId == null) {
            return null;
        }

        return blueIdMap.get(blueId);
    }

    private String getEffectiveBlueId(Node node) {
        if (node.getType() != null && node.getType().getBlueId() != null) {
            return node.getType().getBlueId();
        } else if (node.getType() != null) {
            return BlueIdCalculator.calculateSemanticBlueId(node.getType());
        }
        return null;
    }

    public Map<String, Class<?>> getBlueIdMap() {
        return Collections.unmodifiableMap(blueIdMap);
    }
}