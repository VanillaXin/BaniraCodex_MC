package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 物品选择器作为多步流程的一环时，不应强制覆盖回调打开的下一界面。
 */
public class ItemSelectCompletionPolicyTest {
    @Test
    public void closesAfterSubmitByDefault() {
        AtomicInteger closeCalls = new AtomicInteger();

        ItemSelectScreen.closeAfterSubmit(new ItemSelectScreen.Args(), closeCalls::incrementAndGet);

        assertEquals(1, closeCalls.get());
    }

    @Test
    public void callbackCanOwnNavigation() {
        AtomicInteger closeCalls = new AtomicInteger();
        ItemSelectScreen.Args args = new ItemSelectScreen.Args().closeAfterSubmit(false);

        ItemSelectScreen.closeAfterSubmit(args, closeCalls::incrementAndGet);

        assertEquals(0, closeCalls.get());
    }
}
