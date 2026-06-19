package xin.vanilla.banira.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 维护本端声明的 modid 与服务端同步回调，避免网络包类保存全局状态。
 */
public final class ModLoadedPresenceRegistry {

    private final Object lock = new Object();
    private final Map<String, Consumer<ServerPlayerEntity>> syncByModId = new LinkedHashMap<>();

    @Nonnull
    public ModLoadedRegistration register(@Nonnull String modid, @Nonnull Consumer<ServerPlayerEntity> onServerSync) {
        Objects.requireNonNull(onServerSync, "onServerSync");
        String key = normalizeModId(modid);
        if (key.isEmpty()) {
            return ModLoadedRegistration.NOOP;
        }
        synchronized (lock) {
            syncByModId.put(key, onServerSync);
        }
        return () -> unregister(key, onServerSync);
    }

    public boolean unregister(@Nonnull String modid) {
        String key = normalizeModId(modid);
        if (key.isEmpty()) {
            return false;
        }
        synchronized (lock) {
            return syncByModId.remove(key) != null;
        }
    }

    public boolean unregister(@Nonnull String modid, @Nonnull Consumer<ServerPlayerEntity> onServerSync) {
        Objects.requireNonNull(onServerSync, "onServerSync");
        String key = normalizeModId(modid);
        if (key.isEmpty()) {
            return false;
        }
        synchronized (lock) {
            if (syncByModId.get(key) == onServerSync) {
                syncByModId.remove(key);
                return true;
            }
            return false;
        }
    }

    @Nonnull
    public List<String> announcedModIds() {
        synchronized (lock) {
            return Collections.unmodifiableList(new ArrayList<>(syncByModId.keySet()));
        }
    }

    public boolean hasRegistration(@Nonnull String modid) {
        String key = normalizeModId(modid);
        if (key.isEmpty()) {
            return false;
        }
        synchronized (lock) {
            return syncByModId.containsKey(key);
        }
    }

    public boolean dispatchServerSync(@Nullable ServerPlayerEntity player, @Nonnull String modid) {
        if (player == null) {
            return false;
        }
        Consumer<ServerPlayerEntity> sync;
        synchronized (lock) {
            sync = syncByModId.get(normalizeModId(modid));
        }
        if (sync == null) {
            return false;
        }
        sync.accept(player);
        return true;
    }

    public void clear() {
        synchronized (lock) {
            syncByModId.clear();
        }
    }

    private static String normalizeModId(String modid) {
        return StringUtils.isNullOrEmptyEx(modid) ? "" : modid.trim();
    }
}
