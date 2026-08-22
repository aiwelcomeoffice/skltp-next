package se.skltpnext.experiment001.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseTwoResourceContractTest {
    private static final String CATALOG =
            "experiment-001/scenarios/catalog-phase-2-1.0.0.json";
    private static final String CATALOG_SCHEMA =
            "experiment-001/schemas/scenario-catalog-phase-2.schema.json";
    private static final String SECURITY_ERRORS =
            "experiment-001/profiles/security-errors-phase-2-1.0.0.json";
    private static final String SECURITY_ERRORS_SCHEMA =
            "experiment-001/schemas/security-errors-phase-2.schema.json";

    @Test
    void validatesStrictPhaseTwoResourcesAndAllSixteenUniqueOracles() {
        JsonNode catalog = JsonSupport.readResource(CATALOG);
        JsonNode securityErrors = JsonSupport.readResource(SECURITY_ERRORS);

        JsonSupport.validate(JsonSupport.readResource(CATALOG_SCHEMA), catalog,
                "Phase 2 scenario catalog");
        JsonSupport.validate(JsonSupport.readResource(SECURITY_ERRORS_SCHEMA), securityErrors,
                "Phase 2 security-error oracle");

        Set<String> expectedCombinations = Set.of(
                "E001-FLOW-002/baseline",
                "E001-SEC-001/baseline",
                "E001-SEC-002/baseline",
                "E001-AUTHZ-001/insufficient-scope",
                "E001-AUTHZ-001/local-policy-deny",
                "E001-TOK-001/missing",
                "E001-TOK-001/wrong-issuer",
                "E001-TOK-001/wrong-audience",
                "E001-TOK-001/bad-signature",
                "E001-TOK-001/disallowed-algorithm",
                "E001-TOK-001/wrong-type",
                "E001-TOK-001/expired",
                "E001-TOK-001/not-yet-valid",
                "E001-TOK-001/missing-required-claim",
                "E001-TOK-001/wrong-client-id",
                "E001-TOK-001/wrong-sub");

        Set<String> observedCombinations = new HashSet<>();
        Map<String, JsonNode> errorOraclesById = new HashMap<>();
        for (JsonNode oracle : securityErrors.required("errorOracles")) {
            String id = oracle.required("id").textValue();
            assertFalse(errorOraclesById.containsKey(id), "Duplicate security-error oracle: " + id);
            errorOraclesById.put(id, oracle);
        }
        assertEquals(Set.of(
                "E001-SECERR-BEARER-MISSING",
                "E001-SECERR-BEARER-INVALID-TOKEN",
                "E001-SECERR-BEARER-INSUFFICIENT-SCOPE",
                "E001-SECERR-LOCAL-POLICY-DENY",
                "E001-SECERR-DPOP-SENDER-CONSTRAINT"), errorOraclesById.keySet());

        for (JsonNode variant : catalog.required("variants")) {
            String combination = variant.required("scenarioId").textValue() + "/"
                    + variant.required("variantId").textValue();
            assertTrue(observedCombinations.add(combination),
                    "Duplicate Phase 2 scenario/variant: " + combination);

            if ("allow".equals(variant.required("expectedDecision").textValue())) {
                assertFalse(variant.has("securityErrorRef"),
                        "Positive oracle must not reference an HTTP error: " + combination);
            } else {
                String errorRef = variant.required("securityErrorRef").textValue();
                assertTrue(errorOraclesById.containsKey(errorRef),
                        "Unresolved security-error reference: " + errorRef);
                assertFalse(variant.required("businessOperationExecuted").booleanValue(),
                        "Denied variant reached the business operation: " + combination);
            }
        }

        assertEquals(16, observedCombinations.size());
        assertEquals(expectedCombinations, observedCombinations);
        assertEquals(SECURITY_ERRORS, catalog.required("securityErrorOracle").textValue());
    }

    @Test
    void strictSchemasRejectUnknownTopLevelFields() {
        ObjectNode invalidCatalog = JsonSupport.readResource(CATALOG).deepCopy();
        invalidCatalog.put("unexpected", true);
        assertThrows(IllegalArgumentException.class, () -> JsonSupport.validate(
                JsonSupport.readResource(CATALOG_SCHEMA), invalidCatalog,
                "invalid Phase 2 scenario catalog"));

        ObjectNode invalidSecurityErrors = JsonSupport.readResource(SECURITY_ERRORS).deepCopy();
        invalidSecurityErrors.put("unexpected", true);
        assertThrows(IllegalArgumentException.class, () -> JsonSupport.validate(
                JsonSupport.readResource(SECURITY_ERRORS_SCHEMA), invalidSecurityErrors,
                "invalid Phase 2 security-error oracle"));
    }
}
