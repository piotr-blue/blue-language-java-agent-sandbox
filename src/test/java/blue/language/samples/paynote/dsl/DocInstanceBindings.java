package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

public final class DocInstanceBindings {

    private final DocSpecializer delegate = new DocSpecializer();

    public DocInstanceBindings role(String role, String channelKey) {
        delegate.role(role, channelKey);
        return this;
    }

    public DocSpecializer.ChannelBindingEditor bindRole(String role) {
        return delegate.bindRole(role);
    }

    public DocSpecializer.ChannelBindingEditor bindChannel(String channelKey) {
        return delegate.bindChannel(channelKey);
    }

    Node applyBindings(Node bootstrap) {
        return delegate.applyBindings(bootstrap);
    }
}
