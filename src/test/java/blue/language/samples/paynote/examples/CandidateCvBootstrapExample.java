package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.JsProgram;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.TypeAliases;

public final class CandidateCvBootstrapExample {

    private CandidateCvBootstrapExample() {
    }

    public static Node build(String timestamp,
                             String recruitmentSessionId,
                             String currentAccountId) {
        return MyOsDsl.bootstrap()
                .documentName("Candidate CV B - " + timestamp)
                .putDocumentValue("processingStatus", "pending")
                .putDocumentObject("cv", cv -> cv
                        .put("name", "Mia Zielinska")
                        .put("experience", "Built quality assurance coverage for web products over 2 years using Cypress, Playwright, Postman, and SQL checks. Improved regression stability, prepared test plans, and worked closely with developers during release cycles."))
                .contracts(c -> {
                    c.timelineChannel("ownerChannel");
                    c.documentLinks("links", links -> links
                            .sessionLink("recruitmentLink", "cvs", recruitmentSessionId));

                    c.operation("updateCv", "ownerChannel", "Update CV content deterministically.");

                    c.operation("changeProcessingStatus",
                            "ownerChannel",
                            "Change CV processing status.",
                            request -> request.putNode("status", new Node().type(TypeAliases.TEXT)));

                    c.implementOperation("changeProcessingStatusImpl", "changeProcessingStatus", steps -> steps
                            .js("ApplyStatus", applyStatusProgram())
                            .updateDocumentFromExpression("PersistStatus", "steps.ApplyStatus.changeset"));

                    c.implementOperation("updateCvImpl", "updateCv", steps -> steps
                            .js("ApplyCvUpdate", applyCvUpdateProgram())
                            .updateDocumentFromExpression("PersistCvUpdate", "steps.ApplyCvUpdate.changeset"));
                })
                .bindAccount("ownerChannel", currentAccountId)
                .build();
    }

    private static JsProgram applyStatusProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("request", "event.message?.request ?? {}")
                .constVar("status", "request.status ?? document('/processingStatus') ?? 'pending'")
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw(JsPatchBuilder.patch()
                                .replaceValue("/processingStatus", "status")
                                .build())));
    }

    private static JsProgram applyCvUpdateProgram() {
        return BlueDocDsl.js(js -> js
                .constVar("request", "event.message?.request ?? {}")
                .constVar("changeset", JsPatchBuilder.patch()
                        .replaceValue("/cv/name", "request.candidateName ?? document('/cv/name') ?? ''")
                        .replaceValue("/cv/experience", "request.experience ?? document('/cv/experience') ?? ''")
                        .replaceValue("/processingStatus", "'pending'")
                        .build())
                .returnOutput(JsOutputBuilder.output()
                        .changesetRaw("changeset")
                        .eventsArray(JsArrayBuilder.array()
                                .itemObject(JsObjectBuilder.object()
                                        .propString("type", TypeAliases.CONVERSATION_EVENT)
                                        .propString("message", "CV updated")))));
    }
}
