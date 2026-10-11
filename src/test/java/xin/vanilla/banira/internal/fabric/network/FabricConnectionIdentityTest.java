package xin.vanilla.banira.internal.fabric.network;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class FabricConnectionIdentityTest {
    @Test public void callbacksCaptureTheConnectionBeforeEnqueueing() throws Exception {
        Object first = new Object(), second = new Object();
        Method create = FabricNetworkContext.class.getMethod("client", Object.class);
        Object old = create.invoke(null, first);
        Object current = create.invoke(null, second);
        Method key = FabricNetworkContext.class.getMethod("connectionKey");
        assertSame(first, key.invoke(old));
        assertSame(second, key.invoke(current));
        assertSame(first, key.invoke(old));
    }
}
