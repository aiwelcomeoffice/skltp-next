package se.skltpnext.experiment001.contract;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class PhaseFourContractTest {
    @Test
    void pinnedToolsRejectEveryContractFaultAndAcceptSafeProblems() {
        var validator = new ContractValidators();
        assertTrue(validator.bindVersion("1.0.0").passed());
        assertFalse(validator.bindVersion("2.0.0").passed());
        assertFalse(validator.documentedProblemType(403, "{\"type\":\"urn:test:undocumented\"}"));
        assertTrue(validator.documentedProblemType(403, "{\"type\":\"urn:skltp-next:experiment-001:error:local-policy-deny\"}"));
        for (String role : java.util.List.of("provider", "consumer")) {
            assertFalse(validator.observeRequest(role, URI.create("https://localhost/synthetic-records/invalid-record"), "application/json").passed());
            assertTrue(validator.observeRequest(role, URI.create("https://localhost/synthetic-records/synthetic-record-001"), "application/json").passed());
            assertFalse(validator.observeResponse(role, 200, "application/json", "{\"recordId\":\"synthetic-record-001\"}").passed());
            assertFalse(validator.observeResponse(role, 418, "application/problem+json", "{\"type\":\"urn:test:error\",\"title\":\"Synthetic error\",\"status\":418}").passed());
            assertFalse(validator.observeResponse(role, 403, "application/problem+json", "{\"type\":\"urn:test:error\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"synthetic-internal-marker\"}").passed());
            assertTrue(validator.observeResponse(role, 403, "application/problem+json", "{\"type\":\"urn:test:error\",\"title\":\"Forbidden\",\"status\":403}").passed());
        }
    }
}
