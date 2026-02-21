package blue.language.samples.paynote.types.paynote;

import blue.language.model.BlueDescription;
import blue.language.model.BlueName;
import blue.language.model.Node;
import blue.language.model.TypeBlueId;

public final class PayNoteTypes {

    private PayNoteTypes() {
    }

    @TypeBlueId("FzJqLm7Ek58LsVstJV2c37JfiMhRiWtjZNsiMz2ZfCYG")
    public static class CaptureDeclined {
        public String reason;
    }

    @TypeBlueId("FUh3TARSh4TjnWKAkM5ydjFWLWEmrFByKMBQzcgQfqRW")
    public static class CaptureFailed {
        public String reason;
    }

    @TypeBlueId("DvxKVEFsDmgA1hcBDfh7t42NgTRLaxXjCrB48DufP3i3")
    public static class CaptureFundsRequested {
        @BlueName("amount")
        public String amountName;
        @BlueDescription("amount")
        public String amountDescription;
        public Integer amount;
    }

    @TypeBlueId("DFKVw43E36kimqj64FyiiVxE9yNuB22SETFx5M4WAi9m")
    public static class ChildPayNoteIssuanceDeclined {
        public String reason;
    }

    @TypeBlueId("FAZCx2s5eq9zPV64LdHNFYbjjxD3ci1ZqyTcQk5WhXAs")
    public static class ChildPayNoteIssued {
        public Node childPayNote;
    }

    @TypeBlueId("BJvjorbC5ed5KTV7SxoV3CvrJXjrFPcFxY9QT4jHBbXi")
    public static class FundsCaptured {
        public Integer amountCaptured;
    }

    @TypeBlueId("AopfdGqnwcxsw4mJzXbmjDMnASRtkce9BZB1n6QSRNXX")
    public static class FundsReserved {
        public Integer amountReserved;
    }

    @TypeBlueId("53Dir2sGy1NHuCQXF6suGoDMxYacNhbcy23AKD89SghD")
    public static class IssueChildPayNoteRequested {
        public Node childPayNote;
    }

    @TypeBlueId("BVLMcTzag3D2rvg8LoKQ3Htgoqsh77EAoiTLTxH5aVBE")
    public static class PayeeAssignmentRequested {
        public String payeeEmail;
    }

    @TypeBlueId("HQTUxErobqhSuhWo9DAC1WwaG9oYdjfmdKprGtV4TeEK")
    public static class PayNoteApproved {
    }

    @TypeBlueId("GaYDPA7TTqWuoxioCYFPeyqomjH4g3YDtFxHv9yLRQ8A")
    public static class PayNoteCancellationRejected {
        public String reason;
    }

    @TypeBlueId("DqiwzsNLbHCh6PaDF6wy6ZqBSF5JV5nAQSKFKTPRTbGB")
    public static class PayNoteCancellationRequested {
        public Node childPayNote;
    }

    @TypeBlueId("96buyUXwhkak8xKoCR5nAW9tMuwzkevJFdELVvwKxR6Y")
    public static class PayNoteCancelled {
    }

    @TypeBlueId("AdKfkwRfzRUxUKSzhRfYANsaUBNnz4u6JFWR66qhzyZe")
    public static class PayNoteRejected {
        public String reason;
    }

    @TypeBlueId("4xS8bmZQBGPENmaPfsrtYguYfq4hTtaZAXrefdyFNkKq")
    public static class ReservationDeclined {
        public String reason;
    }

    @TypeBlueId("653sCbbRH3RiKhGjmVxh6wFVs4rn54wJRKDXRMKBZtjA")
    public static class ReservationReleaseDeclined {
        public String reason;
    }

    @TypeBlueId("GU8nkSnUuMs6632rHQyBndRtjDcMB9ZSbgwkGYcfGt97")
    public static class ReservationReleaseRequested {
        public Integer amount;
    }

    @TypeBlueId("CFqiZigjKE5JatANkaAkWw2NbgvEmb2BVEVPf3ckUrWg")
    public static class ReservationReleased {
        public Integer amountReleased;
    }

    @TypeBlueId("3XstDYFkqsUP5PdM6Z6mwspPzgdQMFtUpNyMsKPK2o6N")
    public static class ReserveFundsAndCaptureImmediatelyRequested {
        public Integer amount;
    }

    @TypeBlueId("3Y3TYmSfZMmPYKmF5i3eR8YcVPNP5Sic2bZN8xRnvMWm")
    public static class ReserveFundsRequested {
        public Integer amount;
    }

    @TypeBlueId("3b3ePGPg5GzS6KYfqoDfgjWbjccXVnGzytbpFS53x4HM")
    public static class SettlementAmountRejected {
        public String reason;
    }

    @TypeBlueId("4pVAdZo93FHRRkAkshqCZW4pUvvV1ccczJZ2Lu4jkD1D")
    public static class SettlementAmountSpecified {
        public Integer finalAmount;
    }
}
