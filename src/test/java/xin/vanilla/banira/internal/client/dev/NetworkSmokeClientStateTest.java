package xin.vanilla.banira.internal.client.dev;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkSmokeClientStateTest {

    @Test
    public void reportsTheFirstRemoteLoginAfterConnectionWaitTicks() {
        NetworkSmokeClientState state = new NetworkSmokeClientState();

        assertTrue(state.markRemoteLogin());
        assertFalse(state.markRemoteLogin());
    }
}
