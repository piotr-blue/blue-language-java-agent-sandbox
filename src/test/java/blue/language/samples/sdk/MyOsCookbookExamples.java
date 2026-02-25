package blue.language.samples.sdk;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.sdk.DocBuilder;
import blue.language.sdk.MyOsPermissions;
import blue.language.sdk.SimpleDocBuilder;
import blue.language.types.conversation.Response;
import blue.language.types.myos.AddingParticipantResponded;
import blue.language.types.myos.Agent;
import blue.language.types.myos.CallOperationFailed;
import blue.language.types.myos.CallOperationResponded;
import blue.language.types.myos.SessionEpochAdvanced;
import blue.language.types.myos.SingleDocumentPermissionGranted;
import blue.language.types.myos.SubscriptionToSessionInitiated;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MyOsCookbookExamples {

    private MyOsCookbookExamples() {
    }

    public static Map<String, Node> all() {
        Map<String, Node> docs = new LinkedHashMap<String, Node>();
        docs.put("simplePermissionAndSubscribe", simplePermissionAndSubscribe());
        docs.put("callRemoteOperation", callRemoteOperation());
        docs.put("addParticipantsDynamically", addParticipantsDynamically());
        docs.put("linkedDocsWithUpdates", linkedDocsWithUpdates());
        docs.put("cvClassifierFull", cvClassifierFull());
        return docs;
    }

    public static Node simplePermissionAndSubscribe() {
        return SimpleDocBuilder.doc()
                .name("Weather Monitor Agent")
                .type(Agent.class)
                .description("Requests access and subscribes to a provider session.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/weatherSessionId", "session-weather-prod")
                .set("/status", "initializing")
                .onInit("requestWeatherAccess", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "ownerChannel",
                                "REQ_WEATHER",
                                DocBuilder.expr("document('/weatherSessionId')"),
                                MyOsPermissions.create().read(true)))
                .onMyOsResponse("onWeatherAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_WEATHER",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/weatherSessionId')"),
                                        "SUB_WEATHER")
                                .replaceValue("MarkSubscribing", "/status", "subscribing"))
                .onSubscriptionUpdate("onWeatherReady",
                        "SUB_WEATHER",
                        SubscriptionToSessionInitiated.class,
                        steps -> steps.replaceValue("MarkReady", "/status", "monitoring"))
                .buildDocument();
    }

    public static Node callRemoteOperation() {
        return SimpleDocBuilder.doc()
                .name("Data Analyzer Agent")
                .type(Agent.class)
                .description("Calls a provider operation after permission is granted.")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmSessionId", "session-llm-prod")
                .set("/analysisStatus", "idle")
                .onInit("requestLlmAccess", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "ownerChannel",
                                "REQ_LLM",
                                DocBuilder.expr("document('/llmSessionId')"),
                                MyOsPermissions.create().read(true).singleOps("provideInstructions")))
                .onMyOsResponse("onLlmAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_LLM",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/llmSessionId')"),
                                        "SUB_LLM_PROVIDER"))
                .operation("analyze")
                    .channel("ownerChannel")
                    .description("Analyze input with remote provider.")
                    .steps(steps -> steps
                            .replaceValue("MarkAnalyzing", "/analysisStatus", "analyzing")
                            .myOs().callOperation(
                                    "ownerChannel",
                                    DocBuilder.expr("document('/llmSessionId')"),
                                    "provideInstructions",
                                    new ProvideInstructionsRequest()
                                            .requestId("REQ_ANALYZE_001")
                                            .requester("DATA_ANALYZER")
                                            .instructions("Analyze the provided input.")))
                    .done()
                .onEvent("onCallResponded",
                        CallOperationResponded.class,
                        steps -> steps.replaceValue("MarkDone", "/analysisStatus", "complete"))
                .onEvent("onCallFailed",
                        CallOperationFailed.class,
                        steps -> steps.replaceValue("MarkFailed", "/analysisStatus", "failed"))
                .buildDocument();
    }

    public static Node addParticipantsDynamically() {
        return SimpleDocBuilder.doc()
                .name("Team Setup Agent")
                .type(Agent.class)
                .description("Adds participants and marks setup progress.")
                .channel("managerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/status", "setting-up")
                .onInit("addTeamMembers", steps -> steps
                        .myOs().addParticipant("aliceChannel", "alice@company.com")
                        .myOs().addParticipant("bobChannel", "bob@company.com"))
                .onEvent("onParticipantAdded",
                        AddingParticipantResponded.class,
                        steps -> steps.replaceValue("MarkParticipantsAdded", "/status", "participants-added"))
                .onEvent("onEpochAdvanced",
                        SessionEpochAdvanced.class,
                        steps -> steps.replaceValue("MarkReady", "/status", "ready"))
                .buildDocument();
    }

    public static Node linkedDocsWithUpdates() {
        return SimpleDocBuilder.doc()
                .name("Invoice Monitor Agent")
                .type(Agent.class)
                .description("Requests linked-docs permission and tracks invoice totals.")
                .channel("accountingChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/projectSessionId", "session-project-42")
                .set("/invoiceSubscriptionId", "SUB_INVOICES")
                .set("/totalInvoiced", 0)
                .onInit("requestInvoiceAccess", steps -> steps
                        .myOs().requestLinkedDocsPermission(
                                "accountingChannel",
                                "REQ_INVOICES",
                                DocBuilder.expr("document('/projectSessionId')"),
                                Map.of("invoices", MyOsPermissions.create().read(true).allOps(true))))
                .onMyOsResponse("onInvoiceAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_INVOICES",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("event.targetSessionId"),
                                        DocBuilder.expr("document('/invoiceSubscriptionId')")))
                .onSubscriptionUpdate("onInvoiceUpdate",
                        "SUB_INVOICES",
                        steps -> steps
                                .jsRaw("ProcessInvoiceUpdate", """
                                        const amount = Number(event.update?.amount ?? 0);
                                        const current = Number(document('/totalInvoiced') ?? 0);
                                        return { events: [], changeset: [{
                                          op:'replace',
                                          path:'/totalInvoiced',
                                          val: current + amount
                                        }]};
                                        """)
                                .updateDocumentFromExpression("PersistInvoiceTotal",
                                        "steps.ProcessInvoiceUpdate.changeset"))
                .buildDocument();
    }

    public static Node cvClassifierFull() {
        return SimpleDocBuilder.doc()
                .name("Recruitment Classifier")
                .type(Agent.class)
                .description("Requests access, subscribes, and invokes provider classification.")
                .channel("recruitmentChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/recruitmentSessionId", "session-recruitment-001")
                .set("/llmProviderSessionId", "session-llm-001")
                .set("/cvSubscriptionId", "SUB_RECRUITMENT_CVS")
                .onInit("requestAccess", steps -> steps
                        .myOs().requestSingleDocPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_PROVIDER",
                                DocBuilder.expr("document('/llmProviderSessionId')"),
                                MyOsPermissions.create().read(true).singleOps("provideInstructions"))
                        .myOs().requestLinkedDocsPermission(
                                "recruitmentChannel",
                                "REQ_RECRUITMENT_CVS",
                                DocBuilder.expr("document('/recruitmentSessionId')"),
                                Map.of("cvs", MyOsPermissions.create().read(true).allOps(true))))
                .onMyOsResponse("onCvAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_RECRUITMENT_CVS",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("event.targetSessionId"),
                                        DocBuilder.expr("document('/cvSubscriptionId')")))
                .onMyOsResponse("onLlmProviderAccessGranted",
                        SingleDocumentPermissionGranted.class,
                        "REQ_RECRUITMENT_PROVIDER",
                        steps -> steps
                                .myOs().subscribeToSession(
                                        DocBuilder.expr("document('/llmProviderSessionId')"),
                                        "SUB_RECRUITMENT_PROVIDER"))
                .onSubscriptionUpdate("onCvArrived",
                        "SUB_RECRUITMENT_CVS",
                        steps -> steps.myOs().callOperation(
                                "recruitmentChannel",
                                DocBuilder.expr("document('/llmProviderSessionId')"),
                                "provideInstructions",
                                new ProvideInstructionsRequest()
                                        .requestId("REQ_CV_CLASSIFY")
                                        .requester("RECRUITMENT_CLASSIFIER")
                                        .instructions("Classify candidate seniority based on CV update.")))
                .onSubscriptionUpdate("onClassificationResult",
                        "SUB_RECRUITMENT_PROVIDER",
                        Response.class,
                        steps -> steps
                                .jsRaw("ProcessResult", """
                                        const response = event.update ?? {};
                                        const requestId = response.inResponseTo?.requestId ?? 'unknown';
                                        return { events: [], changeset: [{
                                          op:'replace',
                                          path:'/lastClassificationRequestId',
                                          val: requestId
                                        }]};
                                        """)
                                .updateDocumentFromExpression("PersistResult",
                                        "steps.ProcessResult.changeset"))
                .buildDocument();
    }

    @TypeBlueId("PayNote2-Provide-Instructions-Request-BlueId")
    public static final class ProvideInstructionsRequest {
        public String requestId;
        public String requester;
        public String instructions;

        public ProvideInstructionsRequest requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ProvideInstructionsRequest requester(String requester) {
            this.requester = requester;
            return this;
        }

        public ProvideInstructionsRequest instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }
    }
}
