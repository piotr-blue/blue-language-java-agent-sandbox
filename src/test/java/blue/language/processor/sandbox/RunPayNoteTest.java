package blue.language.processor.sandbox;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunPayNoteTest {

    @Test
    void runsKnownTicketAndPrintsSummary() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputBuffer));
        try {
            RunPayNote.main(new String[]{"--ticket", "1"});
        } finally {
            System.setOut(originalOut);
        }

        String output = new String(outputBuffer.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.contains("ticket=ticket01_satisfactionUnlockCapture"));
        assertTrue(output.contains("initialized=true"));
        assertTrue(output.contains("emittedCount="));
    }

    @Test
    void failsForUnknownTicketSelector() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RunPayNote.main(new String[]{"--ticket", "unknown-ticket"}));
        assertTrue(String.valueOf(exception.getMessage()).contains("Unknown ticket selector"));
    }
}
