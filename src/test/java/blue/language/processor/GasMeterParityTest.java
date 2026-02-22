package blue.language.processor;

import blue.language.model.Node;
import blue.language.processor.util.NodeCanonicalizer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GasMeterParityTest {

    @Test
    void chargesTriggerEventBaseAndUpdateDocumentBase() {
        GasMeter meter = new GasMeter();

        meter.chargeTriggerEventBase();
        meter.chargeUpdateDocumentBase(0);
        meter.chargeUpdateDocumentBase(3);

        assertEquals(30L + 40L + 55L, meter.totalGas());
    }

    @Test
    void chargesDocumentSnapshotByPointerDepthAndPayloadSize() {
        GasMeter meter = new GasMeter();
        Node snapshot = new Node().properties("profile", new Node().properties("name", new Node().value("Ada")));
        long bytes = NodeCanonicalizer.canonicalSize(snapshot);
        long sizeCharge = (bytes + 99L) / 100L;

        meter.chargeDocumentSnapshot("/", snapshot);
        meter.chargeDocumentSnapshot("/a/b", null);

        long expected = (8L + 0L + sizeCharge) + (8L + 2L + 0L);
        assertEquals(expected, meter.totalGas());
    }

    @Test
    void chargesWasmFuelUsingScheduleConversion() {
        GasMeter meter = new GasMeter();

        meter.chargeWasmGas(BigInteger.valueOf(1_701L));
        meter.chargeWasmGas(3_400L);

        assertEquals(2L + 2L, meter.totalGas());
    }
}
