package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

public final class MyOs {

    private MyOs() {
    }

    public static MyOsBootstrapBuilder bootstrap(Node document) {
        return new MyOsBootstrapBuilder(document);
    }
}
