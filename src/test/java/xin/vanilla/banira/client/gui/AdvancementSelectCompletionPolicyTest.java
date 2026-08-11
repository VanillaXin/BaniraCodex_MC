package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 锁定进度选择完成后的默认导航与多步骤流程接管行为。
 */
public class AdvancementSelectCompletionPolicyTest {
    @Test
    public void closesAfterSubmitByDefault() {
        AtomicInteger closeCalls = new AtomicInteger();

        AdvancementSelectScreen.closeAfterSubmit(
                new AdvancementSelectScreen.Args(),
                closeCalls::incrementAndGet
        );

        assertEquals(1, closeCalls.get());
    }

    @Test
    public void allowsCallbackToOwnNavigation() {
        AtomicInteger closeCalls = new AtomicInteger();
        AdvancementSelectScreen.Args args = new AdvancementSelectScreen.Args().closeAfterSubmit(false);

        AdvancementSelectScreen.closeAfterSubmit(args, closeCalls::incrementAndGet);

        assertEquals(0, closeCalls.get());
    }
}
