package se.skltpnext.experiment001.scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.cli.RuntimeEnvironment;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
/** Regression for the former revision-1 blocker documented in the historical 3b1 report. */
class Phase3b1DiscoveryObservationTest {
    @TempDir Path runtime;
    @Test @Timeout(90)
    void signedSecondRevisionReachesSecondReceiverWithoutChangingConsumerInputs() {
        CryptoMaterial.generate(runtime); TlsMaterial.generate(runtime);
        try (var ignored = new RuntimeEnvironment(runtime, "phase3-discovery").start()) {
            var engine = new ScenarioEngine(runtime, "phase3-discovery");
            assertTrue(engine.ready());
            assertTrue(engine.run("E001-DIS-002", "baseline").passed());
            assertTrue(engine.run("E001-FLOW-001", "baseline").passed(), "reset must restore revision 1");
        }
    }
}
