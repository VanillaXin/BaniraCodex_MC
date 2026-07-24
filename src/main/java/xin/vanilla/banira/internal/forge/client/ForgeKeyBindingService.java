package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.platform.BaniraInputService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Forge 按键注册服务：静态初始化阶段先入队，client setup 时统一提交。
 */
public final class ForgeKeyBindingService implements BaniraInputService {
    public static final ForgeKeyBindingService INSTANCE = new ForgeKeyBindingService();

    private final List<ForgeBaniraKeyHandle> pending = new ArrayList<>();
    private boolean flushCompleted;

    private ForgeKeyBindingService() {
    }

    @Nonnull
    @Override
    public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
        String category = spec.category() != null ? spec.category() : BaniraInput.defaultCategory(spec.modId());
        KeyMapping binding = new KeyMapping(BaniraInput.descriptionId(spec.modId(), spec.suffix()), spec.defaultKey(), category);
        ForgeBaniraKeyHandle handle = new ForgeBaniraKeyHandle(binding, category, spec.defaultKey());
        enqueueOrRegister(handle);
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
        for (ForgeBaniraKeyHandle handle : pending) {
            ClientRegistry.registerKeyBinding(handle.binding());
        }
        pending.clear();
        flushCompleted = true;
    }

    private void enqueueOrRegister(@Nonnull ForgeBaniraKeyHandle handle) {
        if (flushCompleted) {
            ClientRegistry.registerKeyBinding(handle.binding());
        } else {
            pending.add(handle);
        }
    }
}
