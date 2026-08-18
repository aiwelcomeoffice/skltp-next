package se.skltpnext.experiment001.tools;

import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.authorization.NimbusDpopGate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NimbusDpopConformanceTest {
    @Test
    void explicitIatSevenSecondWindowReplayNamespacesAndResetAreDeterministic() {
        var result = new NimbusDpopGate().run();
        assertTrue(result.passed(), () -> "Nimbus DPoP gate failed: " + result);
    }
}

