package se.skltpnext.experiment001.contract;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConformanceTest {
    @Test
    void swaggerParserAcceptsExactOpenApi312AndRejectsInvalidFixture() {
        ContractValidators validators = new ContractValidators();
        assertEquals("3.1.2", validators.validateStructure().openApiVersion());

        var valid = ContractValidators.validateSwaggerFixture(
                ContractValidators.requiredUrl(ContractValidators.CONTRACT_RESOURCE), true);
        var invalid = ContractValidators.validateSwaggerFixture(
                ContractValidators.requiredUrl(
                        "experiment-001/tool-conformance/invalid-openapi-3.1.2.json"), false);

        assertTrue(valid.passed(), "Swagger Parser must accept the positive 3.1.2 fixture");
        assertTrue(invalid.passed(), "Swagger Parser must reject the negative 3.1.2 fixture");
    }

    @Test
    void kappaSeparatelyAcceptsValidAndRejectsInvalidRequestAndResponse() {
        ContractValidators validators = new ContractValidators();
        URI uri = URI.create("https://localhost/synthetic-records/synthetic-record-001");

        assertTrue(validators.validateProviderRequest(uri).passed());
        assertTrue(validators.validateConsumerRequest(uri).passed());
        assertTrue(validators.validateProviderResponse(
                "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}").passed());
        assertTrue(validators.validateConsumerResponse(
                "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}").passed());
        assertTrue(validators.rejectsInvalidRequest(uri),
                "Kappa must deny the invalid request fixture");
        assertTrue(validators.rejectsInvalidResponse(),
                "Kappa must deny the invalid response fixture");
    }
}
