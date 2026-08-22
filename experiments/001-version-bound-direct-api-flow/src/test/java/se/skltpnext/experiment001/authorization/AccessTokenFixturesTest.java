package se.skltpnext.experiment001.authorization;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.skltpnext.experiment001.evidence.EvidenceCollector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenFixturesTest {
    private static final String ISSUER = "https://localhost:18443/";
    private static Path runtime;
    private static AccessTokenValidator validator;

    @BeforeAll
    static void prepareKeys() {
        runtime = Path.of("target/experiment-001/test/token-fixtures-" + UUID.randomUUID());
        CryptoMaterial material = CryptoMaterial.generate(runtime);
        validator = new AccessTokenValidator(ISSUER,
                material.authorizationServerSigningKey().toPublicJWK());
    }

    @AfterAll
    static void removeKeys() throws Exception {
        if (runtime != null && Files.exists(runtime)) {
            EvidenceCollector.deleteTree(runtime);
        }
    }

    @Test
    void variantRegistryIsExactlyTheElevenSpecifiedTokVariants() {
        assertEquals(Set.of(
                "missing", "wrong-issuer", "wrong-audience", "bad-signature",
                "disallowed-algorithm", "wrong-type", "expired", "not-yet-valid",
                "missing-required-claim", "wrong-client-id", "wrong-sub"),
                AccessTokenFixtures.VARIANTS);
    }

    @Test
    void missingVariantDoesNotMaterializeOrMisclassifyAToken() {
        var fixture = AccessTokenFixtures.fixture(runtime, ISSUER, "missing");

        assertFalse(fixture.presented());
        assertNull(fixture.serialized());
        assertThrows(IllegalStateException.class, fixture::bearerAuthorizationHeader);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "wrong-issuer",
            "wrong-audience",
            "bad-signature",
            "disallowed-algorithm",
            "wrong-type",
            "expired",
            "not-yet-valid",
            "missing-required-claim",
            "wrong-client-id",
            "wrong-sub"
    })
    void everyPresentedVariantHasExactlyItsNamedValidationFault(String variant) {
        var fixture = AccessTokenFixtures.fixture(runtime, ISSUER, variant);

        assertTrue(fixture.presented());
        var failure = assertThrows(AccessTokenValidator.TokenValidationException.class,
                () -> validator.validate(fixture.serialized()));
        assertEquals(variant, failure.safeReason());
    }
}
