package blue.language.mapping;

import blue.language.mapping.model.BlueTypeMappedExample;
import blue.language.model.BlueAnnotationsBeanSerializerModifier;
import blue.language.model.BlueType;
import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.utils.BlueIdResolver;
import blue.language.utils.TypeClassResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlueTypeAnnotationCompatibilityTest {

    @Test
    void serializerSupportsBlueTypeAnnotation() throws Exception {
        ObjectMapper mapper = mapperWithBlueAnnotations();
        BlueTypeExample value = new BlueTypeExample();
        value.payload = "hello";

        String json = mapper.writeValueAsString(value);
        assertEquals("{\"type\":{\"blueId\":\"BlueType-Example\"},\"payload\":\"hello\"}", json);
    }

    @Test
    void serializerKeepsLegacyTypeBlueIdBehavior() throws Exception {
        ObjectMapper mapper = mapperWithBlueAnnotations();
        LegacyTypeBlueIdExample value = new LegacyTypeBlueIdExample();
        value.payload = "world";

        String json = mapper.writeValueAsString(value);
        assertEquals("{\"type\":{\"blueId\":\"Legacy-TypeBlueId-Example\"},\"payload\":\"world\"}", json);
    }

    @Test
    void blueIdResolverSupportsBlueTypeAndTypeBlueId() {
        assertEquals("BlueType-Example", BlueIdResolver.resolveBlueId(BlueTypeExample.class));
        assertEquals("Legacy-TypeBlueId-Example", BlueIdResolver.resolveBlueId(LegacyTypeBlueIdExample.class));
    }

    @Test
    void typeClassResolverDiscoversBlueTypeAnnotatedClasses() {
        TypeClassResolver resolver = new TypeClassResolver("blue.language.mapping.model");
        assertEquals(BlueTypeMappedExample.class,
                resolver.getBlueIdMap().get("BlueTypeMappedExample-BlueId"));

        Node typedNode = new Node().type(new Node().blueId("BlueTypeMappedExample-BlueId"));
        Class<?> resolved = resolver.resolveClass(typedNode);
        assertNotNull(resolved);
        assertEquals(BlueTypeMappedExample.class, resolved);
    }

    private ObjectMapper mapperWithBlueAnnotations() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new BlueAnnotationsBeanSerializerModifier());
        mapper.registerModule(module);
        return mapper;
    }

    @BlueType("BlueType-Example")
    static class BlueTypeExample {
        public String payload;
    }

    @TypeBlueId("Legacy-TypeBlueId-Example")
    static class LegacyTypeBlueIdExample {
        public String payload;
    }
}
