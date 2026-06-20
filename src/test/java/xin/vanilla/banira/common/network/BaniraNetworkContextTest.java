package xin.vanilla.banira.common.network;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraNetworkContextTest {

    @Test
    public void senderAsReturnsRequestedTypeOnlyWhenCompatible() {
        BaniraNetworkContext context = contextWithSender("player");

        assertEquals("player", context.senderAs(String.class));
        assertNull(context.senderAs(Integer.class));
    }

    @Test
    public void senderAsHandlesNullSender() {
        BaniraNetworkContext context = contextWithSender(null);

        assertNull(context.senderAs(String.class));
    }

    private static BaniraNetworkContext contextWithSender(Object sender) {
        return new BaniraNetworkContext() {
            @Override
            public void enqueueWork(Runnable work) {
                work.run();
            }

            @Override
            public void markHandled() {
            }

            @Override
            public boolean isClientSide() {
                return false;
            }

            @Override
            public boolean isServerSide() {
                return true;
            }

            @Override
            public Object sender() {
                return sender;
            }
        };
    }
}
