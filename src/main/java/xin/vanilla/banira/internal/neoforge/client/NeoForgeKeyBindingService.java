package xin.vanilla.banira.internal.neoforge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.apache.commons.lang3.ArrayUtils;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.platform.BaniraInputService;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * NeoForge 按键注册服务：静态初始化阶段先入队，注册事件到来时统一提交。
 */
public final class NeoForgeKeyBindingService implements BaniraInputService {
    public static final NeoForgeKeyBindingService INSTANCE = new NeoForgeKeyBindingService();

    private final List<NeoForgeBaniraKeyHandle> pending = new ArrayList<>();
    private final Map<String, KeyMapping.Category> categories = new HashMap<>();
    private boolean flushCompleted;

    private NeoForgeKeyBindingService() {
    }

    public static Component categoryLabel(Identifier id) {
        for (var entry : INSTANCE.categories.entrySet()) {
            if (entry.getValue().id().equals(id)) return Component.translatable(entry.getKey());
        }
        return null;
    }

    @Nonnull
    @Override
    public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
        String category = spec.category() != null ? spec.category() : BaniraInput.defaultCategory(spec.modId());
        KeyMapping.Category nativeCategory = categories.computeIfAbsent(category, value ->
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("banira_codex",
                        "key_category/" + HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8)))));
        KeyMapping binding = new KeyMapping(BaniraInput.descriptionId(spec.modId(), spec.suffix()), spec.defaultKey(), nativeCategory);
        NeoForgeBaniraKeyHandle handle = new NeoForgeBaniraKeyHandle(binding, category, spec.defaultKey());
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
        // NeoForge 要求在 RegisterKeyMappingsEvent 中注册；事件到来前只保持队列。
    }

    public void flushPendingRegistrations(@Nonnull RegisterKeyMappingsEvent event) {
        for (NeoForgeBaniraKeyHandle handle : pending) {
            event.register(handle.binding());
        }
        pending.clear();
        flushCompleted = true;
    }

    private void enqueueOrRegister(@Nonnull NeoForgeBaniraKeyHandle handle) {
        if (flushCompleted) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.options != null) {
                Options options = minecraft.options;
                options.keyMappings = ArrayUtils.add(options.keyMappings, handle.binding());
            }
        } else {
            pending.add(handle);
        }
    }
}
