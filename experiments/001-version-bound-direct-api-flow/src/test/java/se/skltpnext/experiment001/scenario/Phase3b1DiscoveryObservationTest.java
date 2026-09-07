package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JWSObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Characterizes a DIS-002 implementation blocker; does not mark DIS-002 as passing. */
class Phase3b1DiscoveryObservationTest {
    @TempDir
    Path runtime;

    @Test
    @Timeout(90)
    void signedSecondRevisionStopsExistingFlowBeforeTokenOrProducer() throws Exception {
        CryptoMaterial.generate(runtime);
        TlsMaterial.generate(runtime);
        try (var ignored = new RuntimeEnvironment(runtime, "phase3b1-observation").start()) {
            var engine = new ScenarioEngine(runtime, "phase3b1-observation");
            assertTrue(engine.ready());
            assertTrue(engine.run("E001-FLOW-001", "baseline").passed(),
                    "positive control must exercise the existing token and producer listeners");

            // Replace the only file the current reader supports, keeping the trusted signer.
            // This is a diagnostic injection, not a revision activation mechanism.
            Path serviceFile = runtime.resolve("metadata/service-rev-1.jws.json");
            var original = JWSObjectJSON.parse(Files.readString(serviceFile));
            ObjectNode payload = (ObjectNode) JsonSupport.MAPPER.readTree(original.getPayload().toString());
            payload.put("revision", 2);
            ObjectNode endpoint = (ObjectNode) payload.required("entries").get(0);
            endpoint.put("endpointId", "PRODUCER-ENDPOINT-REV-2");
            endpoint.put("endpointRevision", 2);
            endpoint.put("endpointUri", "https://localhost:1");
            var replacement = new JWSObjectJSON(new Payload(JsonSupport.compact(payload)));
            var key = CryptoMaterial.load(runtime).metadataKey();
            replacement.sign(original.getSignatures().getFirst().getHeader(), new ECDSASigner(key));
            String serialized = replacement.serializeFlattened();
            assertTrue(JWSObjectJSON.parse(serialized).getSignatures().getFirst()
                    .verify(new ECDSAVerifier(key.toPublicJWK())));
            Files.writeString(serviceFile, serialized);

            var rejection = assertThrows(IllegalArgumentException.class,
                    () -> new MetadataStores(runtime).discover());
            assertEquals("Metadata family/context/revision mismatch", rejection.getMessage());

            // Use the actual existing orchestration, not a test-authored discovery/token chain.
            // run() removes the positive control's events for this same scenario/variant.
            var result = engine.run("E001-FLOW-001", "baseline");
            assertEquals("inconclusive", result.status());
            assertEquals("harness-error", result.actual());
            for (String channel : List.of("network/payload-call-ledger.jsonl",
                    "telemetry/dependencies.jsonl", "telemetry/decisions.jsonl",
                    "contract/validations.jsonl", "audit/records.jsonl")) {
                Path path = runtime.resolve("events").resolve(channel);
                if (Files.exists(path)) {
                    assertTrue(Files.readAllLines(path).stream().allMatch(String::isBlank), channel);
                }
            }
            var errors = Files.readAllLines(runtime.resolve("events/errors/harness.jsonl"));
            assertEquals(1, errors.size());
            assertTrue(JsonSupport.MAPPER.readTree(errors.getFirst()).required("failureLocation")
                    .textValue().startsWith(MetadataStores.class.getName() + "#readAndValidate:"));
        }
    }
}
