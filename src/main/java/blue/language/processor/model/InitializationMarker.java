package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId({"InitializationMarker", "Core/Processing Initialized Marker"})
public class InitializationMarker extends MarkerContract {

    private String documentId;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
