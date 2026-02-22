package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class StepsBuilder {

    private final List<Node> steps = new ArrayList<Node>();

    public StepsBuilder js(String name, JsProgram program) {
        Node step = new Node().type(TypeAliases.CONVERSATION_JAVASCRIPT_CODE);
        if (name != null) {
            step.name(name);
        }
        step.properties("code", new Node().value(program.code()));
        steps.add(step);
        return this;
    }

    public StepsBuilder updateDocument(String name, Consumer<ChangesetBuilder> customizer) {
        ChangesetBuilder changesetBuilder = new ChangesetBuilder();
        customizer.accept(changesetBuilder);

        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().items(changesetBuilder.build()));
        steps.add(step);
        return this;
    }

    public StepsBuilder updateDocumentFromExpression(String name, String expression) {
        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().value(BlueDocDsl.expr(expression)));
        steps.add(step);
        return this;
    }

    public StepsBuilder triggerEvent(String name, Node event) {
        Node step = new Node().type(TypeAliases.CONVERSATION_TRIGGER_EVENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("event", event);
        steps.add(step);
        return this;
    }

    public StepsBuilder raw(Node step) {
        steps.add(step);
        return this;
    }

    List<Node> build() {
        return steps;
    }
}
