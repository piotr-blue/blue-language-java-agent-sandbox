package blue.language.processor;

import blue.language.processor.model.ProcessEmbedded;
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
}
