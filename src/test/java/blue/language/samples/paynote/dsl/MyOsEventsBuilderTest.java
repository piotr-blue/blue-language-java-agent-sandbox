package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyOsEventsBuilderTest {

    @Test
    void buildsSingleDocumentPermissionGrantRequestedWithoutManualNodeShape() {
        Node event = MyOsEvents.singlePermissionGrantRequested()
                .onBehalfOf("recruitmentChannel")
                .requestId("REQ_1")
                .targetSessionIdExpression("document('/llmProviderSessionId')")
                .readPermission(true)
                .singleOperation("provideInstructions")
                .build();

        assertEquals(TypeAliases.MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED, event.getType().getValue());
        assertEquals("recruitmentChannel", event.getAsText("/onBehalfOf/value"));
        assertEquals("${document('/llmProviderSessionId')}", event.getAsText("/targetSessionId/value"));
        assertEquals("provideInstructions", event.getAsText("/permissions/singleOps/0/value"));
    }

    @Test
    void buildsSubscriptionUpdateFilterWithNestedUpdateType() {
        Node filter = MyOsEvents.subscriptionUpdate("SUB_1")
                .updateType(TypeAliases.MYOS_SESSION_EPOCH_ADVANCED)
                .build();

        assertEquals(TypeAliases.MYOS_SUBSCRIPTION_UPDATE, filter.getType().getValue());
        assertEquals("SUB_1", filter.getAsText("/subscriptionId/value"));
        assertEquals(TypeAliases.MYOS_SESSION_EPOCH_ADVANCED, filter.getAsText("/update/type/value"));
    }
}
