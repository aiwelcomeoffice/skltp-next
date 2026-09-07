package se.skltpnext.experiment001.dependency;

/** Local control-plane configuration, never selected by a request's scenario headers. */
public enum DoubleFault {
    NONE, SLOW, UNAVAILABLE, INVALID_RESPONSE, UNDOCUMENTED_ERROR, INTERNAL_DETAIL;

    public void delayResponse() throws java.io.IOException {
        if (this != SLOW) return;
        try { Thread.sleep(600); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new java.io.IOException("Fault delay interrupted", e);
        }
    }
}
