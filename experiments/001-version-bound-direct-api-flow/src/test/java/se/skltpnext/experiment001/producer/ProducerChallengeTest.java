package se.skltpnext.experiment001.producer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProducerChallengeTest {
    @Test
    void missingBearerChallengeNeverClaimsInvalidToken() {
        assertEquals("Bearer realm=\"experiment-001\"",
                ProducerDouble.MISSING_BEARER_CHALLENGE);
        assertFalse(ProducerDouble.MISSING_BEARER_CHALLENGE.contains("invalid_token"));
    }

    @Test
    void presentedInvalidTokenAndInsufficientScopeRemainDistinct() {
        assertTrue(ProducerDouble.INVALID_BEARER_CHALLENGE.contains("error=\"invalid_token\""));
        assertTrue(ProducerDouble.INSUFFICIENT_SCOPE_CHALLENGE
                .contains("error=\"insufficient_scope\""));
        assertTrue(ProducerDouble.INSUFFICIENT_SCOPE_CHALLENGE
                .contains("scope=\"synthetic.read\""));
        assertTrue(ProducerDouble.INVALID_DPOP_CHALLENGE.contains("algs=\"ES256\""));
    }
}
