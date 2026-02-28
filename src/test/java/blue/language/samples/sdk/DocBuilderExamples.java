package blue.language.samples.sdk;

import blue.language.model.Node;
import blue.language.sdk.DocBuilder;
import blue.language.sdk.MyOsPermissions;
import blue.language.sdk.SimpleDocBuilder;
import blue.language.types.myos.AddingParticipantRequested;
import blue.language.types.myos.Agent;
import blue.language.types.myos.CallOperationRequested;
import blue.language.types.myos.SessionEpochAdvanced;
import blue.language.types.myos.SingleDocumentPermissionGranted;

import java.util.Map;

public final class DocBuilderExamples {

    private DocBuilderExamples() {
    }

    public static Node simpleAgentWithPermissions() {
        return SimpleDocBuilder.doc()
                .name("Simple Permission Agent")
                .type(Agent.class)
                .description("Requests read access to provider session on init.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .field("/providerSessionId", "session-abc-123")
                .onInit("requestProviderAccess", steps -> steps.myOs().requestSingleDocPermission(
                        "ownerChannel",
                        "REQ_PROVIDER",
                        DocBuilder.expr("document('/providerSessionId')"),
                        MyOsPermissions.create().read(true).singleOps("getStatus")))
                .onMyOsResponse("onProviderAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_PROVIDER",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/providerSessionId')"),
                                        "SUB_PROVIDER")
                                .replaceValue("MarkReady", "/status", "ready"))
                .buildDocument();
    }

    public static Node agentAddsParticipantAndWaits() {
        return SimpleDocBuilder.doc()
                .name("Collaboration Setup Agent")
                .type(Agent.class)
                .description("Adds Bob as participant and marks setup progress.")
                .channel("aliceChannel")
                .myOsAdmin("myOsAdminChannel")
                .onInit("addBob", steps -> steps
                        .myOs().addParticipant("bobChannel", "bob@gmail.com"))
                .onEvent("onBobAdded",
                        AddingParticipantRequested.class,
                        steps -> steps.replaceValue("MarkBobAdded", "/participants/bob", "added"))
                .onEvent("onEpochAdvanced",
                        SessionEpochAdvanced.class,
                        steps -> steps.replaceValue("Activate", "/status", "active"))
                .buildDocument();
    }

    public static Node agentCallsRemoteOperation() {
        return SimpleDocBuilder.doc()
                .name("Remote Operation Caller")
                .type(Agent.class)
                .description("Calls operation on linked session when /trigger changes.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .field("/linkedSessionId", "session-xyz-789")
                .onDocChange("onTriggerChanged", "/trigger", steps -> steps
                        .myOs().callOperation(
                                "ownerChannel",
                                DocBuilder.expr("document('/linkedSessionId')"),
                                "processData",
                                null))
                .onEvent("onCallQueued",
                        CallOperationRequested.class,
                        steps -> steps.replaceValue("MarkQueued", "/remoteCallStatus", "queued"))
                .buildDocument();
    }

    public static Node cvClassifierAgent() {
        return SimpleDocBuilder.doc()
                .name("CV Classifier Agent")
                .type(Agent.class)
                .description("Classifies linked CVs via llm-provider.")
                .channel("recruitmentChannel")
                .myOsAdmin("myOsAdminChannel")
                .field("/llmProviderSessionId", "session-llm-001")
                .field("/recruitmentSessionId", "session-recruitment-001")
                .field("/cvSubscriptionId", "SUB_CV_UPDATES")
                .onInit("requestAccess", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_PROVIDER",
                                DocBuilder.expr("document('/llmProviderSessionId')"),
                                MyOsPermissions.create()
                                        .read(true)
                                        .singleOps("provideInstructions"))
                        .myOs().requestLinkedDocsPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_CVS",
                                DocBuilder.expr("document('/recruitmentSessionId')"),
                                Map.of("cvs", MyOsPermissions.create()
                                        .read(true)
                                        .allOps(true))))
                .onMyOsResponse("onLlmProviderAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_RECRUITMENT_PROVIDER",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/llmProviderSessionId')"),
                                        "SUB_RECRUITMENT_PROVIDER"))
                .buildDocument();
    }
}
