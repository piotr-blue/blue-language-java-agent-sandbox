package blue.language.samples.paynote.types.domain;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;

public final class RecruitmentEvents {

    private RecruitmentEvents() {
    }

    @TypeAlias("Recruitment/CV Classification Requested")
    @TypeBlueId("Recruitment-CV-Classification-Requested-Demo-BlueId")
    public static class CvClassificationRequested {
        public String cvSessionId;
        public String requestId;
    }

    @TypeAlias("Recruitment/Senior Candidate Detected")
    @TypeBlueId("Recruitment-Senior-Candidate-Detected-Demo-BlueId")
    public static class SeniorCandidateDetected {
        public String cvSessionId;
        public String candidateName;
    }
}
