package se.skltpnext.experiment001.scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.ExperimentConfig;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class PhaseThreeMetadataTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path root;
    @Test @Timeout(90)
    void missingAndAmbiguousEndpointsStopBeforeTokenAndProducer() { runFamily("E001-DIS-003"); }
    @Test @Timeout(90)
    void metadataIntegrityAuthorityAndRelations() { runFamily("E001-META-001"); }
    @Test @Timeout(90)
    void staleFamiliesFailClosedSeparately() { runFamily("E001-META-002"); }
    @Test @Timeout(90)
    void offboardingAndUnpublicationReachTheirOwnCheckpoints() { runFamily("E001-LIFE-001"); }
    private void runFamily(String scenario) {
        CryptoMaterial.generate(root); TlsMaterial.generate(root);
        try (var ignored = new RuntimeEnvironment(root, "phase3-targeted").start()) {
            var engine = new ScenarioEngine(root, "phase3-targeted");
            var variants = ExperimentConfig.PHASE_3_VARIANTS.stream().filter(k -> k.startsWith(scenario + "/")).sorted().toList();
            assertFalse(variants.isEmpty());
            for (String key : variants) {
                var result = engine.run(scenario, key.split("/", 2)[1]);
                assertTrue(result.passed(), key + ": " + result.status() + " runtime=" + root);
            }
        }
    }
}
