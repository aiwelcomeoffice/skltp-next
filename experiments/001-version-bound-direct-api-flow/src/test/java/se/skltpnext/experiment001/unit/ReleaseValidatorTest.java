package se.skltpnext.experiment001.unit;

import org.junit.jupiter.api.Test;
import se.skltpnext.experiment001.release.ReleaseValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseValidatorTest {
    @Test
    void selectsExactlyOneDigestBoundReleaseWithoutDynamicValues() {
        var selection = new ReleaseValidator().validate();
        assertEquals("E001-RELEASE", selection.releaseId());
        assertEquals("1.0.0", selection.releaseVersion());
        assertEquals(13, selection.referenceCount());
    }
}
