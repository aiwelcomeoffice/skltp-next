package se.skltpnext.experiment001.unit;

import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.release.ReleaseValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseValidatorTest {
    @Test
    void selectsExactlyOneDigestBoundReleaseWithoutDynamicValues() {
        var selection = new ReleaseValidator().validate();
        assertEquals("E001-RELEASE", selection.releaseId());
        assertEquals("1.0.0", selection.releaseVersion());
        assertEquals(13, selection.referenceCount());
    }

    @Test
    void missingRefFixtureIsDeniedForIncompleteReferenceSet() {
        var exception = assertThrows(ReleaseValidator.ReleaseValidationException.class,
                () -> new ReleaseValidator().validateFixture("missing-ref"));
        assertEquals(ReleaseValidator.FailureCategory.MISSING_REF, exception.category());
    }

    @Test
    void ambiguousRefFixtureIsDeniedForDuplicateReferenceType() {
        var exception = assertThrows(ReleaseValidator.ReleaseValidationException.class,
                () -> new ReleaseValidator().validateFixture("ambiguous-ref"));
        assertEquals(ReleaseValidator.FailureCategory.AMBIGUOUS_REF, exception.category());
    }

    @Test
    void digestMutationFixtureIsDeniedForByteMismatch() {
        var exception = assertThrows(ReleaseValidator.ReleaseValidationException.class,
                () -> new ReleaseValidator().validateFixture("digest-mutation"));
        assertEquals(ReleaseValidator.FailureCategory.DIGEST_MUTATION, exception.category());
    }
}
