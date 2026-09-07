package se.skltpnext.experiment001.scenario;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.util.Base64URL;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import java.nio.file.Files;
import java.nio.file.Path;

/** Synthetic mutations only. This class neither makes decisions nor assigns scenario outcomes. */
final class MetadataFaultFixtures {
    static void apply(Path root, MetadataStores stores, String variant) {
        String[] parts = variant.split("-", 2);
        String family = parts[0];
        String fault = parts[1];
        ObjectNode next = stores.nextRevision(family);
        var key = CryptoMaterial.metadataSigningKey(root, family);
        switch (fault) {
            case "cross-context" -> next.put("context", "E001-OTHER-CONTEXT");
            case "wrong-metadata-issuer" -> next.put("issuer", ExperimentConfig.METADATA_ISSUER + ":other-authority");
            case "wrong-key" -> key = CryptoMaterial.metadataSigningKey(root, family.equals("service") ? "iam" : "service");
            case "unknown-organization" -> next.put("organizationId", "E001-UNKNOWN-ORG");
            case "unknown-system" -> next.put("systemId", "E001-UNKNOWN-SYSTEM");
            case "unknown-client" -> next.put("clientId", "E001-UNKNOWN-CLIENT");
            case "client-wrong-system" -> ((ObjectNode) next.required("clients").get(0)).put("systemId", "E001-SYSTEM-B");
            case "system-wrong-organization" -> ((ObjectNode) next.required("systems").get(0)).put("organizationId", ExperimentConfig.ORGANIZATION_B);
            case "client-wrong-key" -> next.set("clientAuthenticationJwk", next.required("dpopJwk").deepCopy());
            case "manipulated", "rollback" -> { }
            default -> throw new IllegalArgumentException("Unknown metadata fault fixture");
        }
        MetadataStores.writeSigned(root, family, next, key);
        stores.activate(family, 2);
        if (fault.equals("rollback")) {
            stores.readAndValidate(family); // Persist the accepted high-water mark before replaying rev 1.
            stores.activate(family, 1);
        }
        if (fault.equals("manipulated")) {
            Path file = root.resolve("metadata/" + family + "-rev-2.jws.json");
            try {
                ObjectNode envelope = (ObjectNode) JsonSupport.MAPPER.readTree(file.toFile());
                next.put("expiresAt", "2099-01-01T00:00:00Z");
                envelope.put("payload", Base64URL.encode(JsonSupport.compact(next)).toString());
                JsonSupport.writeJson(file, envelope); // Deliberately do not re-sign.
            } catch (java.io.IOException e) { throw new IllegalStateException("Cannot mutate metadata fixture", e); }
        }
    }
}
