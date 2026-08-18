package se.skltpnext.experiment001.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MetadataStores {
    private static final String SCHEMA_ROOT = "experiment-001/schemas/";
    private final Path runtimeRoot;
    private final ECKey metadataPublicKey;

    public MetadataStores(Path runtimeRoot) {
        this.runtimeRoot = runtimeRoot;
        try {
            metadataPublicKey = ECKey.parse(Files.readString(
                    runtimeRoot.resolve("public-trust/metadata.jwk.json"), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load metadata trust key", e);
        }
    }

    public static void writeBaseline(Path runtimeRoot, URI tokenEndpoint,
                                     URI producerEndpoint, CryptoMaterial material) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ObjectNode service = common("service", now);
        ArrayNode entries = service.putArray("entries");
        ObjectNode entry = entries.addObject();
        entry.put("federationId", ExperimentConfig.FEDERATION_ID);
        entry.put("testContextId", ExperimentConfig.TEST_CONTEXT_ID);
        entry.put("producerOrganizationId", ExperimentConfig.ORGANIZATION_B);
        entry.put("apiProfileId", ExperimentConfig.API_PROFILE_ID);
        entry.put("contractVersion", ExperimentConfig.CONTRACT_VERSION);
        entry.put("endpointId", "PRODUCER-ENDPOINT-REV-1");
        entry.put("endpointRevision", 1);
        entry.put("endpointUri", producerEndpoint.toString());

        ObjectNode membership = common("membership", now);
        ArrayNode members = membership.putArray("members");
        members.addObject().put("organizationId", ExperimentConfig.ORGANIZATION_A)
                .put("role", "consumer").put("status", "active");
        members.addObject().put("organizationId", ExperimentConfig.ORGANIZATION_B)
                .put("role", "producer").put("status", "active");

        ObjectNode iam = common("iam", now);
        iam.put("organizationId", ExperimentConfig.ORGANIZATION_A);
        iam.put("systemId", ExperimentConfig.SYSTEM_A);
        iam.put("clientId", ExperimentConfig.CLIENT_ID);
        iam.put("oauthIssuer", tokenEndpoint.resolve("/").toString());
        iam.put("tokenEndpoint", tokenEndpoint.toString());
        iam.put("audience", ExperimentConfig.AUDIENCE);
        iam.put("scope", ExperimentConfig.SCOPE_READ);
        iam.put("clientAuthenticationMethod", "private_key_jwt");
        iam.set("clientAuthenticationJwk", parseJwk(material.clientAuthenticationKey().toPublicJWK()));
        iam.set("authorizationServerSigningJwk", parseJwk(material.authorizationServerSigningKey().toPublicJWK()));
        iam.set("dpopJwk", parseJwk(material.dpopKey().toPublicJWK()));

        writeSigned(runtimeRoot, "service", service, material.metadataKey());
        writeSigned(runtimeRoot, "membership", membership, material.metadataKey());
        writeSigned(runtimeRoot, "iam", iam, material.metadataKey());
    }

    private static ObjectNode common(String family, Instant now) {
        ObjectNode node = JsonSupport.MAPPER.createObjectNode();
        node.put("family", family);
        node.put("issuer", ExperimentConfig.METADATA_ISSUER);
        node.put("context", ExperimentConfig.TEST_CONTEXT_ID);
        node.put("revision", 1);
        node.put("issuedAt", now.toString());
        node.put("validFrom", now.minusSeconds(1).toString());
        node.put("expiresAt", now.plusSeconds(300).toString());
        node.put("status", "active");
        return node;
    }

    private static JsonNode parseJwk(ECKey key) {
        try {
            return JsonSupport.MAPPER.readTree(key.toJSONString());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeSigned(Path runtimeRoot, String family, JsonNode payload, ECKey key) {
        try {
            JWSObjectJSON jws = new JWSObjectJSON(new Payload(JsonSupport.compact(payload)));
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("metadata+jws"))
                    .keyID(key.getKeyID())
                    .build();
            jws.sign(header, new ECDSASigner(key));
            Path target = runtimeRoot.resolve("metadata").resolve(family + "-rev-1.jws.json");
            Files.createDirectories(target.getParent());
            Files.writeString(target, jws.serializeFlattened(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign " + family + " metadata", e);
        }
    }

    public DiscoveryResult discover() {
        JsonNode service = readAndValidate("service");
        JsonNode membership = readAndValidate("membership");
        JsonNode iam = readAndValidate("iam");

        boolean consumerActive = activeMember(membership, ExperimentConfig.ORGANIZATION_A, "consumer");
        boolean producerActive = activeMember(membership, ExperimentConfig.ORGANIZATION_B, "producer");
        if (!consumerActive || !producerActive) {
            throw new IllegalStateException("Both synthetic memberships must be active");
        }

        List<JsonNode> candidates = new ArrayList<>();
        for (JsonNode entry : service.required("entries")) {
            if (ExperimentConfig.FEDERATION_ID.equals(entry.required("federationId").textValue())
                    && ExperimentConfig.TEST_CONTEXT_ID.equals(entry.required("testContextId").textValue())
                    && ExperimentConfig.ORGANIZATION_B.equals(entry.required("producerOrganizationId").textValue())
                    && ExperimentConfig.API_PROFILE_ID.equals(entry.required("apiProfileId").textValue())
                    && ExperimentConfig.CONTRACT_VERSION.equals(entry.required("contractVersion").textValue())) {
                candidates.add(entry);
            }
        }
        if (candidates.size() != 1) {
            throw new IllegalStateException("Discovery must resolve exactly one endpoint");
        }
        if (!ExperimentConfig.ORGANIZATION_A.equals(iam.required("organizationId").textValue())
                || !ExperimentConfig.SYSTEM_A.equals(iam.required("systemId").textValue())
                || !ExperimentConfig.CLIENT_ID.equals(iam.required("clientId").textValue())) {
            throw new IllegalStateException("IAM organization-system-client relation mismatch");
        }
        JsonNode endpoint = candidates.getFirst();
        return new DiscoveryResult(
                URI.create(endpoint.required("endpointUri").textValue()),
                endpoint.required("endpointId").textValue(),
                endpoint.required("endpointRevision").intValue(),
                URI.create(iam.required("tokenEndpoint").textValue()),
                iam.required("oauthIssuer").textValue(),
                iam.required("audience").textValue(),
                parsePublicKey(iam.required("clientAuthenticationJwk")),
                parsePublicKey(iam.required("authorizationServerSigningJwk")),
                consumerActive, producerActive, candidates.size());
    }

    public JsonNode readAndValidate(String family) {
        try {
            String serialized = Files.readString(runtimeRoot.resolve("metadata/" + family + "-rev-1.jws.json"),
                    StandardCharsets.UTF_8);
            JWSObjectJSON jws = JWSObjectJSON.parse(serialized);
            if (jws.getSignatures().size() != 1) {
                throw new IllegalArgumentException("Metadata must have exactly one signature");
            }
            var signature = jws.getSignatures().getFirst();
            if (!JWSAlgorithm.ES256.equals(signature.getHeader().getAlgorithm())
                    || !metadataPublicKey.getKeyID().equals(signature.getHeader().getKeyID())
                    || !signature.verify(new ECDSAVerifier(metadataPublicKey))) {
                throw new IllegalArgumentException("Metadata provenance validation failed");
            }
            JsonNode payload = JsonSupport.MAPPER.readTree(jws.getPayload().toString());
            if (!family.equals(payload.required("family").textValue())
                    || !ExperimentConfig.METADATA_ISSUER.equals(payload.required("issuer").textValue())
                    || !ExperimentConfig.TEST_CONTEXT_ID.equals(payload.required("context").textValue())
                    || payload.required("revision").intValue() != 1
                    || !"active".equals(payload.required("status").textValue())) {
                throw new IllegalArgumentException("Metadata family/context/revision mismatch");
            }
            Instant now = Instant.now();
            if (now.isBefore(Instant.parse(payload.required("validFrom").textValue()))
                    || !now.isBefore(Instant.parse(payload.required("expiresAt").textValue()))) {
                throw new IllegalArgumentException("Metadata is outside its validity window");
            }
            JsonSupport.validate(JsonSupport.readResource(SCHEMA_ROOT + family + "-metadata.schema.json"),
                    payload, family + " metadata");
            return payload;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("Cannot validate " + family + " metadata", e);
        }
    }

    public boolean membershipActive(String organizationId, String role) {
        return activeMember(readAndValidate("membership"), organizationId, role);
    }

    private static ECKey parsePublicKey(JsonNode node) {
        try {
            return ECKey.parse(node.toString());
        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("Invalid public IAM JWK", e);
        }
    }

    private static boolean activeMember(JsonNode membership, String organizationId, String role) {
        for (JsonNode member : membership.required("members")) {
            if (organizationId.equals(member.required("organizationId").textValue())
                    && role.equals(member.required("role").textValue())) {
                return "active".equals(member.required("status").textValue());
            }
        }
        return false;
    }

    public record DiscoveryResult(
            URI producerEndpoint,
            String endpointId,
            int endpointRevision,
            URI tokenEndpoint,
            String issuer,
            String audience,
            ECKey clientAuthenticationPublicKey,
            ECKey authorizationServerSigningPublicKey,
            boolean consumerMembershipActive,
            boolean producerMembershipActive,
            int candidateCount) {
    }
}
