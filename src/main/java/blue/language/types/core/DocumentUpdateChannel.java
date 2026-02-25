package blue.language.types.core;

import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("Core/Document Update Channel")
@TypeBlueId("6H1iGrDAcqtFE1qv3iyMTj79jCZsMUMxsNUzqYSJNbyR")
public class DocumentUpdateChannel {
    public String path;

    public DocumentUpdateChannel path(String path) {
        this.path = path;
        return this;
    }
}
