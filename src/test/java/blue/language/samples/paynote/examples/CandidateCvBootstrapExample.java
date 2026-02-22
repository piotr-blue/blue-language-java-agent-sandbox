package blue.language.samples.paynote.examples;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsProgram;

public final class CandidateCvBootstrapExample {

    private CandidateCvBootstrapExample() {
    }

    public static Node build(String timestamp,
                             String recruitmentSessionId,
                             String currentAccountId) {
        return BlueDocDsl.documentSessionBootstrap()
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
                            request -> request.putNode("status", new Node().type("Text")));

                    c.sequentialWorkflowOperation("changeProcessingStatusImpl", "changeProcessingStatus", steps -> steps
                            .js("ApplyStatus", applyStatusProgram())
                            .updateDocumentFromExpression("PersistStatus", "steps.ApplyStatus.changeset"));

                    c.sequentialWorkflowOperation("updateCvImpl", "updateCv", steps -> steps
                            .js("ApplyCvUpdate", applyCvUpdateProgram())
                            .updateDocumentFromExpression("PersistCvUpdate", "steps.ApplyCvUpdate.changeset"));
                })
                .bindAccount("ownerChannel", currentAccountId)
                .build();
    }

    private static JsProgram applyStatusProgram() {
        return BlueDocDsl.js(js -> js
                .line("const request = event.message?.request ?? {};")
                .line("const status = request.status ?? document('/processingStatus') ?? 'pending';")
                .returnObject(JsObjectBuilder.object()
                        .propArrayRaw("changeset", "[{ op: 'replace', path: '/processingStatus', val: status }]")));
    }

    private static JsProgram applyCvUpdateProgram() {
        return BlueDocDsl.js(js -> js
                .line("const request = event.message?.request ?? {};")
                .line("const changeset = [")
                .line("  { op: 'replace', path: '/cv/name', val: request.candidateName ?? document('/cv/name') ?? '' },")
                .line("  { op: 'replace', path: '/cv/experience', val: request.experience ?? document('/cv/experience') ?? '' },")
                .line("  { op: 'replace', path: '/processingStatus', val: 'pending' },")
                .line("];")
                .blank()
                .line("return {")
                .line("  changeset,")
                .line("  events: [")
                .line("    {")
                .line("      type: 'Conversation/Event',")
                .line("      message: 'CV updated',")
                .line("    },")
                .line("  ],")
                .line("};"));
    }
}
