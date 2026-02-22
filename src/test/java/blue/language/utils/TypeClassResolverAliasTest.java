package blue.language.utils;

import blue.language.model.Node;
import blue.language.mapping.model.AliasMappedType;
import blue.language.processor.model.ChannelEventCheckpoint;
import blue.language.processor.model.DocumentUpdateChannel;
import blue.language.processor.model.EmbeddedNodeChannel;
import blue.language.processor.model.InitializationMarker;
import blue.language.processor.model.LifecycleChannel;
import blue.language.processor.model.ProcessEmbedded;
import blue.language.processor.model.ProcessingTerminatedMarker;
import blue.language.processor.model.TriggeredEventChannel;
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

    @Test
    void resolvesClassFromInlineTypeChainWhenBlueIdIsDerived() {
        TypeClassResolver resolver = new TypeClassResolver("blue.language.mapping.model");

        Node derived = new Node().type(
                new Node()
                        .blueId("AliasMappedType/Derived")
                        .type(new Node().blueId("AliasMappedType/Primary")));

        assertSame(AliasMappedType.class, resolver.resolveClass(derived));
    }

    @Test
    void resolvesCoreBlueIdAliasesForBuiltInProcessorContracts() {
        TypeClassResolver resolver = new TypeClassResolver("blue.language.processor.model");

        assertSame(DocumentUpdateChannel.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Document Update Channel"))));
        assertSame(EmbeddedNodeChannel.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Embedded Node Channel"))));
        assertSame(LifecycleChannel.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Lifecycle Event Channel"))));
        assertSame(TriggeredEventChannel.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Triggered Event Channel"))));
        assertSame(ProcessEmbedded.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Process Embedded"))));
        assertSame(InitializationMarker.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Processing Initialized Marker"))));
        assertSame(ProcessingTerminatedMarker.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Processing Terminated Marker"))));
        assertSame(ChannelEventCheckpoint.class,
                resolver.resolveClass(new Node().type(new Node().blueId("Core/Channel Event Checkpoint"))));
    }
}
