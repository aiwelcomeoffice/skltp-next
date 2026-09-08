package se.skltpnext.experiment001.telemetry;

import se.skltpnext.experiment001.evidence.JsonSupport;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Independent audit writer and schema. Trace context is deliberately absent from its input. */
public final class AuditRecorder {
    public String write(Path root, Map<String, Object> decision, String decisionRef) {
        Map<String, Object> audit = new LinkedHashMap<>(decision);
        String id = "AUDIT-" + UUID.randomUUID();
        audit.put("auditRecordId", id); audit.put("decisionRef", decisionRef);
        audit.put("organizationRef", "ORG_A"); audit.put("systemRef", "SYSTEM_A"); audit.put("clientRef", "CLIENT_A");
        audit.put("recordedAt", Instant.now().toString());
        JsonSupport.validateResource("experiment-001/schemas/audit-phase-5.schema.json",
                JsonSupport.MAPPER.valueToTree(audit), "OBS audit");
        JsonSupport.appendJsonLine(root.resolve("events/audit/observations.jsonl"), audit);
        return id;
    }
}
