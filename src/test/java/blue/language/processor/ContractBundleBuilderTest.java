package blue.language.processor;

import blue.language.processor.model.DocumentUpdateChannel;
import blue.language.processor.model.LifecycleChannel;
import blue.language.processor.model.ProcessEmbedded;
import blue.language.processor.model.SetProperty;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractBundleBuilderTest {

    @Test
    void setEmbeddedNormalizesAndDeduplicatesPaths() {
        ProcessEmbedded embedded = new ProcessEmbedded()
                .addPath("child")
                .addPath("/child")
                .addPath("/a~1b");

        ContractBundle bundle = ContractBundle.builder()
                .setEmbedded(embedded)
                .build();

        assertEquals(Arrays.asList("/child", "/a~1b"), bundle.embeddedPaths());
    }

    @Test
    void setEmbeddedRejectsMalformedPointerEscapes() {
        ProcessEmbedded embedded = new ProcessEmbedded()
                .addPath("/bad~")
                .addPath("/ok");

        assertThrows(IllegalArgumentException.class,
                () -> ContractBundle.builder().setEmbedded(embedded).build());

        ProcessEmbedded invalidToken = new ProcessEmbedded().addPath("/bad~2");
        assertThrows(IllegalArgumentException.class,
                () -> ContractBundle.builder().setEmbedded(invalidToken).build());
    }

    @Test
    void setEmbeddedTrimsWhitespaceAndSkipsBlankEntries() {
        ProcessEmbedded embedded = new ProcessEmbedded()
                .addPath("  child  ")
                .addPath("   ")
                .addPath("\t/next\t");

        ContractBundle bundle = ContractBundle.builder()
                .setEmbedded(embedded)
                .build();

        assertEquals(Arrays.asList("/child", "/next"), bundle.embeddedPaths());
    }

    @Test
    void builderRejectsBlankContractKeys() {
        assertThrows(IllegalStateException.class,
                () -> ContractBundle.builder().addChannel("   ", new DocumentUpdateChannel()));
        assertThrows(IllegalStateException.class,
                () -> ContractBundle.builder().addHandler("   ", new SetProperty()));
        assertThrows(IllegalStateException.class,
                () -> ContractBundle.builder().addMarker("   ", new ProcessEmbedded()));
    }

    @Test
    void builderTrimsHandlerChannelReferences() {
        SetProperty handler = new SetProperty();
        handler.setChannelKey(" life ");

        ContractBundle bundle = ContractBundle.builder()
                .addChannel("life", new LifecycleChannel())
                .addHandler("setX", handler)
                .build();

        assertEquals(1, bundle.handlersFor("life").size());
    }

    @Test
    void builderRejectsHandlersForMissingChannels() {
        SetProperty handler = new SetProperty();
        handler.setChannelKey("missing");

        assertThrows(IllegalStateException.class,
                () -> ContractBundle.builder().addHandler("setX", handler).build());
    }
}
