package blue.language.types.core;

import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("Core/Document Processing Initiated")
@TypeBlueId("BrpmpNt5JkapeUvPqYcxgXZrHNZX3R757dRwuXXdfNM2")
public class DocumentProcessingInitiated {
    public String documentId;

    public DocumentProcessingInitiated documentId(String documentId) {
        this.documentId = documentId;
        return this;
    }
}
