package xin.vanilla.banira.common.player;

import net.minecraft.nbt.CompoundNBT;
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
    private final String modId;
    private final String suffix;

    // 实例的缓存与锁
    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<Path, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private PlayerDataManager(Supplier<Path> playerDataDirSupplier, String modId, String suffix) {
        this.playerDataDirSupplier = playerDataDirSupplier;
        this.modId = modId;
        this.suffix = sanitizeSuffix(suffix);
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
     * @param playerDataDirSupplier 延迟提供 world/playerdata 目录
     * @param suffix                实例标识符
     */
    public static PlayerDataManager getOrCreateInstance(Supplier<Path> playerDataDirSupplier, String modId, String suffix) {
        String key = sanitizeSuffixStatic(suffix);
        return INSTANCES.computeIfAbsent(key, k -> {
            LOGGER.info("Creating PlayerDataManager instance for suffix '{}'", k);
            return new PlayerDataManager(playerDataDirSupplier, modId, k);
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
        volatile CompoundNBT root;
        volatile boolean dirty = false;

        CachedPlayerData(CompoundNBT root) {
            this.root = root;
        }
    }

    /**
     * 清空此实例的内存缓存
     */
    public void clearCache() {
        playerCache.clear();
        LOGGER.info("PlayerDataManager[{}] cache cleared.", suffix);
    }

    /**
     * 获取或创建当前 mod 的节点
     *
     * @return 缓存中节点的引用
     */
    public CompoundNBT getOrCreate(UUID playerUuid) {
        return getOrCreate(playerUuid, this.modId);
    }

    /**
     * 获取或创建某个 mod 的节点
     *
     * @return 缓存中节点的引用
     */
    public CompoundNBT getOrCreate(UUID playerUuid, String modId) {
        CachedPlayerData cached = loadRootIfAbsent(playerUuid);
        synchronized (cached) {
            CompoundNBT node = cached.root.contains(modId, 10) ? cached.root.getCompound(modId) : new CompoundNBT();
            cached.root.put(modId, node);
            cached.dirty = true;
            return node;
        }
    }

    /**
     * 覆盖某个 mod 的节点
     * tag 为 null 时会移除该节点，但不从 cache 中移除整玩家数据
     */
    public void put(UUID playerUuid, CompoundNBT tag) {
        put(playerUuid, this.modId, tag);
    }

    /**
     * 覆盖某个 mod 的节点
     * tag 为 null 时会移除该节点，但不从 cache 中移除整玩家数据
     */
    public void put(UUID playerUuid, String modId, CompoundNBT tag) {
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
    public CompoundNBT loadFromDisk(UUID playerUuid) {
        return loadFromDisk(playerUuid, this.modId);
    }

    /**
     * 从磁盘读取某玩家的 root 并返回该 mod 的节点
     */
    public CompoundNBT loadFromDisk(UUID playerUuid, String modId) {
        CachedPlayerData cached = loadRootFromDisk(playerUuid);
        synchronized (cached) {
            if (cached.root.contains(modId, 10)) {
                return cached.root.getCompound(modId);
            } else {
                CompoundNBT node = new CompoundNBT();
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
        synchronized (cached) {
            if (!cached.dirty) return;
            File file = getPlayerDataFile(playerUuid);
            Path filePath = file.toPath();
            ReentrantLock lock = fileLocks.computeIfAbsent(filePath, p -> new ReentrantLock());
            lock.lock();
            try {
                atomicWrite(cached.root, file);
                cached.dirty = false;
            } catch (IOException e) {
                LOGGER.error("PlayerDataManager[{}] failed to write {} : {}", suffix, file.getAbsolutePath(), e.getMessage());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试保存所有 dirty 缓存项
     */
    public void saveAll() {
        for (Map.Entry<UUID, CachedPlayerData> e : playerCache.entrySet()) {
            UUID uuid = e.getKey();
            CachedPlayerData cached = e.getValue();
            synchronized (cached) {
                if (!cached.dirty) continue;
                File file = getPlayerDataFile(uuid);
                Path filePath = file.toPath();
                ReentrantLock lock = fileLocks.computeIfAbsent(filePath, p -> new ReentrantLock());
                lock.lock();
                try {
                    atomicWrite(cached.root, file);
                    cached.dirty = false;
                } catch (IOException ex) {
                    LOGGER.error("PlayerDataManager[{}] saveAll failed for {}: {}", suffix, uuid, ex.getMessage());
                } finally {
                    lock.unlock();
                }
            }
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

            CompoundNBT root;
            if (file.exists()) {
                try {
                    root = NBTUtils.readCompressed(file);
                } catch (Exception e) {
                    LOGGER.warn("PlayerDataManager[{}] failed to read {}, using empty root. Error: {}",
                            suffix, file.getAbsolutePath(), e.getMessage());
                    root = new CompoundNBT();
                }
            } else {
                root = new CompoundNBT();
            }
            CachedPlayerData cached = new CachedPlayerData(root);
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
            CompoundNBT root;
            if (file.exists()) {
                try {
                    root = NBTUtils.readCompressed(file);
                } catch (Exception e) {
                    LOGGER.warn("PlayerDataManager[{}] failed to read {}, using empty root. Error: {}",
                            suffix, file.getAbsolutePath(), e.getMessage());
                    root = new CompoundNBT();
                }
            } else {
                root = new CompoundNBT();
            }
            CachedPlayerData cached = new CachedPlayerData(root);
            playerCache.put(playerUuid, cached);
            return cached;
        } finally {
            lock.unlock();
        }
    }

    private File getPlayerDataFile(UUID uuid) {
        Path base = playerDataDirSupplier.get();
        File dir = base.resolve(suffix).toFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                LOGGER.debug("PlayerDataManager[{}] could not create dir: {}", suffix, dir.getAbsolutePath());
            }
        }
        return new File(dir, uuid + ".nbt");
    }

    private void atomicWrite(CompoundNBT root, File target) throws IOException {
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
