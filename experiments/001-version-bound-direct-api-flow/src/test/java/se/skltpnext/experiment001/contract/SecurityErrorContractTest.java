package se.skltpnext.experiment001.contract;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityErrorContractTest {
    @ParameterizedTest
    @MethodSource("securityErrors")
    void producerSecurityErrorsRemainDocumentedProblemDetails(int status, String type, String title) {
        String body = "{\"type\":\"" + type + "\",\"title\":\"" + title
                + "\",\"status\":" + status + "}";

        assertTrue(new ContractValidators().validateProviderError(status, body).passed());
    }

    private static Stream<Arguments> securityErrors() {
        return Stream.of(
                Arguments.of(401, "urn:skltp-next:experiment-001:error:missing-token", "Missing token"),
                Arguments.of(401, "urn:skltp-next:experiment-001:error:invalid-token", "Invalid token"),
                Arguments.of(401, "urn:skltp-next:experiment-001:error:sender-constraint",
                        "Sender constraint failed"),
                Arguments.of(403, "urn:skltp-next:experiment-001:error:insufficient-scope",
                        "Insufficient scope"),
                Arguments.of(403, "urn:skltp-next:experiment-001:error:local-policy-deny",
                        "Forbidden by local policy"));
    }
}
