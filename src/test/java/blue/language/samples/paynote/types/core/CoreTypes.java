package blue.language.samples.paynote.types.core;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;

import java.util.List;
import java.util.Map;

public final class CoreTypes {

    private CoreTypes() {
    }

    @TypeBlueId("DcoJyCh7XXxy1nR5xjy7qfkUgQ1GiZnKKSxh8DJusBSr")
    public static class Channel {
        public Node event;
    }

    @TypeBlueId("B7YQeYdQzUNuzaDQ4tNTd2iJqgd4YnVQkgz4QgymDWWU")
    public static class ChannelEventCheckpoint {
        public Map<String, Node> lastEvents;
    }

    @TypeBlueId("AERp8BWnuUsjoPciAeNXuUWS9fmqPNMdWbxmKn3tcitx")
    public static class Contract {
        public Integer order;
    }

    @TypeBlueId("BrpmpNt5JkapeUvPqYcxgXZrHNZX3R757dRwuXXdfNM2")
    public static class DocumentProcessingInitiated {
        public String documentId;
    }

    @TypeBlueId("5AJiAUgiSDwfCzv9rCYKNaAJu1hm8BXFu7eLNAEHNACr")
    public static class DocumentProcessingTerminated {
        public String cause;
        public String reason;
    }

    @TypeBlueId("7htwgHAXA9FjUGRytXFfwYMUZz4R3BDMfmeHeGvpscLP")
    public static class DocumentUpdate {
        public String op;
        public String path;
        public Node before;
        public Node after;
    }

    @TypeBlueId("6H1iGrDAcqtFE1qv3iyMTj79jCZsMUMxsNUzqYSJNbyR")
    public static class DocumentUpdateChannel {
        public String path;
    }

    @TypeBlueId("Fjbu3QpnUaTruDTcTidETCX2N5STyv7KYxT42PCzGHxm")
    public static class EmbeddedNodeChannel {
        public String childPath;
    }

    @TypeBlueId("9ZE5pGjtSGJgWJG7iAVz4iPEz5CatceX3yb3qp5MpAKJ")
    public static class Handler {
        public String channel;
        public Node event;
    }

    @TypeBlueId("Bz49DbfqKC1yJeCfv5RYPZUKTfb7rtZnmreCaz4RsXn5")
    public static class JsonPatchEntry {
        public String op;
        public String path;
        public Node val;
    }

    @TypeBlueId("H2aCCTUcLMTJozWkn7HPUjyFBFxamraw1q8DyWk87zxr")
    public static class LifecycleEventChannel {
    }

    @TypeBlueId("7QACj919YMRvFCTELCf6jfQTp41RVhtHdE6bPazLUZQ6")
    public static class Marker {
    }

    @TypeBlueId("Hu4XkfvyXLSdfFNUwuXebEu3oJeWcMyhBTcRV9AQyKPC")
    public static class ProcessEmbedded {
        public List<String> paths;
    }

    @TypeBlueId("EVguxFmq5iFtMZaBQgHfjWDojaoesQ1vEXCQFZ59yL28")
    public static class ProcessingInitializedMarker {
        public String documentId;
    }

    @TypeBlueId("5NiEhupJ6uF54Q3vs4GwQX4UX4ExtwHpKRVvjKEHtvjR")
    public static class ProcessingTerminatedMarker {
        public String cause;
        public String reason;
    }

    @TypeBlueId("C77W4kVGcxL7Mkx9WL9QESPEFFL2GzWAe647s1Efprt")
    public static class TriggeredEventChannel {
    }
}
