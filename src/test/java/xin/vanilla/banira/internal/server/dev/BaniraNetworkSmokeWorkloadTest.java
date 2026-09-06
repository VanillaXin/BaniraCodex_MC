package xin.vanilla.banira.internal.server.dev;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaniraNetworkSmokeWorkloadTest {
    @Test
    public void staysActiveForTheConfiguredServerTickWindow() {
        BaniraNetworkSmokeWorkload workload = new BaniraNetworkSmokeWorkload(100);

        assertFalse(workload.completeAt(419));
        assertTrue(workload.completeAt(420));
    }
}
