package se.skltpnext.experiment001.authorization;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.evidence.JsonSupport;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

public final class TlsMaterial {
    private TlsMaterial() {
    }

    public static void generate(Path runtimeRoot) {
        Path privateDir = runtimeRoot.resolve("private");
        Path publicDir = runtimeRoot.resolve("public-trust");
        try {
            Files.createDirectories(privateDir);
            Files.createDirectories(publicDir);
            byte[] passwordBytes = new byte[24];
            new SecureRandom().nextBytes(passwordBytes);
            String password = Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes);
            JsonSupport.writeJson(privateDir.resolve("tls-passwords.json"), Map.of("storePassword", password));

            Path caStore = privateDir.resolve("ca.p12");
            Path caCert = publicDir.resolve("ca.pem");
            keytool("-genkeypair", "-alias", "ca", "-dname",
                    "CN=SKLTP Next Experiment 001 Local Test CA", "-keyalg", "EC",
                    "-groupname", "secp256r1", "-sigalg", "SHA256withECDSA",
                    "-validity", "2", "-ext", "bc=ca:true", "-keystore", caStore.toString(),
                    "-storetype", "PKCS12", "-storepass", password, "-keypass", password, "-noprompt");
            keytool("-exportcert", "-alias", "ca", "-keystore", caStore.toString(),
                    "-storepass", password, "-rfc", "-file", caCert.toString());

            issueServer(privateDir, caStore, caCert, "as", password);
            issueServer(privateDir, caStore, caCert, "producer", password);

            Path trustStore = privateDir.resolve("truststore.p12");
            keytool("-importcert", "-alias", "experiment-ca", "-file", caCert.toString(),
                    "-keystore", trustStore.toString(), "-storetype", "PKCS12",
                    "-storepass", password, "-noprompt");
            JsonSupport.writeJson(publicDir.resolve("tls-fingerprints.json"), Map.of(
                    "caSha256", certificateFingerprint(caCert),
                    "asSha256", storeCertificateFingerprint(privateDir.resolve("as.p12"), "as", password),
                    "producerSha256", storeCertificateFingerprint(privateDir.resolve("producer.p12"), "producer", password)));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot generate per-run TLS material", e);
        }
    }

    private static void issueServer(Path privateDir, Path caStore, Path caCert,
                                    String alias, String password) {
        Path store = privateDir.resolve(alias + ".p12");
        Path csr = privateDir.resolve(alias + ".csr");
        Path signed = privateDir.resolve(alias + "-signed.pem");
        keytool("-genkeypair", "-alias", alias, "-dname", "CN=localhost",
                "-keyalg", "EC", "-groupname", "secp256r1", "-sigalg", "SHA256withECDSA",
                "-validity", "2", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", password, "-keypass", password, "-noprompt");
        keytool("-certreq", "-alias", alias, "-keystore", store.toString(),
                "-storepass", password, "-file", csr.toString(),
                "-ext", "SAN=dns:localhost,ip:127.0.0.1");
        keytool("-gencert", "-alias", "ca", "-keystore", caStore.toString(),
                "-storepass", password, "-infile", csr.toString(), "-outfile", signed.toString(),
                "-rfc", "-validity", "2", "-ext", "KU=digitalSignature",
                "-ext", "EKU=serverAuth", "-ext", "SAN=dns:localhost,ip:127.0.0.1");
        keytool("-importcert", "-alias", "experiment-ca", "-file", caCert.toString(),
                "-keystore", store.toString(), "-storepass", password, "-noprompt");
        keytool("-importcert", "-alias", alias, "-file", signed.toString(),
                "-keystore", store.toString(), "-storepass", password, "-noprompt");
    }

    private static void keytool(String... args) {
        String executable = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        String[] command = new String[args.length + 1];
        command[0] = executable;
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (process.waitFor() != 0) {
                throw new IllegalStateException("keytool failed while generating synthetic TLS material");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start pinned JDK keytool", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running keytool", e);
        }
    }

    public static SSLContext serverContext(Path runtimeRoot, String alias) {
        try {
            char[] password = password(runtimeRoot);
            KeyStore store = loadStore(runtimeRoot.resolve("private/" + alias + ".p12"), password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(store, password);
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(kmf.getKeyManagers(), null, null);
            return context;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot create server SSLContext", e);
        }
    }

    public static SSLContext clientContext(Path runtimeRoot) {
        try {
            char[] password = password(runtimeRoot);
            KeyStore store = loadStore(runtimeRoot.resolve("private/truststore.p12"), password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(null, tmf.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot create client SSLContext", e);
        }
    }

    private static KeyStore loadStore(Path path, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(path)) {
            store.load(input, password);
        }
        return store;
    }

    private static char[] password(Path runtimeRoot) throws IOException {
        JsonNode node = JsonSupport.MAPPER.readTree(runtimeRoot.resolve("private/tls-passwords.json").toFile());
        return node.required("storePassword").textValue().toCharArray();
    }

    private static String certificateFingerprint(Path pem) {
        try (InputStream input = Files.newInputStream(pem)) {
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(input);
            return JsonSupport.sha256(certificate.getEncoded());
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String storeCertificateFingerprint(Path storePath, String alias, String password) {
        try {
            KeyStore store = loadStore(storePath, password.toCharArray());
            return JsonSupport.sha256(store.getCertificate(alias).getEncoded());
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
