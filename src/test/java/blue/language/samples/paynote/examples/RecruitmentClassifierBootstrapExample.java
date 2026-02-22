package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.MyOsEvents;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.TypeAliases;

import java.util.LinkedHashMap;

public final class RecruitmentClassifierBootstrapExample {

    private RecruitmentClassifierBootstrapExample() {
    }

    public static Node build(String timestamp,
                             String recruitmentSessionId,
                             String llmProviderSessionId,
                             String currentAccountId) {
        return MyOsDsl.bootstrap()
                .documentName("Recruitment Classifier - " + timestamp)
                .documentType(TypeAliases.MYOS_AGENT)
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
                    c.withMyOsAdminDefaults();
                    c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
                    c.triggeredEventChannel("triggeredEventChannel");

                    c.onLifecycle("requestAccess", "initLifecycleChannel", steps -> steps
                            .triggerEvent("RequestLlmProviderAccess", requestLlmProviderAccessEvent())
                            .triggerEvent("RequestCvAccess", requestCvAccessEvent()));

                    c.onTriggered("onCvAccessGranted",
                            MyOsEvents.singlePermissionGrantedFilter(),
                            steps -> steps.js("SubscribeToCvUpdates", onCvAccessGrantedProgram()));

                    c.onTriggered("onLlmProviderAccessGranted",
                            MyOsEvents.singlePermissionGrantedFilter(),
                            steps -> steps.js("SubscribeToLlmProvider", onLlmAccessGrantedProgram()));

                    c.onTriggered("onCvSubscriptionInitiated",
                            MyOsEvents.subscriptionInitiated("SUB_RECRUITMENT_CVS").build(),
                            steps -> steps
                                    .js("MarkReadyAndSetProcessing", onCvSubscriptionInitiatedProgram())
                                    .updateDocumentFromExpression("PersistCvSubscriptionReady", "steps.MarkReadyAndSetProcessing.changeset"));

                    c.onTriggered("onProviderSubscriptionInitiated",
                            MyOsEvents.subscriptionInitiated("SUB_RECRUITMENT_PROVIDER").build(),
                            steps -> steps
                                    .updateDocument("MarkProviderSubscriptionReady", changeset -> changeset
                                            .replaceValue("/providerSubscriptionReady", true)));

                    c.onTriggered("onCvEpoch",
                            MyOsEvents.subscriptionUpdate("SUB_RECRUITMENT_CVS")
                                    .updateType(TypeAliases.MYOS_SESSION_EPOCH_ADVANCED)
                                    .build(),
                            steps -> steps
                                    .js("RequestClassification", onCvEpochProgram())
                                    .updateDocumentFromExpression("PersistClassificationRequest", "steps.RequestClassification.changeset"));

                    c.onTriggered("onProviderResponse",
                            MyOsEvents.providerResponseUpdate("SUB_RECRUITMENT_PROVIDER")
                                    .requester("RECRUITMENT_CV_CLASSIFIER")
                                    .build(),
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

    private static Node requestLlmProviderAccessEvent() {
        return MyOsEvents.singlePermissionGrantRequested()
                .onBehalfOf("recruitmentChannel")
                .requestId("REQ_RECRUITMENT_PROVIDER")
                .targetSessionIdExpression("document('/llmProviderSessionId')")
                .readPermission(true)
                .singleOperation("provideInstructions")
                .build();
    }

    private static Node requestCvAccessEvent() {
        return MyOsEvents.linkedPermissionsGrantRequested()
                .onBehalfOf("recruitmentChannel")
                .requestId("REQ_RECRUITMENT_CVS")
                .targetSessionIdExpression("document('/recruitmentSessionId')")
                .linkPermissions("cvs", true, true)
                .build();
    }

    private static JsProgram onCvAccessGrantedProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("cvSessionId", "event.targetSessionId")
                .ifBlock("!cvSessionId", b -> b.returnOutput(
                        JsOutputBuilder.output().eventsArray(JsArrayBuilder.array())))
                .ifBlock("cvSessionId === document('/llmProviderSessionId')", b -> b.returnOutput(
                        JsOutputBuilder.output().eventsArray(JsArrayBuilder.array())))
                .constVar("events", JsArrayBuilder.array()
                        .itemObject(JsObjectBuilder.object()
                                .propString("type", TypeAliases.MYOS_SUBSCRIBE_TO_SESSION_REQUESTED)
                                .propRaw("targetSessionId", "cvSessionId")
                                .propObject("subscription", JsObjectBuilder.object()
                                        .propRaw("id", "document('/cvSubscriptionId')")
                                        .propArray("events", JsArrayBuilder.array())))
                        .build())
                .returnOutput(JsOutputBuilder.output().eventsRaw("events")));
    }

    private static JsProgram onLlmAccessGrantedProgram() {
        return BlueDocDsl.js(js -> js
                .ifBlock("event.targetSessionId !== document('/llmProviderSessionId')", b -> b.returnOutput(
                        JsOutputBuilder.output().eventsArray(JsArrayBuilder.array())))
                .returnOutput(
                        JsOutputBuilder.output().eventsArray(
                                JsArrayBuilder.array().itemObject(
                                        JsObjectBuilder.object()
                                                .propString("type", TypeAliases.MYOS_SUBSCRIBE_TO_SESSION_REQUESTED)
                                                .propRaw("targetSessionId", "document('/llmProviderSessionId')")
                                                .propObject("subscription", JsObjectBuilder.object()
                                                        .propRaw("id", "document('/providerSubscriptionId')")
                                                        .propArray("events", JsArrayBuilder.array()
                                                                .itemObject(JsObjectBuilder.object()
                                                                        .propString("type", TypeAliases.CONVERSATION_RESPONSE))))
                                )
                        )
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
                        "    type: '" + TypeAliases.MYOS_CALL_OPERATION_REQUESTED + "',",
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
                        "        type: '" + TypeAliases.MYOS_CALL_OPERATION_REQUESTED + "',",
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
                        "      type: '" + TypeAliases.MYOS_CALL_OPERATION_REQUESTED + "',",
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
                        "      type: '" + TypeAliases.CONVERSATION_CHAT_MESSAGE + "',",
                        "      message: 'New senior CV to review: ' + candidateName + ' (' + experienceSummary + '). CV sessionId: ' + cvSessionId + '.',",
                        "    }]",
                        "  : [];",
                        "",
                        "events.push({",
                        "  type: '" + TypeAliases.MYOS_CALL_OPERATION_REQUESTED + "',",
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
