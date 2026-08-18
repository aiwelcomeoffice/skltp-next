package se.skltpnext.experiment001.contract;

import com.github.erosb.kappa.core.model.AuthOption;
import com.github.erosb.kappa.core.validation.ValidationException;
import com.github.erosb.kappa.operation.validator.model.Request;
import com.github.erosb.kappa.operation.validator.model.impl.Body;
import com.github.erosb.kappa.operation.validator.model.impl.DefaultRequest;
import com.github.erosb.kappa.operation.validator.model.impl.DefaultResponse;
import com.github.erosb.kappa.operation.validator.validation.RequestValidator;
import com.github.erosb.kappa.parser.OpenApi3Parser;
import com.github.erosb.kappa.parser.model.v3.OpenApi3;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ContractValidators {
    public static final String CONTRACT_RESOURCE =
            "experiment-001/contracts/read-api-1.0.0.openapi.json";
    private final URL contractUrl;
    private final OpenApi3 kappaContract;
    private final RequestValidator kappaValidator;

    public ContractValidators() {
        contractUrl = requiredUrl(CONTRACT_RESOURCE);
        try {
            kappaContract = new OpenApi3Parser().parse(kappaReadableUrl(contractUrl),
                    List.<AuthOption>of(), true);
            kappaValidator = new RequestValidator(kappaContract);
        } catch (Exception e) {
            throw new IllegalStateException("Kappa could not parse the pinned OpenAPI contract", e);
        }
    }

    public StructuralResult validateStructure() {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(readUrl(contractUrl), null, options);
        if (result.getOpenAPI() == null || !result.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Swagger Parser rejected the contract: " + result.getMessages());
        }
        OpenAPI api = result.getOpenAPI();
        if (!"3.1.2".equals(api.getOpenapi())) {
            throw new IllegalArgumentException("OpenAPI version must be exactly 3.1.2");
        }
        // Swagger materialises OAS's implicit "/" default server. The overlay
        // rejects only an authored servers member, because neither value may be
        // used for runtime discovery.
        if (JsonSupport.readResource(CONTRACT_RESOURCE).has("servers")) {
            throw new IllegalArgumentException("OpenAPI servers must not be a runtime endpoint source");
        }
        if (api.getPaths() == null || api.getPaths().size() != 1
                || api.getPaths().get("/synthetic-records/{recordId}") == null
                || api.getPaths().get("/synthetic-records/{recordId}").getGet() == null) {
            throw new IllegalArgumentException("Contract must contain exactly one GET operation");
        }
        var operation = api.getPaths().get("/synthetic-records/{recordId}").getGet();
        if (!"readSyntheticRecord".equals(operation.getOperationId())) {
            throw new IllegalArgumentException("Stable operationId is required");
        }
        if (!"1.0.0".equals(api.getInfo().getVersion())) {
            throw new IllegalArgumentException("Contract version is not release-bound");
        }
        if (api.getComponents() == null || api.getComponents().getSecuritySchemes() == null
                || !api.getComponents().getSecuritySchemes().containsKey("syntheticOAuth")) {
            throw new IllegalArgumentException("OAuth security scheme is required");
        }
        return new StructuralResult("3.1.2", "2.1.45", "pass");
    }

    public ValidationRecord validateProviderRequest(URI requestUri) {
        return validateRequest("provider", requestUri, true);
    }

    public ValidationRecord validateConsumerRequest(URI requestUri) {
        return validateRequest("consumer", requestUri, true);
    }

    public ValidationRecord validateProviderResponse(String body) {
        return validateResponse("provider", body, true);
    }

    public ValidationRecord validateConsumerResponse(String body) {
        return validateResponse("consumer", body, true);
    }

    public boolean rejectsInvalidRequest(URI requestUri) {
        return "denied".equals(validateRequest("tool-gate", requestUri, false).result());
    }

    public boolean rejectsInvalidResponse() {
        return "denied".equals(validateResponse("tool-gate", "{\"recordId\":\"wrong\",\"unexpected\":true}", false).result());
    }

    private ValidationRecord validateRequest(String role, URI requestUri, boolean validFixture) {
        DefaultRequest.Builder builder = new DefaultRequest.Builder(requestUri.toString(), Request.Method.GET);
        if (validFixture) {
            builder.header("Accept", "application/json");
        }
        try {
            kappaValidator.validate(builder.build());
            if (!validFixture) {
                return new ValidationRecord(role, "request", "accepted-negative-fixture");
            }
            return new ValidationRecord(role, "request", "pass");
        } catch (ValidationException e) {
            if (validFixture) {
                throw new IllegalArgumentException(role + " request validation failed", e);
            }
            return new ValidationRecord(role, "request", "denied");
        }
    }

    private ValidationRecord validateResponse(String role, String body, boolean validFixture) {
        var request = new DefaultRequest.Builder(
                "https://localhost/synthetic-records/synthetic-record-001", Request.Method.GET)
                .header("Accept", "application/json")
                .build();
        var response = new DefaultResponse.Builder(200)
                .header("Content-Type", "application/json")
                .body(Body.from(body))
                .build();
        try {
            kappaValidator.validate(response, request);
            if (!validFixture) {
                return new ValidationRecord(role, "response", "accepted-negative-fixture");
            }
            return new ValidationRecord(role, "response", "pass");
        } catch (ValidationException e) {
            if (validFixture) {
                throw new IllegalArgumentException(role + " response validation failed", e);
            }
            return new ValidationRecord(role, "response", "denied");
        }
    }

    public static SwaggerGateResult validateSwaggerFixture(URL url, boolean expectedValid) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(readUrl(url), null, options);
        boolean accepted = result.getOpenAPI() != null && result.getMessages().isEmpty();
        return new SwaggerGateResult(expectedValid, accepted, result.getMessages().size());
    }

    public static URL requiredUrl(String name) {
        URL url = ContractValidators.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalArgumentException("Missing classpath resource " + name);
        }
        return url;
    }

    private static String readUrl(URL url) {
        try (var input = url.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read OpenAPI fixture", e);
        }
    }

    private static URL kappaReadableUrl(URL source) {
        if (!"jar".equals(source.getProtocol())) {
            return source;
        }
        Path materialized = Path.of("target/experiment-001/tool-runtime",
                "read-api-1.0.0.openapi.json");
        try {
            Files.createDirectories(materialized.getParent());
            Files.writeString(materialized, readUrl(source), StandardCharsets.UTF_8);
            return materialized.toAbsolutePath().toUri().toURL();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot materialize OpenAPI for Kappa", e);
        }
    }

    public record StructuralResult(String openApiVersion, String swaggerParserVersion, String result) {
    }

    public record ValidationRecord(String role, String phase, String result) {
        public boolean passed() {
            return "pass".equals(result);
        }
    }

    public record SwaggerGateResult(boolean expectedValid, boolean accepted, int messageCount) {
        public boolean passed() {
            return expectedValid == accepted;
        }
    }
}
