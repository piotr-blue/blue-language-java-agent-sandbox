package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayNoteEventsTest {

    @Test
    void buildsReserveCaptureReleaseEventsWithConsistentShapes() {
        Node reserve = PayNoteEvents.reserveFundsRequested(1000);
        Node capture = PayNoteEvents.captureFundsRequested(700);
        Node release = PayNoteEvents.reservationReleaseRequested(300);

        assertEquals(PayNoteAliases.RESERVE_FUNDS_REQUESTED, reserve.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CAPTURE_FUNDS_REQUESTED, capture.getAsText("/type/value"));
        assertEquals(PayNoteAliases.RESERVATION_RELEASE_REQUESTED, release.getAsText("/type/value"));
        assertEquals("3Y3TYmSfZMmPYKmF5i3eR8YcVPNP5Sic2bZN8xRnvMWm", reserve.getAsText("/type/blueId"));
        assertEquals("DvxKVEFsDmgA1hcBDfh7t42NgTRLaxXjCrB48DufP3i3", capture.getAsText("/type/blueId"));
        assertEquals(1000, reserve.getAsInteger("/amount/value").intValue());
        assertEquals(700, capture.getAsInteger("/amount/value").intValue());
        assertEquals(300, release.getAsInteger("/amount/value").intValue());
    }

    @Test
    void buildsCardTransactionLockAndUnlockEvents() {
        Node lockRequested = PayNoteEvents.cardTransactionCaptureLockRequested()
                .cardTransactionDetails(details -> details
                        .put("authorizationId", "AUTH_123")
                        .put("merchantId", "MERCHANT_456")
                        .put("captureAmountMinor", 80000))
                .build();

        Node unlockRequested = PayNoteEvents.cardTransactionCaptureUnlockRequested()
                .cardTransactionDetails(details -> details
                        .put("authorizationId", "AUTH_123")
                        .put("merchantId", "MERCHANT_456")
                        .put("captureAmountMinor", 80000))
                .build();

        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_LOCK_REQUESTED, lockRequested.getAsText("/type/value"));
        assertEquals(PayNoteAliases.CARD_TRANSACTION_CAPTURE_UNLOCK_REQUESTED, unlockRequested.getAsText("/type/value"));
        assertEquals("PayNote-Card-Capture-Lock-Requested-Demo-BlueId", lockRequested.getAsText("/type/blueId"));
        assertEquals("PayNote-Card-Capture-Unlock-Requested-Demo-BlueId", unlockRequested.getAsText("/type/blueId"));
        assertEquals("AUTH_123", lockRequested.getAsText("/cardTransactionDetails/authorizationId/value"));
        assertEquals(80000, unlockRequested.getAsInteger("/cardTransactionDetails/captureAmountMinor/value").intValue());
    }
}
