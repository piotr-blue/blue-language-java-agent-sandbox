package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsTemplateResolverTest {

    @Test
    void resolvesKnownAliasTokens() {
        String resolved = JsTemplateResolver.resolveDefaults(
                "return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}' }] };");

        assertTrue(resolved.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
    }

    @Test
    void failsFastForUnknownTokens() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsTemplateResolver.resolveDefaults("const x = '{{DOES_NOT_EXIST}}';"));
        assertEquals("Unknown JS template token: DOES_NOT_EXIST", ex.getMessage());
    }

    @Test
    void stepsBuilderJsTemplateProducesResolvedCode() {
        StepsBuilder steps = new StepsBuilder();
        steps.jsTemplate("EmitCapture", "return { events: [{ type: '{{CAPTURE_FUNDS_REQUESTED}}' }] };");

        Node root = new Node().items(steps.build());
        String code = root.getAsText("/0/code/value");
        assertTrue(code.contains(PayNoteAliases.CAPTURE_FUNDS_REQUESTED));
    }

    @Test
    void jsProgramLinesTemplateResolvesTypeAliases() {
        JsProgram program = BlueDocDsl.js(js -> js
                .linesTemplate(
                        "const eventType = '{{MYOS_CALL_OPERATION_REQUESTED}}';",
                        "return { events: [{ type: eventType }] };"));

        assertTrue(program.code().contains(TypeAliases.MYOS_CALL_OPERATION_REQUESTED));
    }
}
