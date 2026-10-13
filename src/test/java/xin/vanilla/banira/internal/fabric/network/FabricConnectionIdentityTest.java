package xin.vanilla.banira.internal.fabric.network;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class FabricConnectionIdentityTest {
    @Test public void payloadCallbacksGuardOriginalSenderBeforeNormalizingTransport() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkHandler.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("FabricNetworkContext.client(clientConnectionKey(context))"));
        assertTrue(source.contains("FabricNetworkContext.server(context.server(), context.player(), context.player().connection)"));
    }
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

    private Object matched(Object packetSender, Object activeSender, Object transport) throws Exception {
        Method method = FabricNetworkHandler.class.getDeclaredMethod("matchClientConnection",
                Object.class, Object.class, Object.class);
        method.setAccessible(true);
        return method.invoke(null, packetSender, activeSender, transport);
    }
    @Test public void currentResponseSenderMapsToNativeTransport() throws Exception {
        Object sender = new Object(), transport = new Object();
        assertSame(transport, matched(sender, sender, transport));
    }
    @Test public void oldResponseSenderCannotBorrowNewTransport() throws Exception {
        assertNull(matched(new Object(), new Object(), new Object()));
    }
    @Test public void missingResponseSenderOrTransportCannotCreateIdentity() throws Exception {
        Object sender = new Object();
        assertNull(matched(null, sender, new Object()));
        assertNull(matched(sender, sender, null));
    }
    @Test public void callbackUsesSenderGuardBeforePublishingNativeIdentity() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkHandler.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("FabricNetworkContext.client(clientConnectionKey(context))"));
        assertTrue(source.contains("matchClientConnection(context.responseSender(), ClientPlayNetworking.getSender(),"));
    }
}
