package se.skltpnext.experiment001.evidence;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Safe observations only. Raw HTTP bodies and library error messages never enter this channel. */
public final class PhaseFourEvents {
    private PhaseFourEvents() { }
    public static void record(Path runtime, String run, String scenario, String variant,
                              String kind, Map<String, Object> values) {
        var event = new LinkedHashMap<String, Object>();
        event.put("runId", run); event.put("scenarioId", scenario); event.put("variantId", variant);
        event.put("kind", kind); event.put("atEpochMillis", System.currentTimeMillis());
        event.putAll(values);
        JsonSupport.validateResource("experiment-001/schemas/observation-phase-4.schema.json",
                JsonSupport.MAPPER.valueToTree(event), "Phase 4 observation");
        JsonSupport.appendJsonLine(runtime.resolve("events/phase-4/observations.jsonl"), event);
    }
}
