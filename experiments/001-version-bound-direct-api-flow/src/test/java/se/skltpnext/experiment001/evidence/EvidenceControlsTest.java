package se.skltpnext.experiment001.evidence;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceControlsTest {
    @Test
    void scenarioSchemaRejectsAnUnknownResultShape() {
        ObjectNode invalid = JsonSupport.MAPPER.createObjectNode();
        invalid.put("schemaVersion", "1.0.0");
        invalid.put("unexpected", "must-not-be-accepted");

        assertThrows(IllegalArgumentException.class, () -> JsonSupport.validate(
                JsonSupport.readResource("experiment-001/schemas/scenario-result.schema.json"),
                invalid, "negative scenario-result fixture"));
    }

    @Test
    void leakageScannerDetectsCanaryValuesAndForbiddenJsonFields() {
        String canary = "E001-CANARY-TEST-DO-NOT-EMIT";

        assertEquals(1, EvidenceCollector.leakageHits(
                "{\"safe\":\"" + canary + "\"}", List.of(canary), true));
        assertEquals(1, EvidenceCollector.leakageHits(
                "{\"access_token\":\"redacted\"}", List.of(canary), true));
        assertEquals(0, EvidenceCollector.leakageHits(
                "{\"result\":\"pass\"}", List.of(canary), true));
    }
}
