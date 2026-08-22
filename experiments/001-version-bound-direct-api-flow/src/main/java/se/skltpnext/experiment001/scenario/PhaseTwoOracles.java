package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PhaseTwoOracles {
    private static final String CATALOG =
            "experiment-001/scenarios/catalog-phase-2-1.0.0.json";
    private static final String CATALOG_SCHEMA =
            "experiment-001/schemas/scenario-catalog-phase-2.schema.json";
    private static final String SECURITY_ERRORS =
            "experiment-001/profiles/security-errors-phase-2-1.0.0.json";
    private static final String SECURITY_ERRORS_SCHEMA =
            "experiment-001/schemas/security-errors-phase-2.schema.json";

    private final Map<String, ExpectedOutcome> outcomes;

    public PhaseTwoOracles() {
        JsonNode catalog = JsonSupport.readResource(CATALOG);
        JsonNode securityErrors = JsonSupport.readResource(SECURITY_ERRORS);
        JsonSupport.validate(JsonSupport.readResource(CATALOG_SCHEMA), catalog,
                "Phase 2 scenario catalog");
        JsonSupport.validate(JsonSupport.readResource(SECURITY_ERRORS_SCHEMA), securityErrors,
                "Phase 2 security-error oracle");

        Map<String, SecurityError> errors = new LinkedHashMap<>();
        for (JsonNode node : securityErrors.required("errorOracles")) {
            JsonNode challenge = node.get("challenge");
            SecurityError error = new SecurityError(
                    node.required("id").textValue(),
                    node.required("httpStatus").intValue(),
                    challenge == null || challenge.isNull()
                            ? null : challenge.required("serialized").textValue(),
                    node.required("problemType").textValue());
            if (errors.put(error.id(), error) != null) {
                throw new IllegalArgumentException("Duplicate Phase 2 security-error oracle");
            }
        }

        Map<String, ExpectedOutcome> loaded = new LinkedHashMap<>();
        for (JsonNode node : catalog.required("variants")) {
            String key = node.required("scenarioId").textValue() + "/"
                    + node.required("variantId").textValue();
            SecurityError error = node.has("securityErrorRef")
                    ? errors.get(node.required("securityErrorRef").textValue()) : null;
            if (node.has("securityErrorRef") && error == null) {
                throw new IllegalArgumentException("Unknown Phase 2 security-error oracle");
            }
            ExpectedOutcome outcome = new ExpectedOutcome(
                    node.required("expectedDecision").textValue(),
                    node.required("securityClass").textValue(),
                    node.required("terminalCheckpoint").textValue(),
                    node.required("businessOperationExecuted").booleanValue(),
                    error);
            if (loaded.put(key, outcome) != null) {
                throw new IllegalArgumentException("Duplicate Phase 2 scenario/variant oracle");
            }
        }
        if (!loaded.keySet().equals(ExperimentConfig.PHASE_2_VARIANTS)) {
            throw new IllegalArgumentException("Phase 2 catalog differs from the required variant register");
        }
        outcomes = Map.copyOf(loaded);
    }

    public ExpectedOutcome expected(String scenarioId, String variantId) {
        ExpectedOutcome outcome = outcomes.get(scenarioId + "/" + variantId);
        if (outcome == null) {
            throw new IllegalArgumentException("Unknown Phase 2 scenario/variant oracle");
        }
        return outcome;
    }

    public record ExpectedOutcome(
            String decision,
            String securityClass,
            String terminalCheckpoint,
            boolean businessOperationExecuted,
            SecurityError securityError) {

        public int httpStatus() {
            return securityError == null ? 200 : securityError.httpStatus();
        }

        public String wwwAuthenticate() {
            return securityError == null ? null : securityError.wwwAuthenticate();
        }

        public String problemType() {
            return securityError == null ? null : securityError.problemType();
        }
    }

    public record SecurityError(
            String id,
            int httpStatus,
            String wwwAuthenticate,
            String problemType) {
    }
}
