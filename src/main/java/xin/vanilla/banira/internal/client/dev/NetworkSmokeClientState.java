package xin.vanilla.banira.internal.client.dev;

/**
 * Tracks one-shot network-smoke client milestones independently from tick timing.
 */
final class NetworkSmokeClientState {
    private boolean remoteLoginReported;

    boolean markRemoteLogin() {
        if (remoteLoginReported) return false;
        remoteLoginReported = true;
        return true;
    }
}
