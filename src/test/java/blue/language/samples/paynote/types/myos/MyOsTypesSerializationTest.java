package blue.language.samples.paynote.types.myos;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MyOsTypesSerializationTest {

    private final Blue blue = new Blue();

    @Test
    void serializesDocumentSessionBootstrapType() {
        MyOsTypes.DocumentSessionBootstrap bootstrap = new MyOsTypes.DocumentSessionBootstrap();
        bootstrap.document = new Node().name("Demo Bootstrap Document");

        Node node = blue.objectToNode(bootstrap);

        assertEquals("84xMEnEYr3DPBuYZL3JtcsZBBTtRH9fEEJiPnk7ASj1o", node.getType().getBlueId());
        assertEquals("Demo Bootstrap Document", node.getAsText("/document/name"));
    }

    @Test
    void serializesCallOperationRequestedType() {
        MyOsTypes.CallOperationRequested event = new MyOsTypes.CallOperationRequested();
        event.onBehalfOf = "recruitmentChannel";
        event.targetSessionId = "session-1";
        event.operation = "changeProcessingStatus";
        event.request = new Node().properties("status", new Node().value("processing"));

        Node node = blue.objectToNode(event);

        assertEquals("EVX6nBdHdVEBH9Gbthpd2eqpxaxS4bb9wM55QNdZmcBy", node.getType().getBlueId());
        assertNotNull(node.getProperties().get("request"));
        assertEquals("processing", node.getAsText("/request/status/value"));
    }
}
