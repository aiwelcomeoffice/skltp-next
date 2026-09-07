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
    private final Map<String, ECKey> metadataPublicKeys = new java.util.HashMap<>();
    private final String actor;
    private final se.skltpnext.experiment001.telemetry.TelemetryRecorder telemetry;
    private final MutableExperimentClock clock;

    public MetadataStores(Path runtimeRoot) {
        this(runtimeRoot, "consumer", null);
    }

    public MetadataStores(Path runtimeRoot, String actor,
                          se.skltpnext.experiment001.telemetry.TelemetryRecorder telemetry) {
        this.runtimeRoot = runtimeRoot;
        this.actor = actor;
        this.telemetry = telemetry;
        this.clock = new MutableExperimentClock(runtimeRoot);
        try {
            for (String family : List.of("service", "membership", "iam")) {
                String file = family.equals("service") ? "metadata" : "metadata-" + family;
                metadataPublicKeys.put(family, ECKey.parse(Files.readString(
                        runtimeRoot.resolve("public-trust/" + file + ".jwk.json"), StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load metadata trust key", e);
        }
    }

    public static void writeBaseline(Path runtimeRoot, URI tokenEndpoint,
                                     URI producerEndpoint, CryptoMaterial material) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        new MutableExperimentClock(runtimeRoot).reset(now);
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
        iam.put("authorizationServerSigningKeyStatus", "active");
        iam.putArray("organizations").add(ExperimentConfig.ORGANIZATION_A).add(ExperimentConfig.ORGANIZATION_B);
        var systems = iam.putArray("systems");
        systems.addObject().put("systemId", ExperimentConfig.SYSTEM_A).put("organizationId", ExperimentConfig.ORGANIZATION_A);
        systems.addObject().put("systemId", "E001-SYSTEM-B").put("organizationId", ExperimentConfig.ORGANIZATION_B);
        iam.putArray("clients").addObject().put("clientId", ExperimentConfig.CLIENT_ID)
                .put("systemId", ExperimentConfig.SYSTEM_A).put("keyId", material.clientAuthenticationKey().getKeyID());
        iam.put("oauthIssuer", tokenEndpoint.resolve("/").toString());
        iam.put("tokenEndpoint", tokenEndpoint.toString());
        iam.put("audience", ExperimentConfig.AUDIENCE);
        iam.put("scope", ExperimentConfig.SCOPE_READ);
        iam.put("clientAuthenticationMethod", "private_key_jwt");
        iam.set("clientAuthenticationJwk", parseJwk(material.clientAuthenticationKey().toPublicJWK()));
        iam.set("authorizationServerSigningJwk", parseJwk(material.authorizationServerSigningKey().toPublicJWK()));
        iam.set("dpopJwk", parseJwk(material.dpopKey().toPublicJWK()));

        writeSigned(runtimeRoot, "service", service, material.metadataKey());
        writeSigned(runtimeRoot, "membership", membership, CryptoMaterial.metadataSigningKey(runtimeRoot, "membership"));
        writeSigned(runtimeRoot, "iam", iam, CryptoMaterial.metadataSigningKey(runtimeRoot, "iam"));
        MetadataStores stores = new MetadataStores(runtimeRoot);
        for (String family : List.of("service", "membership", "iam")) stores.activate(family, 1);
    }

    private static ObjectNode common(String family, Instant now) {
        ObjectNode node = JsonSupport.MAPPER.createObjectNode();
        node.put("family", family);
        node.put("issuer", ExperimentConfig.METADATA_ISSUER + ":" + family);
        node.put("context", ExperimentConfig.TEST_CONTEXT_ID);
        node.put("revision", 1);
        node.putNull("previousDigest");
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

    public static void writeSigned(Path runtimeRoot, String family, JsonNode payload, ECKey key) {
        try {
            JWSObjectJSON jws = new JWSObjectJSON(new Payload(JsonSupport.compact(payload)));
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("metadata+jws"))
                    .keyID(key.getKeyID())
                    .build();
            jws.sign(header, new ECDSASigner(key));
            Path target = runtimeRoot.resolve("metadata").resolve(family + "-rev-" + payload.required("revision").intValue() + ".jws.json");
            Files.createDirectories(target.getParent());
            Files.writeString(target, jws.serializeFlattened(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign " + family + " metadata", e);
        }
    }

    public DiscoveryResult discover() {
        JsonNode service = readAndValidate("service");
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode entry : service.required("entries")) {
            if (ExperimentConfig.FEDERATION_ID.equals(entry.path("federationId").asText())
                    && ExperimentConfig.TEST_CONTEXT_ID.equals(entry.path("testContextId").asText())
                    && ExperimentConfig.ORGANIZATION_B.equals(entry.path("producerOrganizationId").asText())
                    && ExperimentConfig.API_PROFILE_ID.equals(entry.path("apiProfileId").asText())
                    && ExperimentConfig.CONTRACT_VERSION.equals(entry.path("contractVersion").asText())
                    && "active".equals(service.path("status").asText())) matches.add(entry);
        }
        String reason = matches.isEmpty() ? "missing-endpoint" : matches.size() > 1 ? "ambiguous-endpoint" : "single-endpoint";
        if (telemetry != null) {
            telemetry.discovery(matches.size(), matches.size() == 1 ? "allow" : "deny", reason,
                    matches.size() == 1 ? matches.getFirst().required("endpointId").textValue() : null,
                    matches.size() == 1 ? matches.getFirst().required("endpointRevision").intValue() : 0);
            telemetry.decision("discovery.resolved", "discovery", matches.size() == 1 ? "allow" : "deny", reason);
        }
        if (matches.size() != 1) throw new MetadataFailure("service", reason);
        JsonNode membership = readAndValidate("membership");

        boolean consumerActive = checkedMember(membership, ExperimentConfig.ORGANIZATION_A, "consumer");
        boolean producerActive = checkedMember(membership, ExperimentConfig.ORGANIZATION_B, "producer");
        if (!consumerActive || !producerActive) {
            throw new MetadataFailure("membership", !consumerActive ? "consumer-inactive" : "producer-inactive");
        }

        JsonNode iam = readAndValidate("iam");
        JsonNode endpoint = matches.getFirst();
        return new DiscoveryResult(
                URI.create(endpoint.required("endpointUri").textValue()),
                endpoint.required("endpointId").textValue(),
                endpoint.required("endpointRevision").intValue(),
                URI.create(iam.required("tokenEndpoint").textValue()),
                iam.required("oauthIssuer").textValue(),
                iam.required("audience").textValue(),
                parsePublicKey(iam.required("clientAuthenticationJwk")),
                parsePublicKey(iam.required("authorizationServerSigningJwk")),
                consumerActive, producerActive, matches.size());
    }

    /** Publication is local control-plane state; each reader notices the new revision independently. */
    public void activate(String family, int revision) {
        familyProfile(family);
        JsonSupport.writeJson(runtimeRoot.resolve("metadata/" + family + "-active.json"), Map.of(
                "revision", revision, "activatedAt", clock.instant().toString(), "available", true));
    }

    public ObjectNode revision(String family, int revision) {
        try {
            var jws = JWSObjectJSON.parse(Files.readString(revisionPath(family, revision)));
            return (ObjectNode) JsonSupport.MAPPER.readTree(jws.getPayload().toString());
        } catch (Exception e) { throw new IllegalStateException("Cannot load fixture revision", e); }
    }

    public ObjectNode nextRevision(String family) {
        JsonNode active = read(runtimeRoot.resolve("metadata/" + family + "-active.json"));
        ObjectNode previous = revision(family, active.required("revision").intValue());
        ObjectNode next = previous.deepCopy();
        next.put("revision", previous.required("revision").intValue() + 1);
        next.put("previousDigest", JsonSupport.sha256(JsonSupport.compact(previous).getBytes(StandardCharsets.UTF_8)));
        next.put("issuedAt", clock.instant().toString());
        next.put("validFrom", clock.instant().minusSeconds(1).toString());
        next.put("expiresAt", clock.instant().plusSeconds(300).toString());
        return next;
    }

    public void sourceAvailable(String family, boolean available) {
        Path path = runtimeRoot.resolve("metadata/" + family + "-active.json");
        ObjectNode active = (ObjectNode) read(path);
        active.put("available", available);
        JsonSupport.writeJson(path, active);
    }

    private Path revisionPath(String family, int revision) {
        return runtimeRoot.resolve("metadata/" + family + "-rev-" + revision + ".jws.json");
    }

    private static JsonNode read(Path path) {
        try { return JsonSupport.MAPPER.readTree(path.toFile()); }
        catch (IOException e) { throw new IllegalStateException("Cannot read metadata state", e); }
    }

    private static String familyProfile(String family) {
        return switch (family) {
            case "service" -> "discovery";
            case "membership" -> "membership";
            case "iam" -> "iam";
            default -> throw new IllegalArgumentException("Unknown metadata family");
        };
    }

    public JsonNode readAndValidate(String family) {
        long started = System.nanoTime();
        String profile = familyProfile(family);
        JsonNode policy = JsonSupport.readResource("experiment-001/profiles/" + profile + "-1.0.0.json");
        long ttl = policy.required("ttlSeconds").longValue() * 1000;
        long max = policy.required("maxStalenessSeconds").longValue() * 1000;
        Path statePath = runtimeRoot.resolve("metadata/cache/" + actor + "/" + family + ".json");
        Path cachedPath = runtimeRoot.resolve("metadata/cache/" + actor + "/" + family + ".jws.json");
        JsonNode active = read(runtimeRoot.resolve("metadata/" + family + "-active.json"));
        int revision = active.required("revision").intValue();
        JsonNode state = Files.isRegularFile(statePath) ? read(statePath) : null;
        long age = 0;
        Instant fetched = state == null ? clock.instant() : Instant.parse(state.required("fetchedAt").textValue());
        String cacheState = state == null ? "miss" : "hit";
        try {
            if (state != null && revision < state.required("revision").intValue())
                throw new MetadataFailure(family, "rollback");
            boolean changed = state != null && revision != state.required("revision").intValue();
            if (changed) cacheState = "invalidated";
            boolean available = active.required("available").booleanValue();
            String serialized;
            if (!available && state == null) throw new MetadataFailure(family, "unavailable");
            if (!available && changed) throw new MetadataFailure(family, "unavailable");
            if (state != null && !changed) {
                age = java.time.Duration.between(Instant.parse(state.required("issuedAt").textValue()), clock.instant()).toMillis();
            }
            if (available && (state == null || changed || age > ttl)) {
                if (state != null && !changed) cacheState = "revalidated";
                fetched = clock.instant();
                serialized = Files.readString(revisionPath(family, revision), StandardCharsets.UTF_8);
            } else {
                serialized = Files.readString(cachedPath, StandardCharsets.UTF_8);
                if (!available) cacheState = "source-unavailable";
            }
            JWSObjectJSON jws = JWSObjectJSON.parse(serialized);
            if (jws.getSignatures().size() != 1) throw new MetadataFailure(family, "integrity");
            var signature = jws.getSignatures().getFirst();
            ECKey metadataPublicKey = metadataPublicKeys.get(family);
            if (!JWSAlgorithm.ES256.equals(signature.getHeader().getAlgorithm())
                    || !new JOSEObjectType("metadata+jws").equals(signature.getHeader().getType())
                    || !metadataPublicKey.getKeyID().equals(signature.getHeader().getKeyID())
                    || !signature.verify(new ECDSAVerifier(metadataPublicKey)))
                throw new MetadataFailure(family, "integrity");
            JsonNode payload = JsonSupport.MAPPER.readTree(jws.getPayload().toString());
            if (!family.equals(payload.required("family").textValue())) throw new MetadataFailure(family, "family");
            if (!(ExperimentConfig.METADATA_ISSUER + ":" + family).equals(payload.required("issuer").textValue()))
                throw new MetadataFailure(family, "authority");
            if (!ExperimentConfig.TEST_CONTEXT_ID.equals(payload.required("context").textValue()))
                throw new MetadataFailure(family, "context");
            if (payload.required("revision").intValue() != revision) throw new MetadataFailure(family, "revision");
            JsonSupport.validateResource(SCHEMA_ROOT + family + "-metadata-phase-3.schema.json", payload, "metadata");
            String digest = JsonSupport.sha256(JsonSupport.compact(payload).getBytes(StandardCharsets.UTF_8));
            if (state != null) {
                if (!changed && !digest.equals(state.required("digest").textValue())) throw new MetadataFailure(family, "revision-mutation");
                if (changed && (revision != state.required("revision").intValue() + 1
                        || !state.required("digest").textValue().equals(payload.path("previousDigest").asText())))
                    throw new MetadataFailure(family, "revision-chain");
            }
            age = java.time.Duration.between(Instant.parse(payload.required("issuedAt").textValue()), clock.instant()).toMillis();
            if (age < 0 || clock.instant().isBefore(Instant.parse(payload.required("validFrom").textValue())))
                throw new MetadataFailure(family, "not-yet-valid");
            if (age > max) throw new MetadataFailure(family, "stale");
            if (!clock.instant().isBefore(Instant.parse(payload.required("expiresAt").textValue())))
                throw new MetadataFailure(family, "expired");
            if (family.equals("iam")) validateIamRelations(payload);
            Files.createDirectories(cachedPath.getParent());
            Files.writeString(cachedPath, serialized, StandardCharsets.UTF_8);
            JsonSupport.writeJson(statePath, Map.of("revision", revision, "digest", digest,
                    "issuedAt", payload.required("issuedAt").textValue(), "fetchedAt", fetched.toString()));
            observe(family, revision, age, ttl, max, cacheState, "allow", "valid", active, fetched, started);
            return payload;
        } catch (MetadataFailure e) {
            observe(family, revision, age, ttl, max, cacheState, "deny", e.reason(), active, fetched, started);
            throw e;
        } catch (Exception e) {
            // Malformed/unverifiable metadata is a protected input error, never a usable cache fallback.
            observe(family, revision, age, ttl, max, cacheState, "deny", "invalid", active, fetched, started);
            throw new MetadataFailure(family, "invalid");
        }
    }

    private void validateIamRelations(JsonNode iam) {
        String organization = iam.required("organizationId").textValue();
        String system = iam.required("systemId").textValue();
        String client = iam.required("clientId").textValue();
        boolean knownOrganization = false;
        for (JsonNode item : iam.required("organizations")) if (organization.equals(item.asText())) knownOrganization = true;
        if (!knownOrganization) throw new MetadataFailure("iam", "unknown-organization");
        JsonNode systemRecord = unique(iam.required("systems"), "systemId", system, "unknown-system");
        JsonNode clientRecord = unique(iam.required("clients"), "clientId", client, "unknown-client");
        if (!organization.equals(systemRecord.required("organizationId").asText()))
            throw new MetadataFailure("iam", "system-wrong-organization");
        if (!system.equals(clientRecord.required("systemId").asText()))
            throw new MetadataFailure("iam", "client-wrong-system");
        if (!ExperimentConfig.ORGANIZATION_A.equals(organization) || !ExperimentConfig.SYSTEM_A.equals(system)
                || !ExperimentConfig.CLIENT_ID.equals(client)) throw new MetadataFailure("iam", "unexpected-identity");
        ECKey clientKey = parsePublicKey(iam.required("clientAuthenticationJwk"));
        ECKey ownedKey = parsePublicKey(read(runtimeRoot.resolve("public-trust/client-authentication.jwk.json")));
        try {
            if (clientKey.isPrivate() || !clientRecord.required("keyId").asText().equals(clientKey.getKeyID())
                    || !ownedKey.computeThumbprint().equals(clientKey.computeThumbprint()))
                throw new MetadataFailure("iam", "client-wrong-key");
            if (parsePublicKey(iam.required("authorizationServerSigningJwk")).isPrivate()
                    || parsePublicKey(iam.required("dpopJwk")).isPrivate()) throw new MetadataFailure("iam", "private-key");
        } catch (com.nimbusds.jose.JOSEException e) { throw new MetadataFailure("iam", "invalid-key"); }
    }

    private static JsonNode unique(JsonNode records, String field, String value, String missingReason) {
        JsonNode found = null;
        for (JsonNode record : records) if (value.equals(record.path(field).asText())) {
            if (found != null) throw new MetadataFailure("iam", "ambiguous-relation");
            found = record;
        }
        if (found == null) throw new MetadataFailure("iam", missingReason);
        return found;
    }

    private void observe(String family, int revision, long age, long ttl, long max,
                         String cacheState, String result, String reason, JsonNode active, Instant fetched, long started) {
        if (telemetry == null) return;
        telemetry.metadata(family, revision, age, ttl, max, cacheState, result, reason,
                Instant.parse(active.required("activatedAt").textValue()), clock.instant(), fetched);
        telemetry.decision("metadata." + family, "metadata_validation", result, reason);
        if (result.equals("deny")) telemetry.audit("metadata." + family, result, reason);
        telemetry.dependency(family, result.equals("allow") ? "success" : "error",
                java.time.Duration.ofNanos(System.nanoTime() - started).toMillis());
    }

    public static final class MetadataFailure extends IllegalArgumentException {
        private final String family;
        private final String reason;
        public MetadataFailure(String family, String reason) { super(family + ":" + reason); this.family = family; this.reason = reason; }
        public String family() { return family; }
        public String reason() { return reason; }
    }

    public boolean membershipActive(String organizationId, String role) {
        return checkedMember(readAndValidate("membership"), organizationId, role);
    }

    public boolean bothMembershipsActive() {
        JsonNode membership = readAndValidate("membership");
        boolean consumer = checkedMember(membership, ExperimentConfig.ORGANIZATION_A, "consumer");
        boolean producer = checkedMember(membership, ExperimentConfig.ORGANIZATION_B, "producer");
        return consumer && producer;
    }

    private boolean checkedMember(JsonNode membership, String organization, String role) {
        boolean active = activeMember(membership, organization, role);
        if (telemetry != null) telemetry.decision("membership." + role, "membership_validation",
                active ? "allow" : "deny", active ? "active" : role + "-inactive");
        return active;
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
