package se.skltpnext.experiment001.metadata;

import se.skltpnext.experiment001.evidence.JsonSupport;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Local metadata time shared by CLI and listeners; never substitutes for HTTP elapsed time. */
public final class MutableExperimentClock {
    private final Path file;
    public MutableExperimentClock(Path root) { file = root.resolve("metadata/clock.json"); }
    public Instant instant() {
        try { return Instant.parse(JsonSupport.MAPPER.readTree(file.toFile()).required("instant").textValue()); }
        catch (java.io.IOException e) { throw new IllegalStateException("Cannot read experiment clock", e); }
    }
    public void reset(Instant instant) { JsonSupport.writeJson(file, Map.of("instant", instant.toString())); }
    public void advance(Duration duration) {
        if (duration.isNegative()) throw new IllegalArgumentException("Clock must be monotonic within scenario");
        reset(instant().plus(duration));
    }
}
