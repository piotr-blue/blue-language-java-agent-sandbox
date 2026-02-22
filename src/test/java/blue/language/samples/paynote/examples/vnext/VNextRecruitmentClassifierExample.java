package blue.language.samples.paynote.examples.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsCommon;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.MyOsEvents;
import blue.language.samples.paynote.dsl.TypeAliases;

public final class VNextRecruitmentClassifierExample {

    private VNextRecruitmentClassifierExample() {
    }

    public static Node build(String timestamp,
                             String recruitmentSessionId,
                             String llmProviderSessionId,
                             String currentAccountId) {
        return MyOsDsl.bootstrap()
                .documentName("Recruitment Classifier vNext - " + timestamp)
                .documentType(TypeAliases.MYOS_AGENT)
                .documentDescription("Classifies linked CVs via llm-provider and emits typed events for senior candidates.")
                .putDocumentValue("recruitmentSessionId", recruitmentSessionId)
                .putDocumentValue("llmProviderSessionId", llmProviderSessionId)
                .putDocumentValue("cvSubscriptionId", "SUB_RECRUITMENT_CVS")
                .putDocumentValue("providerSubscriptionId", "SUB_RECRUITMENT_PROVIDER")
                .putDocumentObject("pendingClassification", obj -> {
                })
                .putDocumentObject("classificationByCv", obj -> {
                })
                .putDocumentObject("alertedSeniorCv", obj -> {
                })
                .contracts(c -> {
                    c.timelineChannels("recruitmentChannel", "myOsAdminChannel");
                    c.withMyOsAdminDefaults();
                    c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
                    c.triggeredEventChannel("triggeredEventChannel");

                    c.onLifecycle("requestAccess", "initLifecycleChannel", steps -> steps
                            .triggerEvent("RequestLlmProviderAccess", MyOsEvents.singlePermissionGrantRequested()
                                    .onBehalfOf("recruitmentChannel")
                                    .requestId("REQ_RECRUITMENT_PROVIDER")
                                    .targetSessionIdExpression("document('/llmProviderSessionId')")
                                    .readPermission(true)
                                    .singleOperation("provideInstructions")
                                    .build())
                            .triggerEvent("RequestCvAccess", MyOsEvents.linkedPermissionsGrantRequested()
                                    .onBehalfOf("recruitmentChannel")
                                    .requestId("REQ_RECRUITMENT_CVS")
                                    .targetSessionIdExpression("document('/recruitmentSessionId')")
                                    .linkPermissions("cvs", true, true)
                                    .build()));

                    c.onTriggered("onCvEpoch",
                            MyOsEvents.subscriptionUpdate("SUB_RECRUITMENT_CVS")
                                    .updateType(TypeAliases.MYOS_SESSION_EPOCH_ADVANCED)
                                    .build(),
                            steps -> steps
                                    .js("RequestClassification", requestClassificationProgram())
                                    .updateDocumentFromExpression("PersistClassificationRequest",
                                            "steps.RequestClassification.changeset"));

                    c.onTriggered("onProviderResponse",
                            MyOsEvents.providerResponseUpdate("SUB_RECRUITMENT_PROVIDER")
                                    .requester("RECRUITMENT_CV_CLASSIFIER")
                                    .build(),
                            steps -> steps
                                    .js("ApplyProviderResponse", applyProviderResponseProgram())
                                    .updateDocumentFromExpression("PersistClassificationResult",
                                            "steps.ApplyProviderResponse.changeset"));
                })
                .bindAccount("recruitmentChannel", currentAccountId)
                .bindAccount("myOsAdminChannel", "0")
                .build();
    }

    private static JsProgram requestClassificationProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("cvSessionId", "event.update?.sessionId")
                .ifBlock("!cvSessionId", block -> block.returnOutput(
                        JsOutputBuilder.output()
                                .changesetRaw("[]")
                                .emptyEvents()))
                .constVar("cvEpoch", "event.update?.epoch ?? 0")
                .constVar("cvDoc", "event.update?.document ?? {}")
                .constVar("requestId", "'REQ_CV_CLASSIFY_' + cvSessionId + '_' + String(cvEpoch)")
                .constVar("experience", JsCommon.coalesce("cvDoc.cv?.experience", "cvDoc.experience", "''"))
                .constVar("instructions",
                        "'Return raw JSON only with schema {\"candidateName\":\"string\",\"yearsExperience\":0,\"experienceSummary\":\"string\",\"seniority\":\"junior\",\"isSenior\":false}. ' + " +
                                "'Experience: ' + experience")
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw(JsPatchBuilder.patch()
                                .replaceValue("/pendingClassification", "{ ...document('/pendingClassification'), [requestId]: { cvSessionId, cvEpoch } }")
                                .build())
                        .eventsArray(JsArrayBuilder.array()
                                .itemObject(JsObjectBuilder.object()
                                        .propString("type", TypeAliases.MYOS_CALL_OPERATION_REQUESTED)
                                        .propString("onBehalfOf", "recruitmentChannel")
                                        .propRaw("targetSessionId", "document('/llmProviderSessionId')")
                                        .propString("operation", "provideInstructions")
                                        .propObject("request", JsObjectBuilder.object()
                                                .propRaw("requestId", "requestId")
                                                .propString("requester", "RECRUITMENT_CV_CLASSIFIER")
                                                .propRaw("instructions", "instructions")))
                                .itemObject(JsCommon.typedEvent(
                                        blue.language.samples.paynote.types.domain.RecruitmentEvents.CvClassificationRequested.class,
                                        event -> event
                                                .propRaw("cvSessionId", "cvSessionId")
                                                .propRaw("requestId", "requestId"))))));
    }

    private static JsProgram applyProviderResponseProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("response", "event.update ?? {}")
                .constVar("requestId", "response.inResponseTo?.requestId")
                .ifBlock("!requestId", block -> block.returnOutput(
                        JsOutputBuilder.output().changesetRaw("[]").emptyEvents()))
                .constVar("pending", "document('/pendingClassification/' + requestId) ?? {}")
                .constVar("cvSessionId", "pending.cvSessionId")
                .ifBlock("!cvSessionId", block -> block.returnOutput(
                        JsOutputBuilder.output().changesetRaw("[]").emptyEvents()))
                .constVar("result", "response.result ?? {}")
                .constVar("candidateName", JsCommon.coalesce("result.candidateName", "'Unknown candidate'"))
                .safeNumber("yearsExperience", "result.yearsExperience", "0")
                .constVar("seniority", "String(result.seniority ?? 'unknown').toLowerCase()")
                .constVar("isSenior", "result.isSenior === true || seniority === 'senior' || yearsExperience >= 5")
                .constVar("alreadyAlerted", "document('/alertedSeniorCv/' + cvSessionId) === true")
                .constVar("shouldAlert", "isSenior && !alreadyAlerted")
                .constVar("classificationByCv", "{ ...document('/classificationByCv'), [cvSessionId]: { candidateName, yearsExperience, seniority, isSenior } }")
                .constVar("pendingClassification", "document('/pendingClassification') ?? {}")
                .line("delete pendingClassification[requestId];")
                .constVar("changeset", JsPatchBuilder.patch()
                        .replaceValue("/classificationByCv", "classificationByCv")
                        .replaceValue("/pendingClassification", "pendingClassification")
                        .replaceValue("/alertedSeniorCv", "shouldAlert ? { ...document('/alertedSeniorCv'), [cvSessionId]: true } : document('/alertedSeniorCv')")
                        .build())
                .constVar("events", "shouldAlert ? [" + JsCommon.typedEvent(
                        blue.language.samples.paynote.types.domain.RecruitmentEvents.SeniorCandidateDetected.class,
                        event -> event
                                .propRaw("cvSessionId", "cvSessionId")
                                .propRaw("candidateName", "candidateName")).build() + "] : []")
                .line("events.push({ type: '" + TypeAliases.CONVERSATION_CHAT_MESSAGE + "', message: 'CV classified: ' + candidateName + ' (' + seniority + ').' });")
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw("changeset")
                        .eventsRaw("events")));
    }
}
