package se.skltpnext.experiment001.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.skltpnext.experiment001.authorization.NimbusDpopGate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConformanceGates {
    public GateReport run() {
        NimbusDpopGate.GateResult nimbus = new NimbusDpopGate().run();
        ContractValidators contracts = new ContractValidators();
        var structure = contracts.validateStructure();
        var swaggerPositive = ContractValidators.validateSwaggerFixture(
                ContractValidators.requiredUrl(ContractValidators.CONTRACT_RESOURCE), true);
        var swaggerNegative = ContractValidators.validateSwaggerFixture(
                ContractValidators.requiredUrl(
                        "experiment-001/tool-conformance/invalid-openapi-3.1.2.json"), false);
        URI requestUri = URI.create("https://localhost/synthetic-records/synthetic-record-001");
        boolean kappaPositive = contracts.validateProviderRequest(requestUri).passed()
                && contracts.validateConsumerRequest(requestUri).passed()
                && contracts.validateProviderResponse(
                "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}").passed()
                && contracts.validateConsumerResponse(
                "{\"recordId\":\"synthetic-record-001\",\"status\":\"available\"}").passed();
        boolean kappaNegativeRequest = contracts.rejectsInvalidRequest(requestUri);
        boolean kappaNegativeResponse = contracts.rejectsInvalidResponse();
        String jacksonVersion = new ObjectMapper().version().toString();
        boolean jacksonCompatible = "2.22.0".equals(jacksonVersion);
        boolean swaggerPass = "3.1.2".equals(structure.openApiVersion())
                && swaggerPositive.passed() && swaggerNegative.passed();
        boolean kappaPass = kappaPositive && kappaNegativeRequest
                && kappaNegativeResponse && jacksonCompatible;
        boolean all = nimbus.passed() && swaggerPass && kappaPass;
        return new GateReport(nimbus, swaggerPass, swaggerPositive.passed(),
                swaggerNegative.passed(), kappaPass, kappaPositive,
                kappaNegativeRequest, kappaNegativeResponse, jacksonVersion,
                all ? "pass" : "inconclusive");
    }

    public record GateReport(
            NimbusDpopGate.GateResult nimbus,
            boolean swaggerParserGate,
            boolean swaggerPositiveAccepted,
            boolean swaggerNegativeRejected,
            boolean kappaGate,
            boolean kappaPositiveAccepted,
            boolean kappaInvalidRequestRejected,
            boolean kappaInvalidResponseRejected,
            String jacksonVersion,
            String status) {
        public boolean passed() {
            return "pass".equals(status);
        }
    }
}
