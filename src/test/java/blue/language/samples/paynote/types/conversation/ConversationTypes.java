package blue.language.samples.paynote.types.conversation;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.types.core.CoreTypes;

import java.util.List;

public final class ConversationTypes {

    private ConversationTypes() {
    }

    @TypeBlueId("CN5efWVizJbRsMCw8YWRfT2q9vM9XfeGDN9wFvYcvMnQ")
    public static class Actor {
    }

    @TypeBlueId("AkUKoKY1hHY1CytCrAXDPKCd4md1QGmn1WNcQtWBsyAD")
    public static class ChatMessage {
        public String message;
    }

    @TypeBlueId("HsNatiPt2YvmkWQoqtfrFCbdp75ZUBLBUkWeq84WTfnr")
    public static class CompositeTimelineChannel extends TimelineChannel {
        public List<String> channels;
    }

    @TypeBlueId("58B8orsFkxxy7bWqjLXJmtBs2b5rwnNQNbeoAbGhPkJc")
    public static class DocumentStatus {
        public String mode;
    }

    @TypeBlueId("5Wz4G9qcnBJnntYRkz4dgLK5bSuoMpYJZj4j5M59z4we")
    public static class Event {
    }

    @TypeBlueId("8Akr9sdTkxBqMYWSGh8gHgoXQQeYEPfhV4s8fXeKTd9W")
    public static class InformUserAboutPendingAction {
        public String operation;
        public String title;
        public String message;
        public String channel;
        public Node expectedRequest;
    }

    @TypeBlueId("3hYcmWMtMUPAzXBLFLb7BpuG9537tuTJPCr7pxWXvTZK")
    public static class JavaScriptCode {
        public String code;
    }

    @TypeBlueId("5YZEAtUaHhNfnVnwtSsyBnBakpcSwdL75UcgtrtnqeYn")
    public static class LifecycleEvent {
    }

    @TypeBlueId("BoAiqVUZv9Fum3wFqaX2JnQMBHJLxJSo2V9U2UBmCfsC")
    public static class Operation {
        public Node request;
        public String channel;
    }

    @TypeBlueId("HM4Ku4LFcjC5MxnhPMRwQ8w3BbHmJKKZfHTTzsd4jbJq")
    public static class OperationRequest {
        public String operation;
        public Node request;
        public Node document;
        public Boolean allowNewerVersion;
    }

    @TypeBlueId("8f9UhHMbRe62sFgzQVheToaJPYi7t7HPNVvpQTbqfL5n")
    public static class Request {
        public String requestId;
    }

    @TypeBlueId("36epvrpVHZLjapbeZsNodz2NDnm7XZeNZcnkWHgkP1pp")
    public static class Response {
        public Node inResponseTo;
    }

    @TypeBlueId("7X3LkN54Yp88JgZbppPhP6hM3Jqiqv8Z2i4kS7phXtQe")
    public static class SequentialWorkflow {
        public List<Node> steps;
    }

    @TypeBlueId("CGdxkNjPcsdescqLPz6SNLsMyak6demQQr7RoKNHbCyv")
    public static class SequentialWorkflowOperation {
        public String operation;
    }

    @TypeBlueId("HYsLiqsqk7t98d5fK6YxKEQGeqBaHNVjM4rRNdxK4yUW")
    public static class SequentialWorkflowStep {
    }

    @TypeBlueId("C48nKSkbxraMSv4gCiGYVHKFjmtK12k63Yasn95zknWE")
    public static class StatusChange {
        public String status;
    }

    @TypeBlueId("EhPFqrRCreg7StsZEcW8fRQ1FQVdqYSsMSis2CHHJZ1G")
    public static class StatusCompleted {
        public String mode;
    }

    @TypeBlueId("Guus3kHbivXvy5G93yhiKs3Pc8sxCc4XVvSo7CqLsQEc")
    public static class StatusFailed {
        public String mode;
    }

    @TypeBlueId("56Lhu3Z2oF3kuYG41eZAK8TvgVtRNevkbRi4D31kKWZm")
    public static class StatusInProgress {
        public String mode;
    }

    @TypeBlueId("ETAPjwZVyYEfPUWvXkCWAuybBhYUayg4hKC2V7mLXmsv")
    public static class StatusPending {
        public String mode;
    }

    @TypeBlueId("3Ge54FsGJaaeZmm8nTKJdS6HVdYhiR3g18fDwg3Ev1Sa")
    public static class Timeline {
        public String timelineId;
    }

    @TypeBlueId("EvuCWsG1E6WJQg8QXmk6rwMANYTZjoLWVZ1vYQWUwdTH")
    public static class TimelineChannel extends CoreTypes.Channel {
        public String timelineId;
    }

    @TypeBlueId("EzDiC9Frs8V5yQBMtDYh1DobVnWtWpFemXDX5fGUULBn")
    public static class TimelineEntry {
        public Node timeline;
        public Node prevEntry;
        public Node message;
        public Long timestamp;
        public Node actor;
    }

    @TypeBlueId("GxUtWr3eH9a6YQeioQkujEnsPjD5s9RU8ZhEfmsV1vuU")
    public static class TriggerEvent {
        public Node event;
    }

    @TypeBlueId("FtHZJzH4hqAoGxFBjsmy1svfT4BwEBB4aHpFSZycZLLa")
    public static class UpdateDocument {
        public List<Node> changeset;
    }
}
