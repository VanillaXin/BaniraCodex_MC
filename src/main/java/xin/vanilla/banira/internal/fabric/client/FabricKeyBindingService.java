package xin.vanilla.banira.internal.fabric.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.platform.BaniraInputService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Fabric 按键注册服务，隐藏 KeyMapping 实现细节。
 */
public enum FabricKeyBindingService implements BaniraInputService {
    INSTANCE;

    private final List<FabricKeyHandle> pending = new ArrayList<>();
    private boolean flushed;

    @Nonnull
    @Override
    public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
        String category = spec.category() != null ? spec.category() : BaniraInput.defaultCategory(spec.modId());
        FabricKeyHandle handle = new FabricKeyHandle(new KeyMapping(
                BaniraInput.descriptionId(spec.modId(), spec.suffix()),
                spec.defaultKey(),
                category
        ), spec.defaultKey());
        if (flushed) {
            KeyBindingHelper.registerKeyBinding(handle.mapping);
        } else {
            pending.add(handle);
        }
        return handle;
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return InputStateManager.isKeyPressing(keyCode);
    }

    @Override
    public boolean isMouseDown(int button) {
        return InputStateManager.isMousePressing(button);
    }

    @Override
    public void flushPendingRegistrations() {
        for (FabricKeyHandle handle : pending) {
            KeyBindingHelper.registerKeyBinding(handle.mapping);
        }
        pending.clear();
        flushed = true;
    }

    private static final class FabricKeyHandle implements BaniraKeyHandle {
        private final KeyMapping mapping;
        private final int defaultKey;

        private FabricKeyHandle(KeyMapping mapping, int defaultKey) {
            this.mapping = mapping;
            this.defaultKey = defaultKey;
        }

        @Nonnull
        @Override
        public String descriptionId() {
            return mapping.getName();
        }

        @Nonnull
        @Override
        public String category() {
            return mapping.getCategory();
        }

        @Override
        public int defaultKey() {
            return defaultKey;
        }

        @Override
        public boolean isDown() {
            return mapping.isDown();
        }

        @Override
        public boolean consumeClick() {
            return mapping.consumeClick();
        }
    }
}
