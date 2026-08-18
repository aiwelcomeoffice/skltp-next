package se.skltpnext.experiment001.authorization;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.evidence.EvidenceCollector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpsTrustConformanceTest {
    @Test
    void generatedTrustAnchorAllowsTheServerWhileDefaultTrustRejectsIt() throws Exception {
        Path runtime = Path.of("target/experiment-001/test",
                "https-trust-" + UUID.randomUUID());
        HttpsServer server = null;
        try {
            TlsMaterial.generate(runtime);
            server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(
                    TlsMaterial.serverContext(runtime, "producer")));
            server.createContext("/ready", exchange -> {
                byte[] body = "ready".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            URI endpoint = URI.create("https://localhost:" + server.getAddress().getPort() + "/ready");
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(3)).GET().build();

            HttpClient trusted = HttpClient.newBuilder()
                    .sslContext(TlsMaterial.clientContext(runtime)).build();
            assertEquals(200, trusted.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());

            HttpClient untrusted = HttpClient.newHttpClient();
            assertThrows(IOException.class,
                    () -> untrusted.send(request, HttpResponse.BodyHandlers.discarding()));
        } finally {
            if (server != null) {
                server.stop(0);
            }
            if (Files.exists(runtime)) {
                EvidenceCollector.deleteTree(runtime);
            }
        }
    }
}
