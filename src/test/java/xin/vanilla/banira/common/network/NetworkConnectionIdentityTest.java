package xin.vanilla.banira.common.network;

import org.junit.Test;
import static org.junit.Assert.*;

public class NetworkConnectionIdentityTest {
    @Test public void oldContextsDeclareConnectionIdentityUnavailable() {
        BaniraNetworkContext context = new BaniraNetworkContext() {
            public void enqueueWork(Runnable work) { work.run(); }
            public void markHandled() { }
            public boolean isClientSide() { return false; }
            public boolean isServerSide() { return true; }
            public Object sender() { return null; }
        };
        assertNull(context.connectionKey());
    }
}
