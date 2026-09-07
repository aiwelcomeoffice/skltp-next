package se.skltpnext.experiment001.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.*;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.evidence.PhaseFourEvidence;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class PhaseFourFaultTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path temporary;
    @Test @Timeout(90)
    void allFaultsAreIsolatedAndLateResponsesCannotChangeResults() throws Exception {
        Path runtime = temporary.resolve("runtime");
        CryptoMaterial.generate(runtime); TlsMaterial.generate(runtime);
        var failures = new ArrayList<String>();
        try (var environment = new RuntimeEnvironment(runtime, "phase4-directed").start()) {
            var engine = new ScenarioEngine(runtime, "phase4-directed");
            for (String key : ExperimentConfig.PHASE_4_VARIANTS.stream().sorted().toList()) {
                String[] parts = key.split("/", 2);
                var result = engine.run(parts[0], parts[1]);
                if (!result.passed()) failures.add(key + ": " + result.status());
                var baseline = engine.run("E001-FLOW-001", "baseline");
                if (!baseline.passed()) failures.add("reset after " + key + ": " + baseline.status());
            }
            var observations = new PhaseFourEvidence(runtime, "phase4-directed", true).evaluate();
            observations.forEach((key, valid) -> { if (!valid) failures.add("observation " + key); });
        }
        assertTrue(failures.isEmpty(), failures + " runtime=" + runtime);
    }
}
