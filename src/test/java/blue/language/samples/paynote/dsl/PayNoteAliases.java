package blue.language.samples.paynote.dsl;

public final class PayNoteAliases {

    private PayNoteAliases() {
    }

    // Document types
    public static final String PAYNOTE = "PayNote/PayNote";
    public static final String CARD_TRANSACTION_PAYNOTE = "PayNote/Card Transaction PayNote";
    public static final String SHIPMENT_CAPTURE_PAYNOTE = "PayNote/Shipment Capture PayNote";

    // Generic PayNote events
    public static final String RESERVE_FUNDS_REQUESTED = "PayNote/Reserve Funds Requested";
    public static final String CAPTURE_FUNDS_REQUESTED = "PayNote/Capture Funds Requested";
    public static final String RESERVATION_RELEASE_REQUESTED = "PayNote/Reservation Release Requested";
    public static final String RESERVE_FUNDS_AND_CAPTURE_IMMEDIATELY_REQUESTED =
            "PayNote/Reserve Funds and Capture Immediately Requested";
    public static final String ISSUE_CHILD_PAYNOTE_REQUESTED = "PayNote/Issue Child PayNote Requested";
    public static final String PAYNOTE_CANCELLATION_REQUESTED = "PayNote/PayNote Cancellation Requested";

    // Card transaction escrow flow events
    public static final String CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED =
            "PayNote/Card Transaction Capture Lock Requested";
    public static final String CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED =
            "PayNote/Card Transaction Capture Unlock Requested";
    public static final String CARD_TRANSACTION_CAPTURE_LOCKED =
            "PayNote/Card Transaction Capture Locked";
    public static final String CARD_TRANSACTION_CAPTURE_UNLOCKED =
            "PayNote/Card Transaction Capture Unlocked";
}
