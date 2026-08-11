package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * 锁定效果选择完成后的默认导航与多步骤流程接管行为。
 */
public class EffectSelectCompletionPolicyTest {
    @Test
    public void closesAfterSubmitByDefault() {
        AtomicInteger closeCalls = new AtomicInteger();

        EffectSelectScreen.closeAfterSubmit(true, closeCalls::incrementAndGet);

        assertEquals(1, closeCalls.get());
    }

    @Test
    public void allowsCallbackToOwnNavigation() {
        AtomicInteger closeCalls = new AtomicInteger();
        EffectSelectScreen.closeAfterSubmit(false, closeCalls::incrementAndGet);

        assertEquals(0, closeCalls.get());
    }
}
