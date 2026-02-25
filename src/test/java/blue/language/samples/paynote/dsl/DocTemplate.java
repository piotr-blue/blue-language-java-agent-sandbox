package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Deprecated(forRemoval = true)
public final class DocTemplate {

    private final Node bootstrap;

    private DocTemplate(Node bootstrap) {
        this.bootstrap = bootstrap.clone();
    }

    public static DocTemplate of(Node bootstrap) {
        return new DocTemplate(bootstrap);
    }

    public DocTemplate specialize(Consumer<DocSpecializer> customizer) {
        DocSpecializer specializer = new DocSpecializer();
        customizer.accept(specializer);

        List<JsonPatch> patches = new ArrayList<JsonPatch>(specializer.patches());
        Node patched = DocTemplates.applyPatch(bootstrap, patches);
        Node withBindings = specializer.applyBindings(patched);
        return new DocTemplate(withBindings);
    }

    public DocTemplate instantiate(Consumer<DocInstanceBindings> customizer) {
        DocInstanceBindings bindings = new DocInstanceBindings();
        customizer.accept(bindings);
        return new DocTemplate(bindings.applyBindings(bootstrap));
    }

    public Node build() {
        return bootstrap.clone();
    }
}
