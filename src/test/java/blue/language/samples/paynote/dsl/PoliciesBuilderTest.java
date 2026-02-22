package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoliciesBuilderTest {

    @Test
    void attachesContractsChangePolicyAllowListAndRateLimit() {
        Node bootstrap = MyOsDsl.bootstrap()
                .documentName("Policy Harness")
                .documentType(PayNoteAliases.PAYNOTE)
                .policies(p -> p
                        .contractsChangePolicy("allow-listed-direct-change", "only approved paths are mutable")
                        .changesetAllowList("directChange", "/amount/total", "/details/description")
                        .operationRateLimit("directChange", 3, "PT10M"))
                .build();

        assertEquals("allow-listed-direct-change",
                bootstrap.getAsText("/document/policies/contractsChangePolicy/mode/value"));
        assertEquals("/amount/total",
                bootstrap.getAsText("/document/policies/changesetAllowList/directChange/0/value"));
        assertEquals(3,
                bootstrap.getAsInteger("/document/policies/operationRateLimit/directChange/maxCalls/value").intValue());
        assertEquals("PT10M",
                bootstrap.getAsText("/document/policies/operationRateLimit/directChange/window/value"));
    }
}
