package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsProgram;

import java.util.LinkedHashMap;

public final class RecruitmentClassifierBootstrapExample {

    private RecruitmentClassifierBootstrapExample() {
    }

    public static Node build(String timestamp,
                             String recruitmentSessionId,
                             String llmProviderSessionId,
                             String currentAccountId) {
        return BlueDocDsl.documentSessionBootstrap()
                .documentName("Recruitment Classifier - " + timestamp)
                .documentType("MyOS/Agent")
                .documentDescription("Classifies linked CVs via llm-provider and emits message for new senior candidates.")
                .putDocumentValue("recruitmentSessionId", recruitmentSessionId)
                .putDocumentValue("llmProviderSessionId", llmProviderSessionId)
                .putDocumentValue("cvSubscriptionId", "SUB_RECRUITMENT_CVS")
                .putDocumentValue("providerSubscriptionId", "SUB_RECRUITMENT_PROVIDER")
                .putDocumentValue("cvSubscriptionReady", false)
                .putDocumentValue("providerSubscriptionReady", false)
                .putDocumentValue("cvMeta", emptyObject())
                .putDocumentValue("pendingClassification", emptyObject())
                .putDocumentValue("alertedSeniorCv", emptyObject())
                .putDocumentValue("classificationByCv", emptyObject())
                .contracts(c -> {
                    c.timelineChannel("recruitmentChannel");
                    c.timelineChannel("myOsAdminChannel");
                    c.lifecycleEventChannel("initLifecycleChannel", "Core/Document Processing Initiated");
                    c.triggeredEventChannel("triggeredEventChannel");

                    c.operation("myOsAdminUpdate", "myOsAdminChannel", null);
                    c.sequentialWorkflowOperation("myOsAdminUpdateImpl", "myOsAdminUpdate", steps -> steps
                            .js("EmitAdminEvents", myOsAdminUpdateProgram()));

                    c.sequentialWorkflow("requestAccess", "initLifecycleChannel", null, steps -> steps
                            .triggerEvent("RequestLlmProviderAccess", requestLlmProviderAccessEvent())
                            .triggerEvent("RequestCvAccess", requestCvAccessEvent()));

                    c.sequentialWorkflow("onCvAccessGranted",
                            "triggeredEventChannel",
                            eventType("MyOS/Single Document Permission Granted"),
                            steps -> steps.js("SubscribeToCvUpdates", onCvAccessGrantedProgram()));

                    c.sequentialWorkflow("onLlmProviderAccessGranted",
                            "triggeredEventChannel",
                            eventType("MyOS/Single Document Permission Granted"),
                            steps -> steps.js("SubscribeToLlmProvider", onLlmAccessGrantedProgram()));

                    c.sequentialWorkflow("onCvSubscriptionInitiated",
                            "triggeredEventChannel",
                            cvSubscriptionInitiatedEventFilter(),
                            steps -> steps
                                    .js("MarkReadyAndSetProcessing", onCvSubscriptionInitiatedProgram())
                                    .updateDocumentFromExpression("PersistCvSubscriptionReady", "steps.MarkReadyAndSetProcessing.changeset"));

                    c.sequentialWorkflow("onProviderSubscriptionInitiated",
                            "triggeredEventChannel",
                            providerSubscriptionInitiatedEventFilter(),
                            steps -> steps
                                    .updateDocument("MarkProviderSubscriptionReady", changeset -> changeset
                                            .replaceValue("/providerSubscriptionReady", true)));

                    c.sequentialWorkflow("onCvEpoch",
                            "triggeredEventChannel",
                            cvEpochEventFilter(),
                            steps -> steps
                                    .js("RequestClassification", onCvEpochProgram())
                                    .updateDocumentFromExpression("PersistClassificationRequest", "steps.RequestClassification.changeset"));

                    c.sequentialWorkflow("onProviderResponse",
                            "triggeredEventChannel",
                            providerResponseFilter(),
                            steps -> steps
                                    .js("ApplyClassificationResult", onProviderResponseProgram())
                                    .updateDocumentFromExpression("PersistClassificationResult", "steps.ApplyClassificationResult.changeset"));
                })
                .bindAccount("recruitmentChannel", currentAccountId)
                .bindAccount("myOsAdminChannel", "0")
                .build();
    }

    private static Node emptyObject() {
        return new Node().properties(new LinkedHashMap<String, Node>());
    }

    private static Node eventType(String typeAlias) {
        return new Node().type(typeAlias);
    }

    private static Node requestLlmProviderAccessEvent() {
        return new Node().type("MyOS/Single Document Permission Grant Requested")
                .properties("onBehalfOf", new Node().value("recruitmentChannel"))
                .properties("requestId", new Node().value("REQ_RECRUITMENT_PROVIDER"))
                .properties("targetSessionId", new Node().value(BlueDocDsl.expr("document('/llmProviderSessionId')")))
                .properties("permissions", new Node()
                        .properties("read", new Node().value(true))
                        .properties("singleOps", new Node().items(new Node().value("provideInstructions"))));
    }

    private static Node requestCvAccessEvent() {
        return new Node().type("MyOS/Linked Documents Permission Grant Requested")
                .properties("onBehalfOf", new Node().value("recruitmentChannel"))
                .properties("requestId", new Node().value("REQ_RECRUITMENT_CVS"))
                .properties("targetSessionId", new Node().value(BlueDocDsl.expr("document('/recruitmentSessionId')")))
                .properties("links", new Node()
                        .properties("cvs", new Node()
                                .properties("read", new Node().value(true))
                                .properties("allOps", new Node().value(true))));
    }

    private static Node cvSubscriptionInitiatedEventFilter() {
        return new Node().type("MyOS/Subscription to Session Initiated")
                .properties("subscriptionId", new Node().value("SUB_RECRUITMENT_CVS"));
    }

    private static Node providerSubscriptionInitiatedEventFilter() {
        return new Node().type("MyOS/Subscription to Session Initiated")
                .properties("subscriptionId", new Node().value("SUB_RECRUITMENT_PROVIDER"));
    }

    private static Node cvEpochEventFilter() {
        return new Node().type("MyOS/Subscription Update")
                .properties("subscriptionId", new Node().value("SUB_RECRUITMENT_CVS"))
                .properties("update", new Node().type("MyOS/Session Epoch Advanced"));
    }

    private static Node providerResponseFilter() {
        return new Node().type("MyOS/Subscription Update")
                .properties("subscriptionId", new Node().value("SUB_RECRUITMENT_PROVIDER"))
                .properties("update", new Node()
                        .type("Conversation/Response")
                        .properties("inResponseTo", new Node()
                                .properties("incomingEvent", new Node()
                                        .properties("requester", new Node().value("RECRUITMENT_CV_CLASSIFIER")))));
    }

    private static JsProgram myOsAdminUpdateProgram() {
        return BlueDocDsl.js(js -> js.returnStatement("{ events: event.message.request }"));
    }

    private static JsProgram onCvAccessGrantedProgram() {
        return BlueDocDsl.js(js -> js
                .lines(
                        "const cvSessionId = event.targetSessionId;",
                        "if (!cvSessionId) {",
                        "  return { events: [] };",
                        "}",
                        "if (cvSessionId === document('/llmProviderSessionId')) {",
                        "  return { events: [] };",
                        "}",
                        "",
                        "const events = [",
                        "  {",
                        "    type: 'MyOS/Subscribe to Session Requested',",
                        "    targetSessionId: cvSessionId,",
                        "    subscription: {",
                        "      id: document('/cvSubscriptionId'),",
                        "      events: [],",
                        "    },",
                        "  },",
                        "];",
                        "",
                        "return { events };"
                ));
    }

    private static JsProgram onLlmAccessGrantedProgram() {
        return BlueDocDsl.js(js -> js
                .lines(
                        "if (event.targetSessionId !== document('/llmProviderSessionId')) {",
                        "  return { events: [] };",
                        "}",
                        "",
                        "return {",
                        "  events: [",
                        "    {",
                        "      type: 'MyOS/Subscribe to Session Requested',",
                        "      targetSessionId: document('/llmProviderSessionId'),",
                        "      subscription: {",
                        "        id: document('/providerSubscriptionId'),",
                        "        events: [",
                        "          {",
                        "            type: 'Conversation/Response',",
                        "          },",
                        "        ],",
                        "      },",
                        "    },",
                        "  ],",
                        "};"
                ));
    }

    private static JsProgram onCvSubscriptionInitiatedProgram() {
        return BlueDocDsl.js(js -> js
                .lines(
                        "const cvSessionId = event.targetSessionId;",
                        "const changeset = [",
                        "  { op: 'replace', path: '/cvSubscriptionReady', val: true },",
                        "];",
                        "",
                        "if (!cvSessionId) {",
                        "  return { changeset, events: [] };",
                        "}",
                        "",
                        "const events = [",
                        "  {",
                        "    type: 'MyOS/Call Operation Requested',",
                        "    onBehalfOf: 'recruitmentChannel',",
                        "    targetSessionId: cvSessionId,",
                        "    operation: 'changeProcessingStatus',",
                        "    request: {",
                        "      status: 'processing',",
                        "    },",
                        "  },",
                        "];",
                        "",
                        "return { changeset, events };"
                ));
    }

    private static JsProgram onCvEpochProgram() {
        return BlueDocDsl.js(js -> js
                .lines(
                        "const cvSessionId = event.update?.sessionId;",
                        "if (!cvSessionId) {",
                        "  return { events: [], changeset: [] };",
                        "}",
                        "",
                        "const cvDoc = event.update?.document ?? {};",
                        "const cvEpoch = event.update?.epoch ?? 0;",
                        "const processingStatus = cvDoc.processingStatus ?? 'pending';",
                        "const requestId = 'REQ_CV_CLASSIFY_' + cvSessionId + '_' + String(cvEpoch);",
                        "",
                        "const cvSnapshot = {",
                        "  cvSessionId,",
                        "  cvEpoch,",
                        "  name: cvDoc.cv?.name ?? cvDoc.name ?? '',",
                        "  experience: cvDoc.cv?.experience ?? cvDoc.experience ?? '',",
                        "  processingStatus,",
                        "};",
                        "",
                        "if (processingStatus === 'processed') {",
                        "  return {",
                        "    changeset: [",
                        "      { op: 'replace', path: '/cvMeta/' + cvSessionId, val: cvSnapshot },",
                        "    ],",
                        "    events: [],",
                        "  };",
                        "}",
                        "",
                        "if (processingStatus === 'pending') {",
                        "  return {",
                        "    changeset: [",
                        "      { op: 'replace', path: '/cvMeta/' + cvSessionId, val: cvSnapshot },",
                        "    ],",
                        "    events: [",
                        "      {",
                        "        type: 'MyOS/Call Operation Requested',",
                        "        onBehalfOf: 'recruitmentChannel',",
                        "        targetSessionId: cvSessionId,",
                        "        operation: 'changeProcessingStatus',",
                        "        request: {",
                        "          status: 'processing',",
                        "        },",
                        "      },",
                        "    ],",
                        "  };",
                        "}",
                        "",
                        "const cvPayload = [",
                        "  'cvSessionId=' + cvSessionId,",
                        "  'cvEpoch=' + cvEpoch,",
                        "  'experience=' + cvSnapshot.experience,",
                        "].join(' | ');",
                        "",
                        "const instructions = [",
                        "  'Return raw JSON only. No markdown and no prose. First character must be { and last character must be }.',",
                        "  'Use EXACT schema: {\"candidateName\":\"string\",\"yearsExperience\":0,\"experienceSummary\":\"string\",\"seniority\":\"junior\",\"isSenior\":false}.',",
                        "  'Read only experience text from the payload.',",
                        "  'Parse yearsExperience from experience text as a number.',",
                        "  'Set seniority to one of: junior, mid, senior.',",
                        "  'Set isSenior based on parsed experience and seniority.',",
                        "  'CV payload: ' + cvPayload,",
                        "].join(' ');",
                        "",
                        "return {",
                        "  changeset: [",
                        "    { op: 'replace', path: '/cvMeta/' + cvSessionId, val: cvSnapshot },",
                        "    { op: 'replace', path: '/pendingClassification/' + requestId, val: { cvSessionId, cvEpoch } },",
                        "  ],",
                        "  events: [",
                        "    {",
                        "      type: 'MyOS/Call Operation Requested',",
                        "      onBehalfOf: 'recruitmentChannel',",
                        "      targetSessionId: document('/llmProviderSessionId'),",
                        "      operation: 'provideInstructions',",
                        "      request: {",
                        "        requestId,",
                        "        requester: 'RECRUITMENT_CV_CLASSIFIER',",
                        "        instructions,",
                        "      },",
                        "    },",
                        "  ],",
                        "};"
                ));
    }

    private static JsProgram onProviderResponseProgram() {
        return BlueDocDsl.js(js -> js
                .lines(
                        "const response = event.update ?? {};",
                        "const inResponseTo = response.inResponseTo ?? {};",
                        "const requestId = inResponseTo.requestId;",
                        "if (!requestId) {",
                        "  return { events: [], changeset: [] };",
                        "}",
                        "",
                        "const pending = document('/pendingClassification/' + requestId);",
                        "if (!pending || !pending.cvSessionId) {",
                        "  return { events: [], changeset: [] };",
                        "}",
                        "",
                        "const cvSessionId = pending.cvSessionId;",
                        "const cvMeta = document('/cvMeta/' + cvSessionId) ?? {};",
                        "const result = response.result ?? {};",
                        "",
                        "const candidateName = result.candidateName || cvMeta.name || 'Unknown candidate';",
                        "const yearsExperienceRaw = result.yearsExperience ?? 0;",
                        "const yearsExperienceNumber = Number(yearsExperienceRaw);",
                        "const yearsExperience = Number.isFinite(yearsExperienceNumber) ? yearsExperienceNumber : 0;",
                        "const experienceSummary = result.experienceSummary || cvMeta.experience || 'No experience summary';",
                        "const seniority = String(result.seniority || 'unknown').toLowerCase();",
                        "const isSenior = result.isSenior === true || result.isSenior === 'true' || seniority === 'senior' || yearsExperience >= 5;",
                        "",
                        "const normalized = {",
                        "  candidateName,",
                        "  yearsExperience,",
                        "  experienceSummary,",
                        "  seniority,",
                        "  isSenior,",
                        "};",
                        "",
                        "const alreadyAlerted = document('/alertedSeniorCv/' + cvSessionId) === true;",
                        "const shouldAlert = isSenior && !alreadyAlerted;",
                        "",
                        "const events = shouldAlert",
                        "  ? [{",
                        "      type: 'Conversation/Chat Message',",
                        "      message: 'New senior CV to review: ' + candidateName + ' (' + experienceSummary + '). CV sessionId: ' + cvSessionId + '.',",
                        "    }]",
                        "  : [];",
                        "",
                        "events.push({",
                        "  type: 'MyOS/Call Operation Requested',",
                        "  onBehalfOf: 'recruitmentChannel',",
                        "  targetSessionId: cvSessionId,",
                        "  operation: 'changeProcessingStatus',",
                        "  request: {",
                        "    status: 'processed',",
                        "  },",
                        "});",
                        "",
                        "const changeset = [",
                        "  { op: 'replace', path: '/classificationByCv/' + cvSessionId, val: normalized },",
                        "  { op: 'remove', path: '/pendingClassification/' + requestId },",
                        "  ...(shouldAlert",
                        "    ? [{ op: 'replace', path: '/alertedSeniorCv/' + cvSessionId, val: true }]",
                        "    : []),",
                        "];",
                        "",
                        "return { events, changeset };"
                ));
    }
}
