package se.skltpnext.experiment001.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import se.skltpnext.experiment001.evidence.JsonSupport;
import java.nio.file.*;
import java.util.*;

/** Explicit per-runtime OBS capture, installed only while an isolated stimulus is active. */
public final class ObservationContext {
    public static JsonNode read(Path root) {
        try {
            Path file = root.resolve("private/observation-context.json");
            return Files.isRegularFile(file) ? JsonSupport.MAPPER.readTree(Files.readAllBytes(file)) : null;
        } catch (Exception e) { throw new IllegalStateException("Cannot read observation context", e); }
    }

    public static void externalResponse(Path root, int status, String body, String challenge) throws Exception {
        if (status >= 400 && read(root) != null) Files.writeString(root.resolve("events/errors/http-response.txt"),
                status + "\n" + body + "\n" + challenge + "\n", StandardOpenOption.APPEND);
    }

    public static void guard(Path root, Map<String, Object> row) {
        if (read(root) == null) return;
        try {
            String text = JsonSupport.compact(row);
            for (String line : Files.readAllLines(root.resolve("private/canaries.jsonl"))) {
                if (line.isBlank()) continue;
                var canary = JsonSupport.MAPPER.readTree(line);
                for (String value : se.skltpnext.experiment001.evidence.ObservationScanner.representations(canary.required("value").asText()))
                    if (text.contains(value)) throw new IllegalArgumentException("Canary rejected by safe writer");
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read safe-writer registry", e); }
        catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Cannot validate safe writer input", e); }
    }

    public static Map<String, Object> common(JsonNode context, String run, String actor,
                                             String checkpoint, String category, String result, String reason,
                                             String policy) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("runId", run); row.put("scenarioId", context.required("scenarioId").asText());
        row.put("variantId", "baseline"); row.put("stimulusRef", context.required("stimulusRef").asText());
        row.put("sourceScenarioId", context.required("sourceScenarioId").asText());
        row.put("sourceVariantId", context.required("sourceVariantId").asText());
        row.put("actor", actor); row.put("checkpoint", checkpoint); row.put("category", category);
        row.put("result", result); row.put("reason", reason);
        row.put("releaseId", se.skltpnext.experiment001.ExperimentConfig.RELEASE_ID);
        row.put("releaseVersion", se.skltpnext.experiment001.ExperimentConfig.RELEASE_VERSION);
        row.put("profileVersion", "1.0.0"); row.put("policyVersion", policy);
        return row;
    }
}
