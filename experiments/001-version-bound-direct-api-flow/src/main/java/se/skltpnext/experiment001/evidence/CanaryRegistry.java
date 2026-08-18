package se.skltpnext.experiment001.evidence;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public final class CanaryRegistry {
    private final Path file;

    public CanaryRegistry(Path privateDirectory) {
        this.file = privateDirectory.resolve("canaries.jsonl");
    }

    public String newValue(String type) {
        String value = "E001-CANARY-" + type.toUpperCase().replace('_', '-') + "-" + UUID.randomUUID();
        register(type, value);
        return value;
    }

    public void register(String type, String value) {
        JsonSupport.appendJsonLine(file, Map.of(
                "canaryId", "CANARY-" + type.toUpperCase().replace('_', '-'),
                "type", type,
                "value", value));
    }
}

