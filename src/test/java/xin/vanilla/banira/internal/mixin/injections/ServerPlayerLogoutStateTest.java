package xin.vanilla.banira.internal.mixin.injections;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServerPlayerLogoutStateTest {
    private boolean begin(ServerPlayerMixin player) throws Exception {
        Method method = ServerPlayerMixin.class.getDeclaredMethod("banira$beginLogout");
        return (Boolean) method.invoke(player);
    }

    @Test
    public void nativeRemoveAfterRemoveAllDoesNotDispatchAgain() throws Exception {
        ServerPlayerMixin player = new ServerPlayerMixin() {
        };
        assertTrue(begin(player));
        assertFalse(begin(player));
        assertFalse(begin(player));
    }

    @Test
    public void ANewPlayerInstanceCanStartItsOwnLogout() throws Exception {
        ServerPlayerMixin first = new ServerPlayerMixin() {
        };
        ServerPlayerMixin rejoined = new ServerPlayerMixin() {
        };
        assertTrue(begin(first));
        assertFalse(begin(first));
        assertTrue(begin(rejoined));
    }
}
