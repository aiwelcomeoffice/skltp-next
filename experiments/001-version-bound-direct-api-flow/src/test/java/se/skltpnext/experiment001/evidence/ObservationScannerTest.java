package se.skltpnext.experiment001.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ObservationScannerTest {
    @TempDir Path root;
    @Test void recognizesMaterializedRepresentationsAndNeverReportsTheirValues() throws Exception {
        Path registry = root.resolve("canaries.jsonl"), channel = root.resolve("console.bin");
        String marker = "E001-Synthetic /+Claim-Only";
        JsonSupport.appendJsonLine(registry, Map.of("canaryId", "CANARY-TEST", "type", "sensitive_claim", "value", marker));
        // Independent fixtures exercise the scanner, including raw non-JSON console bytes.
        for (String encoded : List.of(marker, marker.toLowerCase(Locale.ROOT), marker.toUpperCase(Locale.ROOT),
                "E001-Synthetic+%2F%2BClaim-Only", Base64.getEncoder().encodeToString(marker.getBytes(StandardCharsets.UTF_8)),
                Base64.getUrlEncoder().withoutPadding().encodeToString(marker.getBytes(StandardCharsets.UTF_8)))) {
            Files.writeString(channel, "before\n" + encoded + "\nafter");
            var findings = ObservationScanner.scan(channel, registry);
            assertTrue(ObservationScanner.hits(findings) > 0);
            assertFalse(JsonSupport.compact(findings).contains(encoded));
        }
        Files.writeString(channel, "safe synthetic reference");
        assertEquals(0, ObservationScanner.hits(ObservationScanner.scan(channel, registry)));
    }
    @Test void detectsForbiddenKeysInEveryChannelAndRejectsMissingBytes() throws Exception {
        Path registry = root.resolve("canaries.jsonl"), channel = root.resolve("console.log");
        Files.writeString(registry, "");
        for (String forbidden : List.of("{\"ACCESS_TOKEN\" : \"redacted\"}", "Authorization: synthetic", "private_key=value",
                "%22client_assertion%22%3A", "ImRwb3BfcHJvb2YiOg==")) {
            Files.writeString(channel, forbidden);
            assertTrue(ObservationScanner.hits(ObservationScanner.scan(channel, registry)) > 0, "forbidden field fixture");
        }
        Files.delete(channel);
        assertThrows(NoSuchFileException.class, () -> ObservationScanner.scan(channel, registry));
    }
    @Test void separateWritersRejectCanariesAndAuditSchemaRejectsTraceIdentity() throws Exception {
        var context = Map.of("scenarioId", "E001-OBS-002", "sourceScenarioId", "E001-AUTHN-001",
                "sourceVariantId", "bad-signature", "stimulusRef", "STIMULUS-00000000-0000-0000-0000-000000000000");
        JsonSupport.writeJson(root.resolve("private/observation-context.json"), context);
        String secret = new CanaryRegistry(root.resolve("private")).newValue("sensitive_claim");
        try (var writer = new se.skltpnext.experiment001.telemetry.TelemetryRecorder(root, "writer-test", "E001-AUTHN-001", "bad-signature", "authorization-server")) {
            assertThrows(IllegalArgumentException.class, () -> writer.decision("authorization-server.client-authentication", "client_authentication", "deny", secret));
            assertThrows(IllegalArgumentException.class, () -> writer.dependency(secret, "error", 1));
        }
        assertFalse(Files.exists(root.resolve("events/telemetry/decisions.jsonl")));
        var row = se.skltpnext.experiment001.telemetry.ObservationContext.common(JsonSupport.MAPPER.valueToTree(context),
                "writer-test", "authorization-server", "authorization-server.client-authentication", "client_authentication", "deny", secret, "1.0.0");
        assertThrows(IllegalArgumentException.class, () -> new se.skltpnext.experiment001.telemetry.AuditRecorder().write(root, row, "DECISION-00000000-0000-0000-0000-000000000000"));
        assertFalse(Files.exists(root.resolve("events/audit/observations.jsonl")));
        row.put("reason", "invalid-client-or-proof"); row.put("traceId", "11111111111111111111111111111111");
        assertThrows(IllegalArgumentException.class, () -> new se.skltpnext.experiment001.telemetry.AuditRecorder().write(root, row, "DECISION-00000000-0000-0000-0000-000000000000"));
    }

}
