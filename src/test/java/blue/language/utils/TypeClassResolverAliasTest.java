package blue.language.utils;

import blue.language.model.Node;
import blue.language.mapping.model.AliasMappedType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TypeClassResolverAliasTest {

    @Test
    void resolvesClassesForAllDeclaredTypeBlueIdAliases() {
        TypeClassResolver resolver = new TypeClassResolver("blue.language.mapping.model");

        Node primary = new Node().type(new Node().blueId("AliasMappedType/Primary"));
        Node secondary = new Node().type(new Node().blueId("AliasMappedType/Secondary"));

        assertSame(AliasMappedType.class, resolver.resolveClass(primary));
        assertSame(AliasMappedType.class, resolver.resolveClass(secondary));
        assertEquals(AliasMappedType.class, resolver.getBlueIdMap().get("AliasMappedType/Primary"));
        assertEquals(AliasMappedType.class, resolver.getBlueIdMap().get("AliasMappedType/Secondary"));
    }
}
