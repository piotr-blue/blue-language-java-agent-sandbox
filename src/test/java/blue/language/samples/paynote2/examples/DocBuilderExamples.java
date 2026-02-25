package blue.language.samples.paynote2.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import blue.language.samples.paynote2.sdk.DocBuilder;
import blue.language.samples.paynote2.sdk.MyOsPermissions;
import blue.language.samples.paynote2.sdk.SimpleDocBuilder;

import java.util.Map;

public final class DocBuilderExamples {

    private DocBuilderExamples() {
    }

    public static Node simpleAgentWithPermissions() {
        return SimpleDocBuilder.doc()
                .name("Simple Permission Agent")
                .type(MyOsTypes.Agent.class)
                .description("Requests read access to provider session on init.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/providerSessionId", "session-abc-123")
                .onInit("requestProviderAccess", steps -> steps.myOs().requestSingleDocPermission(
                        "ownerChannel",
                        "REQ_PROVIDER",
                        DocBuilder.expr("document('/providerSessionId')"),
                        MyOsPermissions.create().read(true).singleOps("getStatus")))
                .onMyOsResponse("onProviderAccessGranted",
                        MyOsTypes.SingleDocumentPermissionGranted.class,
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
                .type(MyOsTypes.Agent.class)
                .description("Adds Bob as participant and marks setup progress.")
                .channel("aliceChannel")
                .myOsAdmin("myOsAdminChannel")
                .onInit("addBob", steps -> steps
                        .myOs().addParticipant("bobChannel", "bob@gmail.com"))
                .onEvent("onBobAdded",
                        MyOsTypes.AddingParticipantRequested.class,
                        steps -> steps.replaceValue("MarkBobAdded", "/participants/bob", "added"))
                .onEvent("onEpochAdvanced",
                        MyOsTypes.SessionEpochAdvanced.class,
                        steps -> steps.replaceValue("Activate", "/status", "active"))
                .buildDocument();
    }

    public static Node agentCallsRemoteOperation() {
        return SimpleDocBuilder.doc()
                .name("Remote Operation Caller")
                .type(MyOsTypes.Agent.class)
                .description("Calls operation on linked session when /trigger changes.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/linkedSessionId", "session-xyz-789")
                .onDocChange("onTriggerChanged", "/trigger", steps -> steps
                        .myOs().callOperation(
                                "ownerChannel",
                                DocBuilder.expr("document('/linkedSessionId')"),
                                "processData",
                                null))
                .onEvent("onCallQueued",
                        MyOsTypes.CallOperationRequested.class,
                        steps -> steps.replaceValue("MarkQueued", "/remoteCallStatus", "queued"))
                .buildDocument();
    }

    public static Node cvClassifierAgent() {
        return SimpleDocBuilder.doc()
                .name("CV Classifier Agent")
                .type(MyOsTypes.Agent.class)
                .description("Classifies linked CVs via llm-provider.")
                .channel("recruitmentChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmProviderSessionId", "session-llm-001")
                .set("/recruitmentSessionId", "session-recruitment-001")
                .set("/cvSubscriptionId", "SUB_CV_UPDATES")
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
                        MyOsTypes.SingleDocumentPermissionGranted.class,
                        "REQ_RECRUITMENT_PROVIDER",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/llmProviderSessionId')"),
                                        "SUB_RECRUITMENT_PROVIDER"))
                .buildDocument();
    }
}
