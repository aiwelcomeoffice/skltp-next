package se.skltpnext.experiment001.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import se.skltpnext.experiment001.contract.ConformanceGates;
import se.skltpnext.experiment001.evidence.EvidenceCollector;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3ScenarioIntegrationTest {
    private static final String RUN_ID = "phase3-integration-test";

    @TempDir
    Path temporaryDirectory;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void phaseOneTwoAndThreeVariantsPassWithValidatedEvidence() {
        Path runtime = temporaryDirectory.resolve("runtime");
        Path evidence = temporaryDirectory.resolve("evidence");
        CryptoMaterial.generate(runtime);
        TlsMaterial.generate(runtime);
        var gates = new ConformanceGates().run();
        assertTrue(gates.passed(), "pinned tool gates must pass");
        JsonSupport.writeJson(runtime.resolve("validation/tool-gates.json"), gates);

        try (RuntimeEnvironment.RunningEnvironment ignored =
                     new RuntimeEnvironment(runtime, RUN_ID).start()) {
            ScenarioEngine engine = new ScenarioEngine(runtime, RUN_ID);
            assertTrue(engine.ready(), "both isolated HTTPS listeners must be ready");

            List<String> combinations = new ArrayList<>(List.of(
                    "E001-REL-001/valid",
                    "E001-DIS-001/baseline",
                    "E001-FLOW-001/baseline",
                    "E001-CON-001/baseline"));
            combinations.addAll(ExperimentConfig.PHASE_2_VARIANTS.stream().sorted().toList());
            combinations.addAll(ExperimentConfig.PHASE_3_VARIANTS.stream().sorted().toList());
            assertEquals(23, combinations.size());

            for (String combination : combinations) {
                String[] parts = combination.split("/", 2);
                ScenarioEngine.ScenarioResult result = engine.run(parts[0], parts[1]);
                assertTrue(result.passed(), combination + " must match its external oracle");
            }

            EvidenceCollector collector = new EvidenceCollector(runtime, evidence, RUN_ID);
            assertEquals("pass", collector.collect().status());
            assertEquals("pass", collector.validate().status());
        }
    }
}
