package blue.language.samples.paynote.dsl;

import blue.language.model.Node;

public final class MyOsDsl {

    private MyOsDsl() {
    }

    public static DocumentBuilder bootstrap() {
        return BlueDocDsl.documentSessionBootstrap();
    }

    public static MyOsBootstrapBuilder bootstrap(Node document) {
        return new MyOsBootstrapBuilder(document);
    }

    public static MyOsEvents.SinglePermissionGrantRequestedBuilder requestSingleDocumentPermission() {
        return MyOsEvents.singlePermissionGrantRequested();
    }

    public static MyOsEvents.LinkedPermissionsGrantRequestedBuilder requestLinkedDocumentPermission() {
        return MyOsEvents.linkedPermissionsGrantRequested();
    }

    public static MyOsEvents.CallOperationRequestedBuilder callOperation() {
        return MyOsEvents.callOperationRequested();
    }

    public static MyOsEvents.SubscribeToSessionRequestedBuilder subscribeToSession() {
        return MyOsEvents.subscribeToSessionRequested();
    }
}
