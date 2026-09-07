package se.skltpnext.experiment001.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import se.skltpnext.experiment001.authorization.CryptoMaterial;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class MetadataCacheTest {
    @TempDir Path root;
    private MetadataStores prepare() {
        var keys = CryptoMaterial.generate(root);
        MetadataStores.writeBaseline(root, URI.create("https://localhost:18443/token"), URI.create("https://localhost:19443"), keys);
        return new MetadataStores(root);
    }
    @ParameterizedTest @ValueSource(strings = {"service", "membership", "iam"})
    void eachFamilyEnforcesMaxStalenessBoundaryAndRetainsHighWatermark(String family) {
        var stores = prepare();
        var clock = new MutableExperimentClock(root);
        stores.readAndValidate(family);
        stores.sourceAvailable(family, false);
        long max = family.equals("service") ? 60000 : 30000;
        clock.advance(Duration.ofMillis(max - 1));
        assertDoesNotThrow(() -> stores.readAndValidate(family));
        clock.advance(Duration.ofMillis(1));
        assertDoesNotThrow(() -> stores.readAndValidate(family));
        clock.advance(Duration.ofMillis(1));
        assertEquals("stale", assertThrows(MetadataStores.MetadataFailure.class,
                () -> stores.readAndValidate(family)).reason());
        stores.sourceAvailable(family, true);
        assertEquals("stale", assertThrows(MetadataStores.MetadataFailure.class,
                () -> stores.readAndValidate(family)).reason(), "refetching old bytes cannot renew their age");
    }
    @ParameterizedTest @ValueSource(strings = {"service", "membership", "iam"})
    void rollbackPersistsAcrossReaderRecreationAndOtherAuthorityCannotSign(String family) {
        var stores = prepare();
        stores.readAndValidate(family);
        var next = stores.nextRevision(family);
        MetadataStores.writeSigned(root, family, next, CryptoMaterial.metadataSigningKey(root, family));
        stores.activate(family, 2);
        assertEquals(2, stores.readAndValidate(family).path("revision").asInt());
        stores.activate(family, 1);
        assertEquals("rollback", assertThrows(MetadataStores.MetadataFailure.class,
                () -> new MetadataStores(root).readAndValidate(family)).reason());
        stores.activate(family, 2);
        next = stores.nextRevision(family);
        String other = family.equals("service") ? "iam" : "service";
        MetadataStores.writeSigned(root, family, next, CryptoMaterial.metadataSigningKey(root, other));
        stores.activate(family, 3);
        assertEquals("integrity", assertThrows(MetadataStores.MetadataFailure.class,
                () -> stores.readAndValidate(family)).reason());
    }
    @Test void clockCannotMoveBackWithinScenario() {
        prepare();
        assertThrows(IllegalArgumentException.class, () -> new MutableExperimentClock(root).advance(Duration.ofMillis(-1)));
    }
}
