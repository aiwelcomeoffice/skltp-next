package se.skltpnext.experiment001.authorization;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import se.skltpnext.experiment001.evidence.CanaryRegistry;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.ParseException;
import java.util.Map;
import java.util.Set;

public record CryptoMaterial(
        ECKey metadataKey,
        ECKey clientAuthenticationKey,
        ECKey authorizationServerSigningKey,
        ECKey dpopKey) {

    public static CryptoMaterial generate(Path runtimeRoot) {
        Path privateDir = runtimeRoot.resolve("private");
        Path publicDir = runtimeRoot.resolve("public-trust");
        try {
            Files.createDirectories(privateDir);
            Files.createDirectories(publicDir);
            ownerOnlyDirectory(privateDir);
            CryptoMaterial material = new CryptoMaterial(
                    key("metadata-provenance"),
                    key("client-authentication"),
                    key("authorization-server-signing"),
                    key("dpop"));
            writePrivate(privateDir.resolve("metadata.jwk.json"), material.metadataKey);
            writePrivate(privateDir.resolve("client-authentication.jwk.json"), material.clientAuthenticationKey);
            writePrivate(privateDir.resolve("authorization-server-signing.jwk.json"), material.authorizationServerSigningKey);
            writePrivate(privateDir.resolve("dpop.jwk.json"), material.dpopKey);
            JsonSupport.writeJson(publicDir.resolve("metadata.jwk.json"),
                    JsonSupport.MAPPER.readTree(material.metadataKey.toPublicJWK().toJSONString()));
            CanaryRegistry canaries = new CanaryRegistry(privateDir);
            canaries.register("private_key", material.clientAuthenticationKey.toJSONString());
            return material;
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException("Cannot generate per-run JOSE material", e);
        }
    }

    public static CryptoMaterial load(Path runtimeRoot) {
        Path privateDir = runtimeRoot.resolve("private");
        return new CryptoMaterial(
                readKey(privateDir.resolve("metadata.jwk.json")),
                readKey(privateDir.resolve("client-authentication.jwk.json")),
                readKey(privateDir.resolve("authorization-server-signing.jwk.json")),
                readKey(privateDir.resolve("dpop.jwk.json")));
    }

    private static ECKey key(String id) throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).keyID("E001-" + id).generate();
    }

    private static void writePrivate(Path path, ECKey key) throws IOException {
        Files.writeString(path, key.toJSONString(), StandardCharsets.UTF_8);
        ownerOnlyFile(path);
    }

    private static ECKey readKey(Path path) {
        try {
            return ECKey.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("Cannot load private runtime key", e);
        }
    }

    public Map<String, String> publicFingerprints() {
        try {
            return Map.of(
                    "metadata", metadataKey.computeThumbprint().toString(),
                    "clientAuthentication", clientAuthenticationKey.computeThumbprint().toString(),
                    "authorizationServerSigning", authorizationServerSigningKey.computeThumbprint().toString(),
                    "dpop", dpopKey.computeThumbprint().toString());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void ownerOnlyDirectory(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // The module can be on a non-POSIX filesystem; target remains ignored.
        }
    }

    private static void ownerOnlyFile(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // See ownerOnlyDirectory.
        }
    }
}

