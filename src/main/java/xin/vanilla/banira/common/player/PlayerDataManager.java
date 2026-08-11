package xin.vanilla.banira.common.player;

import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.NBTUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 玩家数据管理
 */
public final class PlayerDataManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // suffix -> instance
    private static final Map<String, PlayerDataManager> INSTANCES = new ConcurrentHashMap<>();

    private final Supplier<Path> playerDataDirSupplier;
    private final @Nullable Supplier<Path> legacyPlayerDataBaseSupplier;
    private final String modId;
    /**
     * 新区 NBT 相对 {@code playerDataDirSupplier} 的子目录，空串表示直接存于该根目录下
     */
    private final String suffix;
    /**
     * 旧版迁移时相对 {@code legacyPlayerDataBaseSupplier} 的子目录；为 null 时与 {@link #suffix} 相同
     */
    private final @Nullable String legacyMigrationSubdir;

    // 实例的缓存与锁
    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<Path, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private PlayerDataManager(
            Supplier<Path> playerDataDirSupplier,
            @Nullable Supplier<Path> legacyPlayerDataBaseSupplier,
            String modId,
            String suffix,
            @Nullable String legacyMigrationSubdir
    ) {
        this.playerDataDirSupplier = playerDataDirSupplier;
        this.legacyPlayerDataBaseSupplier = legacyPlayerDataBaseSupplier;
        this.modId = modId;
        this.suffix = sanitizeSuffix(suffix);
        this.legacyMigrationSubdir = legacyMigrationSubdir == null ? null : sanitizeSuffix(legacyMigrationSubdir);
    }


    /**
     * 获取已存在实例
     */
    public static @Nullable PlayerDataManager getInstance(String suffix) {
        if (suffix == null) suffix = "";
        return INSTANCES.get(sanitizeSuffixStatic(suffix));
    }

    /**
     * 创建或获取实例
     *
     * @param playerDataDirSupplier 延迟提供玩家 NBT 根目录（通常为 {@code world/VanillaXin/playerdata}）
     * @param suffix                实例标识符
     */
    public static PlayerDataManager getOrCreateInstance(Supplier<Path> playerDataDirSupplier, String modId, String suffix) {
        return getOrCreateInstance(playerDataDirSupplier, null, modId, suffix);
    }

    /**
     * 创建或获取实例；若提供 {@code legacyPlayerDataBaseSupplier}，则在新区无文件且旧版路径存在文件时自动迁移。
     *
     * @param playerDataDirSupplier        新区玩家 NBT 根目录
     * @param legacyPlayerDataBaseSupplier 旧版 {@code world/playerdata} 目录，可为 null
     * @param suffix                       新区实例子目录名，空串表示 NBT 直接位于 {@code playerDataDirSupplier} 根下
     */
    public static PlayerDataManager getOrCreateInstance(
            Supplier<Path> playerDataDirSupplier,
            @Nullable Supplier<Path> legacyPlayerDataBaseSupplier,
            String modId,
            String suffix
    ) {
        return getOrCreateInstance(playerDataDirSupplier, legacyPlayerDataBaseSupplier, modId, suffix, null);
    }

    /**
     * @param legacyMigrationSubdir 旧版数据所在子目录（相对 {@code world/playerdata}）；为 null 时与 {@code suffix} 相同
     */
    public static PlayerDataManager getOrCreateInstance(
            Supplier<Path> playerDataDirSupplier,
            @Nullable Supplier<Path> legacyPlayerDataBaseSupplier,
            String modId,
            String suffix,
            @Nullable String legacyMigrationSubdir
    ) {
        String key = sanitizeSuffixStatic(suffix);
        return INSTANCES.computeIfAbsent(key, k -> {
            LOGGER.info("Creating PlayerDataManager instance for suffix '{}'", k.isEmpty() ? "(root)" : k);
            return new PlayerDataManager(playerDataDirSupplier, legacyPlayerDataBaseSupplier, modId, k, legacyMigrationSubdir);
        });
    }

    /**
     * 移除并关闭实例
     */
    public static void removeInstance(String suffix) {
        String key = sanitizeSuffixStatic(suffix);
        INSTANCES.remove(key);
        LOGGER.info("Removed PlayerDataManager instance for suffix '{}'", key);
    }


    private static final class CachedPlayerData {
        volatile CompoundTag root;
        @Nullable
        CompoundTag persistedRoot;
        volatile boolean dirty = false;

        CachedPlayerData(CompoundTag root, boolean migratedFromLegacy) {
            this.root = root;
            this.persistedRoot = migratedFromLegacy ? null : root.copy();
            this.dirty = migratedFromLegacy;
        }
    }

    /**
     * 清空此实例的内存缓存
     */
    public void clearCache() {
        playerCache.clear();
        fileLocks.clear();
        LOGGER.info("PlayerDataManager[{}] cache cleared.", suffix);
    }

    /**
     * 获取或创建当前 mod 的节点
     *
     * @return 缓存中节点的引用
     */
    public CompoundTag getOrCreate(UUID playerUuid) {
        return getOrCreate(playerUuid, this.modId);
    }

    /**
     * 获取或创建某个 mod 的节点
     *
     * @return 缓存中节点的引用
     */
    public CompoundTag getOrCreate(UUID playerUuid, String modId) {
        CachedPlayerData cached = loadRootIfAbsent(playerUuid);
        synchronized (cached) {
            CompoundTag node = cached.root.contains(modId, 10) ? cached.root.getCompound(modId) : new CompoundTag();
            cached.root.put(modId, node);
            cached.dirty = true;
            return node;
        }
    }

    /**
     * 覆盖某个 mod 的节点
     * tag 为 null 时会移除该节点，但不从 cache 中移除整玩家数据
     */
    public void put(UUID playerUuid, CompoundTag tag) {
        put(playerUuid, this.modId, tag);
    }

    /**
     * 覆盖某个 mod 的节点
     * tag 为 null 时会移除该节点，但不从 cache 中移除整玩家数据
     */
    public void put(UUID playerUuid, String modId, CompoundTag tag) {
        CachedPlayerData cached = loadRootIfAbsent(playerUuid);
        synchronized (cached) {
            if (tag == null) {
                if (cached.root.contains(modId, 10)) {
                    cached.root.remove(modId);
                    cached.dirty = true;
                }
            } else {
                cached.root.put(modId, tag);
                cached.dirty = true;
            }
        }
    }

    /**
     * 从磁盘读取某玩家的 root 并返回该 mod 的节点
     */
    public CompoundTag loadFromDisk(UUID playerUuid) {
        return loadFromDisk(playerUuid, this.modId);
    }

    /**
     * 从磁盘读取某玩家的 root 并返回该 mod 的节点
     */
    public CompoundTag loadFromDisk(UUID playerUuid, String modId) {
        CachedPlayerData cached = loadRootFromDisk(playerUuid);
        synchronized (cached) {
            if (cached.root.contains(modId, 10)) {
                return cached.root.getCompound(modId);
            } else {
                CompoundTag node = new CompoundTag();
                cached.root.put(modId, node);
                cached.dirty = true;
                return node;
            }
        }
    }

    /**
     * 从 cache 中删除某个 mod 的节点
     */
    public void remove(UUID playerUuid) {
        remove(playerUuid, this.modId);
    }

    /**
     * 从 cache 中删除某个 mod 的节点
     */
    public void remove(UUID playerUuid, String modId) {
        CachedPlayerData cached = playerCache.get(playerUuid);
        if (cached == null) return;
        synchronized (cached) {
            if (cached.root.contains(modId, 10)) {
                cached.root.remove(modId);
                cached.dirty = true;
            }
        }
    }

    /**
     * 尝试保指定玩家的 dirty 缓存项
     */
    public void saveToDisk(UUID playerUuid) {
        CachedPlayerData cached = playerCache.get(playerUuid);
        if (cached == null) {
            return;
        }
        File file = getPlayerDataFile(playerUuid);
        Path filePath = file.toPath();
        ReentrantLock lock = fileLocks.computeIfAbsent(filePath, p -> new ReentrantLock());
        lock.lock();
        try {
            CompoundTag snapshot;
            synchronized (cached) {
                if (!cached.dirty) return;
                if (cached.persistedRoot != null && cached.root.equals(cached.persistedRoot)) {
                    cached.dirty = false;
                    return;
                }
                // 只在短锁内复制；压缩和文件替换不阻塞内存数据读写。
                snapshot = cached.root.copy();
            }
            atomicWrite(snapshot, file);
            synchronized (cached) {
                cached.persistedRoot = snapshot;
                cached.dirty = !cached.root.equals(snapshot);
            }
        } catch (IOException e) {
            LOGGER.error("PlayerDataManager[{}] failed to write {} : {}", suffix, file.getAbsolutePath(), e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试保存所有 dirty 缓存项
     */
    public void saveAll() {
        for (UUID uuid : playerCache.keySet()) {
            saveToDisk(uuid);
        }
    }

    private CachedPlayerData loadRootIfAbsent(UUID playerUuid) {
        CachedPlayerData existing = playerCache.get(playerUuid);
        if (existing != null) return existing;

        File file = getPlayerDataFile(playerUuid);
        Path filePath = file.toPath();
        ReentrantLock lock = fileLocks.computeIfAbsent(filePath, p -> new ReentrantLock());
        lock.lock();
        try {
            // double-check
            existing = playerCache.get(playerUuid);
            if (existing != null) return existing;

            CompoundTag root;
            boolean migratedFromLegacy = false;
            if (file.exists()) {
                try {
                    root = NBTUtils.readCompressed(file);
                } catch (Exception e) {
                    LOGGER.warn("PlayerDataManager[{}] failed to read {}, using empty root. Error: {}",
                            suffix, file.getAbsolutePath(), e.getMessage());
                    root = new CompoundTag();
                }
            } else {
                RootLoadFromLegacy legacy = tryLoadRootFromLegacy(playerUuid);
                root = legacy.root;
                migratedFromLegacy = legacy.fromLegacy;
            }
            CachedPlayerData cached = new CachedPlayerData(root, migratedFromLegacy);
            playerCache.put(playerUuid, cached);
            return cached;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 强制从磁盘读取玩家数据
     */
    private CachedPlayerData loadRootFromDisk(UUID playerUuid) {
        File file = getPlayerDataFile(playerUuid);
        Path filePath = file.toPath();
        ReentrantLock lock = fileLocks.computeIfAbsent(filePath, p -> new ReentrantLock());
        lock.lock();
        try {
            CompoundTag root;
            boolean migratedFromLegacy = false;
            if (file.exists()) {
                try {
                    root = NBTUtils.readCompressed(file);
                } catch (Exception e) {
                    LOGGER.warn("PlayerDataManager[{}] failed to read {}, using empty root. Error: {}",
                            suffix, file.getAbsolutePath(), e.getMessage());
                    root = new CompoundTag();
                }
            } else {
                RootLoadFromLegacy legacy = tryLoadRootFromLegacy(playerUuid);
                root = legacy.root;
                migratedFromLegacy = legacy.fromLegacy;
            }
            CachedPlayerData cached = new CachedPlayerData(root, migratedFromLegacy);
            playerCache.put(playerUuid, cached);
            return cached;
        } finally {
            lock.unlock();
        }
    }

    private File getPlayerDataFile(UUID uuid) {
        Path base = playerDataDirSupplier.get();
        Path dirPath = suffix.isEmpty() ? base : base.resolve(suffix);
        File dir = dirPath.toFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                LOGGER.debug("PlayerDataManager[{}] could not create dir: {}", suffix.isEmpty() ? "(root)" : suffix, dir.getAbsolutePath());
            }
        }
        return new File(dir, uuid + ".nbt");
    }

    private String effectiveLegacySubdir() {
        return legacyMigrationSubdir != null ? legacyMigrationSubdir : suffix;
    }

    private static final class RootLoadFromLegacy {
        final CompoundTag root;
        final boolean fromLegacy;

        RootLoadFromLegacy(CompoundTag root, boolean fromLegacy) {
            this.root = root;
            this.fromLegacy = fromLegacy;
        }
    }

    private RootLoadFromLegacy tryLoadRootFromLegacy(UUID uuid) {
        File legacyFile = getLegacyPlayerDataFile(uuid);
        if (legacyFile != null && legacyFile.exists()) {
            try {
                CompoundTag root = NBTUtils.readCompressed(legacyFile);
                LOGGER.info("PlayerDataManager[{}] loaded player {} from legacy path (will save to new location on next save)",
                        suffix.isEmpty() ? "(root)" : suffix, uuid);
                return new RootLoadFromLegacy(root, true);
            } catch (Exception e) {
                LOGGER.warn("PlayerDataManager[{}] failed to read legacy {}: {}",
                        suffix.isEmpty() ? "(root)" : suffix, legacyFile.getAbsolutePath(), e.getMessage());
            }
        }
        return new RootLoadFromLegacy(new CompoundTag(), false);
    }

    private @Nullable File getLegacyPlayerDataFile(UUID uuid) {
        if (legacyPlayerDataBaseSupplier == null) {
            return null;
        }
        Path base = legacyPlayerDataBaseSupplier.get();
        if (base == null) {
            return null;
        }
        String leg = effectiveLegacySubdir();
        Path legacyDir = leg.isEmpty() ? base : base.resolve(leg);
        return legacyDir.resolve(uuid + ".nbt").toFile();
    }

    private void atomicWrite(CompoundTag root, File target) throws IOException {
        File dir = target.getParentFile();
        File tmpFile = new File(dir, target.getName() + ".tmp");
        File bakFile = new File(dir, target.getName() + ".bak");

        boolean written = NBTUtils.writeCompressed(root, tmpFile);
        if (!written) throw new IOException("Failed to write temp file: " + tmpFile.getAbsolutePath());

        try {
            if (target.exists()) {
                try {
                    Files.move(target.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ex) {
                    Files.move(target.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("PlayerDataManager[{}] failed to move original to bak: {} -> {}. Error: {}",
                    suffix, target.getAbsolutePath(), bakFile.getAbsolutePath(), e.getMessage());
        }

        try {
            try {
                Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("PlayerDataManager[{}] failed to move tmp to target: {} -> {}. Error: {}",
                    suffix, tmpFile.getAbsolutePath(), target.getAbsolutePath(), e.getMessage());
            if (bakFile.exists()) {
                try {
                    Files.move(bakFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.warn("PlayerDataManager[{}] rollback: restored bak to target {}", suffix, target.getAbsolutePath());
                } catch (Exception rex) {
                    LOGGER.error("PlayerDataManager[{}] rollback failed for target: {} (bak: {})", suffix, target.getAbsolutePath(), bakFile.getAbsolutePath());
                }
            }
            if (tmpFile.exists()) tmpFile.delete();
            throw e;
        }
    }

    private static String sanitizeSuffixStatic(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("[/\\\\]+", "_");
    }

    private String sanitizeSuffix(String s) {
        return sanitizeSuffixStatic(s);
    }


    /**
     * 返回当前 registry 的快照（仅用于调试）
     */
    public static Map<String, PlayerDataManager> listInstances() {
        return new ConcurrentHashMap<>(INSTANCES);
    }

}
