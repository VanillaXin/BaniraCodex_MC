package xin.vanilla.banira.internal.mixin;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the loader-independent optional-integration guard.
 */
public final class OptionalCompatibilityMixinPluginTest {
    private final OptionalCompatibilityMixinPlugin plugin = new OptionalCompatibilityMixinPlugin();

    @Test
    public void keepsCoreMixinsWithoutOptionalTargets() {
        assertTrue(plugin.shouldApplyMixin("java.lang.String",
                "xin.vanilla.banira.internal.mixin.injections.ServerPlayerMixin"));
    }

    @Test
    public void skipsOptionalMixinWhenItsTargetIsMissing() {
        assertFalse(plugin.shouldApplyMixin("missing.optional.JeiButton",
                "xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonMixin"));
        assertFalse(plugin.shouldApplyMixin("missing.optional.IpnButton",
                "xin.vanilla.banira.internal.mixin.compat.ipn.IpnButtonMixin"));
    }
}
