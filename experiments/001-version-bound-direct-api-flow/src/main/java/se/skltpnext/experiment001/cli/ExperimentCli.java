package se.skltpnext.experiment001.cli;

import com.fasterxml.jackson.databind.node.ObjectNode;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import se.skltpnext.experiment001.authorization.TlsMaterial;
import se.skltpnext.experiment001.contract.ConformanceGates;
import se.skltpnext.experiment001.contract.ContractValidators;
import se.skltpnext.experiment001.evidence.EvidenceCollector;
import se.skltpnext.experiment001.evidence.JsonSupport;
import se.skltpnext.experiment001.release.ReleaseValidator;
import se.skltpnext.experiment001.scenario.ScenarioEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ExperimentCli {
    private ExperimentCli() {
    }

    public static void main(String[] args) {
        int exit;
        try {
            exit = execute(args);
        } catch (IllegalArgumentException e) {
            System.err.println("experiment-001: invalid command or argument");
            exit = 64;
        } catch (Exception e) {
            System.err.println("experiment-001: command failed; inspect safe generated evidence");
            exit = 70;
        }
        System.exit(exit);
    }

    static int execute(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("command required");
        }
        String command = args[0];
        Map<String, String> options = options(Arrays.copyOfRange(args, 1, args.length));
        return switch (command) {
            case "verify-prerequisites" -> verifyPrerequisites();
            case "prepare-fixtures" -> prepareFixtures(options);
            case "validate" -> validate(options);
            case "start-environment" -> startEnvironment(options);
            case "check-readiness" -> checkReadiness(options);
            case "reset-test-state" -> resetTestState(options);
            case "run-scenario" -> runScenario(options);
            case "collect-evidence" -> collectEvidence(options);
            case "validate-evidence" -> validateEvidence(options);
            case "stop-environment" -> stopEnvironment(options);
            default -> throw new IllegalArgumentException("Unknown Phase 1 command");
        };
    }

    private static int verifyPrerequisites() {
        boolean java = "25.0.4+7-LTS".equals(System.getProperty("java.runtime.version"))
                && System.getProperty("java.vendor", "").contains("Eclipse Adoptium");
        boolean linuxAmd64 = "Linux".equals(System.getProperty("os.name"))
                && "amd64".equals(System.getProperty("os.arch"));
        boolean wrapper = readText(Path.of(".mvn/wrapper/maven-wrapper.properties"))
                .contains("apache-maven-3.9.16-bin.zip")
                && readText(Path.of(".mvn/wrapper/maven-wrapper.properties"))
                .contains("wrapperVersion=3.3.4");
        boolean noSourcePrivate = noSourcePrivateMaterial();
        boolean loopback = loopbackAvailable();
        boolean pass = java && linuxAmd64 && wrapper && noSourcePrivate && loopback;
        System.out.println("prerequisite-checks: runtime=" + outcome(java)
                + ", platform=" + outcome(linuxAmd64)
                + ", wrapper=" + outcome(wrapper)
                + ", source-private-scan=" + outcome(noSourcePrivate)
                + ", loopback=" + outcome(loopback));
        System.out.println(pass ? "prerequisites: pass" : "prerequisites: inconclusive");
        return pass ? 0 : 2;
    }

    private static int prepareFixtures(Map<String, String> options) {
        require(options, "release", "1.0.0");
        require(options, "parameters", "1.0.0");
        String runId = required(options, "run-id");
        Path runtime = ExperimentConfig.runtimeRoot(runId);
        if (Files.exists(runtime)) {
            RuntimeEnvironment.EnvironmentInfo existing = safeEnvironment(runtime);
            if (existing != null && ProcessHandle.of(existing.pid()).map(ProcessHandle::isAlive).orElse(false)) {
                throw new IllegalStateException("Run is active");
            }
            try {
                EvidenceCollector.deleteTree(runtime);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot reset generated runtime state", e);
            }
        }
        new ReleaseValidator().validate();
        CryptoMaterial material = CryptoMaterial.generate(runtime);
        TlsMaterial.generate(runtime);
        JsonSupport.writeJson(runtime.resolve("runtime-fixture.json"), Map.of(
                "runId", runId,
                "releaseVersion", "1.0.0",
                "parameterVersion", "1.0.0",
                "status", "prepared",
                "privateState", "target-only",
                "publicJwkFingerprints", material.publicFingerprints()));
        System.out.println("fixtures: prepared");
        return 0;
    }

    private static int validate(Map<String, String> options) {
        String runId = required(options, "run-id");
        Path runtime = ExperimentConfig.runtimeRoot(runId);
        if (!Files.isDirectory(runtime.resolve("private"))) {
            throw new IllegalStateException("Fixtures are not prepared");
        }
        new ReleaseValidator().validate();
        System.out.println("release-validation: pass");
        try {
            new ContractValidators().validateStructure();
        } catch (IllegalArgumentException e) {
            System.out.println("contract-structure: inconclusive (" + safeDiagnostic(e.getMessage()) + ")");
            return 2;
        }
        System.out.println("contract-structure: pass");
        ConformanceGates.GateReport gates = new ConformanceGates().run();
        JsonSupport.writeJson(runtime.resolve("validation/tool-gates.json"), gates);
        System.out.println("tool-gates: " + gates.status());
        return gates.passed() ? 0 : 2;
    }

    private static int startEnvironment(Map<String, String> options) {
        String runId = required(options, "run-id");
        Path runtime = ExperimentConfig.runtimeRoot(runId);
        if (!Files.isDirectory(runtime.resolve("private"))) {
            throw new IllegalStateException("Fixtures are not prepared");
        }
        System.out.println("environment: starting");
        new RuntimeEnvironment(runtime, runId).serve();
        return 0;
    }

    private static int checkReadiness(Map<String, String> options) {
        String runId = required(options, "run-id");
        boolean ready = new ScenarioEngine(ExperimentConfig.runtimeRoot(runId), runId).ready();
        System.out.println(ready ? "readiness: pass" : "readiness: not-ready");
        return ready ? 0 : 2;
    }

    private static int resetTestState(Map<String, String> options) {
        String runId = required(options, "run-id");
        String scenario = required(options, "scenario");
        String variant = required(options, "variant");
        if (!ExperimentConfig.PHASE_1_VARIANTS.contains(scenario + "/" + variant)) {
            throw new IllegalArgumentException("Not a Phase 1 scenario/variant");
        }
        new ScenarioEngine(ExperimentConfig.runtimeRoot(runId), runId)
                .resetForScenario(scenario, variant);
        System.out.println("state-reset: pass");
        return 0;
    }

    private static int runScenario(Map<String, String> options) {
        String runId = required(options, "run-id");
        var result = new ScenarioEngine(ExperimentConfig.runtimeRoot(runId), runId)
                .run(required(options, "scenario"), required(options, "variant"));
        System.out.println(result.scenarioId() + "/" + result.variantId() + ": " + result.status());
        return result.passed() ? 0 : 2;
    }

    private static int collectEvidence(Map<String, String> options) {
        String runId = required(options, "run-id");
        var result = new EvidenceCollector(ExperimentConfig.runtimeRoot(runId),
                ExperimentConfig.evidenceRoot(runId), runId).collect();
        System.out.println("evidence: " + result.status());
        return "pass".equals(result.status()) ? 0 : 2;
    }

    private static int validateEvidence(Map<String, String> options) {
        String runId = required(options, "run-id");
        var result = new EvidenceCollector(ExperimentConfig.runtimeRoot(runId),
                ExperimentConfig.evidenceRoot(runId), runId).validate();
        System.out.println("evidence-validation: " + result.status());
        return result.passed() ? 0 : 2;
    }

    private static int stopEnvironment(Map<String, String> options) {
        String runId = required(options, "run-id");
        Path runtime = ExperimentConfig.runtimeRoot(runId);
        RuntimeEnvironment.EnvironmentInfo environment = safeEnvironment(runtime);
        if (environment != null) {
            ProcessHandle.of(environment.pid()).filter(ProcessHandle::isAlive).ifPresent(handle -> {
                String command = handle.info().commandLine().orElse("");
                if (!command.contains("experiment-001-cli.jar")) {
                    throw new IllegalStateException("PID does not match the experiment CLI");
                }
                handle.destroy();
                try {
                    handle.onExit().get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    handle.destroyForcibly();
                }
            });
        }
        Path privateDir = runtime.resolve("private");
        if (Files.exists(privateDir)) {
            try {
                EvidenceCollector.deleteTree(privateDir);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot remove private generated state", e);
            }
        }
        System.out.println("environment: stopped; private state removed");
        return 0;
    }

    private static RuntimeEnvironment.EnvironmentInfo safeEnvironment(Path runtime) {
        try {
            return Files.isRegularFile(runtime.resolve("environment.json"))
                    ? RuntimeEnvironment.read(runtime) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Map<String, String> options(String[] args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Options must be --name value pairs");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (!args[i].startsWith("--") || result.put(args[i].substring(2), args[i + 1]) != null) {
                throw new IllegalArgumentException("Invalid or duplicate option");
            }
        }
        return result;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing --" + name);
        }
        return value;
    }

    private static void require(Map<String, String> options, String name, String expected) {
        if (!expected.equals(required(options, name))) {
            throw new IllegalArgumentException("Unsupported " + name);
        }
    }

    private static boolean noSourcePrivateMaterial() {
        try (var stream = Files.walk(Path.of("src"))) {
            return stream.filter(Files::isRegularFile).noneMatch(path ->
                    path.getFileName().toString().matches("(?i).*(\\.p12|\\.jks|private.*\\.jwk|passwords).*"));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean loopbackAvailable() {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return socket.isBound();
        } catch (IOException e) {
            return false;
        }
    }

    private static String readText(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    private static String outcome(boolean value) {
        return value ? "pass" : "fail";
    }

    private static String safeDiagnostic(String value) {
        if (value == null) {
            return "validator-error";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9 .:_,-]", "?");
        return sanitized.substring(0, Math.min(160, sanitized.length()));
    }
}
