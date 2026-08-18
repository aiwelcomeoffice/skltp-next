package se.skltpnext.experiment001.cli;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.AuthorizationServerDouble;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.metadata.MetadataStores;
import se.skltpnext.experiment001.producer.ProducerDouble;
import se.skltpnext.experiment001.telemetry.TelemetryRecorder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public final class RuntimeEnvironment {
    private final Path runtimeRoot;
    private final String runId;

    public RuntimeEnvironment(Path runtimeRoot, String runId) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
    }

    public void serve() {
        HttpsServer authorizationServer = null;
        HttpsServer producerServer = null;
        try {
            CryptoMaterial material = CryptoMaterial.load(runtimeRoot);
            authorizationServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            producerServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            URI tokenEndpoint = URI.create("https://localhost:"
                    + authorizationServer.getAddress().getPort() + "/token");
            URI producerEndpoint = URI.create("https://localhost:"
                    + producerServer.getAddress().getPort());

            MetadataStores.writeBaseline(runtimeRoot, tokenEndpoint, producerEndpoint, material);
            new MetadataStores(runtimeRoot).discover();
            AuthorizationServerDouble authorization = new AuthorizationServerDouble(
                    runtimeRoot, runId, tokenEndpoint, material);
            ProducerDouble producer = new ProducerDouble(runtimeRoot, runId, producerEndpoint);
            String warmUpToken = authorization.warmUpSecurityLibraries();
            producer.warmUpSecurityLibraries(warmUpToken, material.dpopKey());
            producer.warmUpContractValidation();
            try (TelemetryRecorder telemetry = new TelemetryRecorder(
                    runtimeRoot, runId, "runtime-warmup", "baseline", "runtime")) {
                telemetry.warmUpWithoutEvidence();
            }

            authorizationServer.setHttpsConfigurator(new HttpsConfigurator(
                    TlsMaterial.serverContext(runtimeRoot, "as")));
            producerServer.setHttpsConfigurator(new HttpsConfigurator(
                    TlsMaterial.serverContext(runtimeRoot, "producer")));
            authorizationServer.setExecutor(Executors.newFixedThreadPool(2));
            producerServer.setExecutor(Executors.newFixedThreadPool(2));
            authorizationServer.createContext("/token", authorization);
            producerServer.createContext("/synthetic-records", producer);
            authorizationServer.createContext("/ready", exchange -> ready(exchange, "authorization-server"));
            producerServer.createContext("/ready", exchange -> ready(exchange, "producer"));
            authorizationServer.createContext("/__reset", exchange -> {
                authorization.reset();
                ready(exchange, "authorization-server-reset");
            });
            producerServer.createContext("/__reset", exchange -> {
                producer.reset();
                ready(exchange, "producer-reset");
            });
            authorizationServer.start();
            producerServer.start();

            JsonSupport.writeJson(runtimeRoot.resolve("environment.json"), Map.of(
                    "runId", runId,
                    "pid", ProcessHandle.current().pid(),
                    "status", "ready",
                    "authorizationServerEndpoint", tokenEndpoint.toString(),
                    "producerEndpoint", producerEndpoint.toString(),
                    "authorizationListenerId", "AS-LISTENER",
                    "producerListenerId", "PRODUCER-ENDPOINT-REV-1"));

            HttpsServer finalAuthorizationServer = authorizationServer;
            HttpsServer finalProducerServer = producerServer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                finalAuthorizationServer.stop(0);
                finalProducerServer.stop(0);
            }, "experiment-001-shutdown"));
            new CountDownLatch(1).await();
        } catch (Exception e) {
            if (authorizationServer != null) {
                authorizationServer.stop(0);
            }
            if (producerServer != null) {
                producerServer.stop(0);
            }
            throw new IllegalStateException("Experiment environment failed", e);
        }
    }

    public static EnvironmentInfo read(Path runtimeRoot) {
        try {
            var node = JsonSupport.MAPPER.readTree(runtimeRoot.resolve("environment.json").toFile());
            return new EnvironmentInfo(
                    node.required("runId").textValue(),
                    node.required("pid").longValue(),
                    URI.create(node.required("authorizationServerEndpoint").textValue()),
                    URI.create(node.required("producerEndpoint").textValue()),
                    node.required("status").textValue());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read environment state", e);
        }
    }

    private static void ready(com.sun.net.httpserver.HttpExchange exchange, String component)
            throws IOException {
        byte[] body = ("{\"component\":\"" + component + "\",\"status\":\"ready\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public record EnvironmentInfo(
            String runId, long pid, URI authorizationServerEndpoint,
            URI producerEndpoint, String status) {
    }
}
