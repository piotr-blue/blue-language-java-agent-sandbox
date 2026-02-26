package blue.language.sdk.dsl;

import blue.language.model.Node;
import blue.language.sdk.DocBuilder;
import blue.language.sdk.MyOsPermissions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocBuilderMyOsDslParityTest {

    @Test
    void myOsPermissionsBuilderProducesExpectedNode() {
        Node built = MyOsPermissions.create()
                .read(true)
                .write(false)
                .allOps(true)
                .singleOps(" increment ", "", null, "decrement")
                .build();

        assertEquals(Boolean.TRUE, built.get("/read/value"));
        assertEquals(Boolean.FALSE, built.get("/write/value"));
        assertEquals(Boolean.TRUE, built.get("/allOps/value"));
        assertEquals("increment", built.get("/singleOps/0/value"));
        assertEquals("decrement", built.get("/singleOps/1/value"));
    }

    @Test
    void myOsPermissionsSingleOpsCanBeResetToEmptyList() {
        Node built = MyOsPermissions.create()
                .singleOps("one")
                .singleOps((String[]) null)
                .build();

        Node items = built.getAsNode("/singleOps");
        assertEquals(0, items.getItems().size());
    }

    @Test
    void myOsStepsMethodsProduceExpectedEventContracts() {
        Node workerConfig = new Node().properties("mode", new Node().value("safe"));
        Map<String, Object> links = new LinkedHashMap<String, Object>();
        links.put("invoices", MyOsPermissions.create().read(true).allOps(true));
        links.put("profile", new Node().properties("write", new Node().value(true)));

        Node built = DocBuilder.doc()
                .name("MyOS step parity")
                .set("/targetSessionId", "session-42")
                .onInit("bootstrap", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "ownerChannel",
                                "REQ_1",
                                DocBuilder.expr("document('/targetSessionId')"),
                                MyOsPermissions.create().read(true).singleOps("sync"))
                        .myOs().requestLinkedDocsPermission(
                                "ownerChannel",
                                "REQ_2",
                                DocBuilder.expr("document('/targetSessionId')"),
                                links)
                        .myOs().addParticipant("guestChannel", "guest@example.com")
                        .myOs().removeParticipant("legacyChannel")
                        .myOs().callOperation(
                                "ownerChannel",
                                DocBuilder.expr("document('/targetSessionId')"),
                                "sync",
                                Map.of("value", 1))
                        .myOs().subscribeToSession(
                                DocBuilder.expr("document('/targetSessionId')"),
                                "SUB_1")
                        .myOs().startWorkerSession("agentChannel", workerConfig))
                .buildDocument();

        String stepsPath = "/contracts/bootstrap/steps";

        assertEquals("MyOS/Single Document Permission Grant Requested",
                built.getAsText(stepsPath + "/0/event/type/value"));
        assertEquals("ownerChannel", built.getAsText(stepsPath + "/0/event/onBehalfOf/value"));
        assertEquals("REQ_1", built.getAsText(stepsPath + "/0/event/requestId/value"));
        assertEquals("${document('/targetSessionId')}", built.getAsText(stepsPath + "/0/event/targetSessionId/value"));
        assertEquals(Boolean.TRUE, built.get(stepsPath + "/0/event/permissions/read/value"));
        assertEquals("sync", built.getAsText(stepsPath + "/0/event/permissions/singleOps/0/value"));

        assertEquals("MyOS/Linked Documents Permission Grant Requested",
                built.getAsText(stepsPath + "/1/event/type/value"));
        assertEquals("REQ_2", built.getAsText(stepsPath + "/1/event/requestId/value"));
        assertEquals(Boolean.TRUE, built.get(stepsPath + "/1/event/links/invoices/read/value"));
        assertEquals(Boolean.TRUE, built.get(stepsPath + "/1/event/links/profile/write/value"));

        assertEquals("MyOS/Adding Participant Requested",
                built.getAsText(stepsPath + "/2/event/type/value"));
        assertEquals("guestChannel", built.getAsText(stepsPath + "/2/event/channelKey/value"));
        assertEquals("guest@example.com", built.getAsText(stepsPath + "/2/event/email/value"));

        assertEquals("MyOS/Removing Participant Requested",
                built.getAsText(stepsPath + "/3/event/type/value"));
        assertEquals("legacyChannel", built.getAsText(stepsPath + "/3/event/channelKey/value"));

        assertEquals("MyOS/Call Operation Requested",
                built.getAsText(stepsPath + "/4/event/type/value"));
        assertEquals("ownerChannel", built.getAsText(stepsPath + "/4/event/onBehalfOf/value"));
        assertEquals("sync", built.getAsText(stepsPath + "/4/event/operation/value"));
        assertEquals("${document('/targetSessionId')}", built.getAsText(stepsPath + "/4/event/targetSessionId/value"));
        assertEquals("1", String.valueOf(built.get(stepsPath + "/4/event/request/value/value")));

        assertEquals("MyOS/Subscribe to Session Requested",
                built.getAsText(stepsPath + "/5/event/type/value"));
        assertEquals("${document('/targetSessionId')}", built.getAsText(stepsPath + "/5/event/targetSessionId/value"));
        assertEquals("SUB_1", built.getAsText(stepsPath + "/5/event/subscription/id/value"));
        assertEquals(0, built.getAsNode(stepsPath + "/5/event/subscription/events").getItems().size());

        assertEquals("MyOS/Start Worker Session Requested",
                built.getAsText(stepsPath + "/6/event/type/value"));
        assertEquals("agentChannel", built.getAsText(stepsPath + "/6/event/agentChannelKey/value"));
        assertEquals("safe", built.getAsText(stepsPath + "/6/event/config/mode/value"));
    }

    @Test
    void myOsCallOperationOmitsRequestWhenInputIsNull() {
        Node built = DocBuilder.doc()
                .name("Call operation without request")
                .set("/targetSessionId", "session-42")
                .onInit("bootstrap", steps -> steps
                        .myOs().callOperation(
                                "ownerChannel",
                                DocBuilder.expr("document('/targetSessionId')"),
                                "sync",
                                null))
                .buildDocument();

        assertEquals("MyOS/Call Operation Requested",
                built.getAsText("/contracts/bootstrap/steps/0/event/type/value"));
        Node requestNode = built.getAsNode("/contracts/bootstrap/steps/0/event/request");
        assertNull(requestNode.getValue());
        assertNull(requestNode.getType());
        assertNull(requestNode.getProperties());
    }

    @Test
    void myOsStepsValidateRequiredArguments() {
        IllegalArgumentException singleDocFailure = assertThrows(IllegalArgumentException.class, () ->
                DocBuilder.doc()
                        .name("Invalid single-doc permission")
                        .onInit("bootstrap", steps -> steps.myOs().requestSingleDocPermission(
                                "ownerChannel",
                                "",
                                "session-1",
                                MyOsPermissions.create().read(true)))
                        .buildDocument());
        assertEquals("requestId is required", singleDocFailure.getMessage());

        IllegalArgumentException subscribeFailure = assertThrows(IllegalArgumentException.class, () ->
                DocBuilder.doc()
                        .name("Invalid subscribe")
                        .onInit("bootstrap", steps -> steps.myOs().subscribeToSession("session-1", " "))
                        .buildDocument());
        assertEquals("subscriptionId is required", subscribeFailure.getMessage());

        IllegalArgumentException workerFailure = assertThrows(IllegalArgumentException.class, () ->
                DocBuilder.doc()
                        .name("Invalid worker")
                        .onInit("bootstrap", steps -> steps.myOs().startWorkerSession(null, new Node()))
                        .buildDocument());
        assertEquals("agentChannelKey is required", workerFailure.getMessage());
    }
}
