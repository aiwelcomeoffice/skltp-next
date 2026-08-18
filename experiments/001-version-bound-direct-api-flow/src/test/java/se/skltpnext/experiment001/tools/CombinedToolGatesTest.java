package se.skltpnext.experiment001.tools;

import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.contract.ConformanceGates;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CombinedToolGatesTest {
    @Test
    void allPinnedToolGatesPassTogether() {
        var report = new ConformanceGates().run();
        assertTrue(report.passed(), () -> "Tool gate report: " + report);
    }
}
