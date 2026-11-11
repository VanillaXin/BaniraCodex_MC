package xin.vanilla.banira.internal.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.platform.BaniraInputService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Fabric 按键注册服务，隐藏 KeyMapping 实现细节。
 */
public enum FabricKeyBindingService implements BaniraInputService {
    INSTANCE;

    private final List<FabricKeyHandle> pending = new ArrayList<>();
    private final Map<String, KeyMapping.Category> categories = new HashMap<>();
    private boolean flushed;

    @Nullable
    public static Component categoryLabel(Identifier id) {
        for (Map.Entry<String, KeyMapping.Category> entry : INSTANCE.categories.entrySet()) {
            if (entry.getValue().id().equals(id)) return Component.translatable(entry.getKey());
        }
        return null;
    }

    @Nonnull
    @Override
    public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
        String category = spec.category() != null ? spec.category() : BaniraInput.defaultCategory(spec.modId());
        KeyMapping.Category nativeCategory = categories.computeIfAbsent(category, value -> KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("banira_codex", "key_category/" + HexFormat.of().formatHex(
                        value.getBytes(StandardCharsets.UTF_8)))));
        FabricKeyHandle handle = new FabricKeyHandle(new KeyMapping(
                BaniraInput.descriptionId(spec.modId(), spec.suffix()),
                spec.defaultKey(),
                nativeCategory
        ), spec.defaultKey(), category);
        if (flushed) {
            KeyMappingHelper.registerKeyMapping(handle.mapping);
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
            KeyMappingHelper.registerKeyMapping(handle.mapping);
        }
        pending.clear();
        flushed = true;
    }

    private static final class FabricKeyHandle implements BaniraKeyHandle {
        private final KeyMapping mapping;
        private final int defaultKey;
        private final String category;

        private FabricKeyHandle(KeyMapping mapping, int defaultKey, String category) {
            this.mapping = mapping;
            this.defaultKey = defaultKey;
            this.category = category;
        }

        @Nonnull
        @Override
        public String descriptionId() {
            return mapping.getName();
        }

        @Nonnull
        @Override
        public String category() {
            return category;
        }

        @Override
        public int defaultKey() {
            return defaultKey;
        }

        @Override
        public int currentKey() {
            return InputConstants.getKey(mapping.saveString()).getValue();
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
