package xin.vanilla.banira.internal.neoforge.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class NeoForgeConnectionIdentityTest {
    @Test public void payloadContextPreservesNativeConnectionIdentity() throws Exception {
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        IPayloadContext payload = (IPayloadContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("connection")) return connection;
                    throw new AssertionError("Unexpected context access " + method.getName());
                });
        Object context = new NeoForgeNetworkContext(payload);
        assertSame(connection, context.getClass().getMethod("connectionKey").invoke(context));
    }
}
