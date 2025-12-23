package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.minecraft.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.ArraySet;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.internal.network.data.AdvancementData;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public class AdvancementUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 进度信息
     */
    private static final ArraySet<AdvancementData> advancementData = new ArraySet<>();

    /**
     * 是否已经向服务器请求过进度数据
     */
    private static boolean requestedAdvancementData = false;

    /**
     * 数据是否已加载完成
     */
    @Getter
    private static boolean dataLoaded = false;

    /**
     * 确保进度数据已在客户端缓存
     */
    public static void ensureAdvancementData() {
        if (FMLEnvironment.dist.isClient() && !requestedAdvancementData && advancementData.isEmpty()) {
            requestAdvancementDataFromServer();
        }
    }

    /**
     * 向服务器请求进度数据
     */
    public static void requestAdvancementDataFromServer() {
        if (FMLEnvironment.dist.isClient() && !requestedAdvancementData) {
            requestedAdvancementData = true;
            dataLoaded = false;
            PacketUtils.sendPacketToServer(NetworkInit.HANDLER.getChannel(),
                    new RequestToBoth(NetworkInit.REQUEST_ADVANCEMENT_DATA));
            LOGGER.debug("Request advancement data from server.");
        }
    }

    /**
     * 检查数据是否正在加载中
     */
    public static boolean isLoading() {
        return requestedAdvancementData && !dataLoaded;
    }

    /**
     * 检查数据是否可用
     */
    public static boolean isDataAvailable() {
        return dataLoaded && !advancementData.isEmpty();
    }

    public static void clearAdvancementData() {
        AdvancementUtils.advancementData.clear();
        dataLoaded = false;
        requestedAdvancementData = false;
    }

    public static void addAdvancementData(AdvancementData advancementData) {
        AdvancementUtils.advancementData.add(advancementData);
    }

    public static void addAdvancementData(AdvancementData... advancementData) {
        AdvancementUtils.advancementData.addAll(Arrays.asList(advancementData));
    }

    /**
     * 设置进度数据
     *
     * @param advancementData 进度数据集合
     */
    public static void advancementData(Collection<AdvancementData> advancementData) {
        if (CollectionUtils.isNotNullOrEmpty(advancementData)) {
            AdvancementUtils.advancementData.clear();
            AdvancementUtils.advancementData.addAll(advancementData);
            dataLoaded = true;
            LOGGER.debug("Advancement data loaded: {} items", advancementData.size());
        }
    }

    /**
     * 获取进度数据
     */
    public static ArraySet<AdvancementData> advancementData() {
        // 服务端
        if (advancementData.isEmpty() && BaniraCodex.serverInstance().val()) {
            advancementData(BaniraCodex.serverInstance().key().getAdvancements().getAllAdvancements().stream()
                    .map(AdvancementData::fromAdvancement).collect(Collectors.toList())
            );
        }
        // 客户端
        else if (FMLEnvironment.dist.isClient()) {
            ensureAdvancementData();
        }
        return advancementData;
    }

    // region 获取进度信息

    /**
     * 获取进度显示名称
     */
    public static @NonNull String getDisplayName(AdvancementData advancementData) {
        if (advancementData == null) {
            return "";
        }
        return advancementData.getDisplayInfo().getTitle().getString();
    }

    /**
     * 获取进度描述
     */
    public static @NonNull String getDescription(AdvancementData advancementData) {
        if (advancementData == null) {
            return "";
        }
        return advancementData.getDisplayInfo().getDescription().getString();
    }

    /**
     * 获取进度的注册ID字符串
     */
    public static String getAdvancementRegistryString(AdvancementData advancementData) {
        if (advancementData == null) {
            return "";
        }
        return advancementData.getId().toString();
    }

    // endregion

    // region 获取所有进度

    /**
     * 获取所有进度列表
     */
    public static List<AdvancementData> getAllAdvancements() {
        // 在客户端确保已请求数据
        if (FMLEnvironment.dist.isClient()) {
            ensureAdvancementData();
        }
        return new ArrayList<>(advancementData().asList());
    }

    /**
     * 获取所有可显示的进度列表（有图标的进度）
     */
    @OnlyIn(Dist.CLIENT)
    public static List<AdvancementData> getDisplayableAdvancements() {
        return getAllAdvancements().stream()
                .filter(o -> o.getDisplayInfo().getIcon().getItem() != Items.AIR)
                .collect(Collectors.toList());
    }

    /**
     * 筛选进度列表
     *
     * @param predicate 筛选条件
     * @return 筛选后的进度列表
     */
    public static List<AdvancementData> filterAdvancements(Predicate<AdvancementData> predicate) {
        if (predicate == null) return getAllAdvancements();
        return getAllAdvancements().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 检查进度是否匹配关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - $：搜索描述
     *
     * @param advancementData 进度数据
     * @param keyword         关键字
     * @return 是否匹配
     */
    private static boolean matchesKeyword(AdvancementData advancementData, String keyword) {
        if (advancementData == null || StringUtils.isNullOrEmpty(keyword)) {
            return StringUtils.isNullOrEmpty(keyword);
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        if (lowerKeyword.isEmpty()) {
            return true;
        }

        // 检查前缀
        if (lowerKeyword.startsWith("@")) {
            // @ 搜索注册ID
            String searchTerm = lowerKeyword.substring(1).trim();
            if (searchTerm.isEmpty()) return true;
            String registry = getAdvancementRegistryString(advancementData).toLowerCase();
            return registry.contains(searchTerm);
        } else if (lowerKeyword.startsWith("$")) {
            // $ 搜索描述
            String searchTerm = lowerKeyword.substring(1).trim();
            if (searchTerm.isEmpty()) return true;
            String description = getDescription(advancementData).toLowerCase();
            return StringUtils.isNotNullOrEmpty(description) && description.contains(searchTerm);
        } else {
            // 搜索所有字段
            String registry = getAdvancementRegistryString(advancementData).toLowerCase();
            String displayName = getDisplayName(advancementData).toLowerCase();
            String description = getDescription(advancementData).toLowerCase();

            // 搜索注册ID
            if (registry.contains(lowerKeyword)) {
                return true;
            }
            // 搜索显示名称
            if (displayName.contains(lowerKeyword)) {
                return true;
            }
            // 搜索描述
            if (StringUtils.isNotNullOrEmpty(description) && description.contains(lowerKeyword)) {
                return true;
            }
            return false;
        }
    }

    /**
     * 模糊搜索进度
     * <p>
     * - @：搜索注册ID
     * <p>
     * - $：搜索描述
     *
     * @param keyword 搜索关键字
     * @return 匹配的进度列表
     */
    public static List<AdvancementData> searchAdvancements(String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return getAllAdvancements();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return getAllAdvancements();
        }

        return getAllAdvancements().stream()
                .filter(data -> {
                    if (data == null) return false;
                    return matchesKeyword(data, trimmedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索可显示的进度（有图标的进度）
     * <p>
     * - @：搜索注册ID
     * <p>
     * - $：搜索描述
     *
     * @param keyword 搜索关键字
     * @return 匹配的进度列表
     */
    @OnlyIn(Dist.CLIENT)
    public static List<AdvancementData> searchDisplayableAdvancements(String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return getDisplayableAdvancements();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return getDisplayableAdvancements();
        }

        return getDisplayableAdvancements().stream()
                .filter(data -> {
                    if (data == null) return false;
                    return matchesKeyword(data, trimmedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索进度
     * 支持多个关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - $：搜索描述
     *
     * @param keywords 搜索关键字数组
     * @return 匹配的进度列表
     */
    public static List<AdvancementData> searchAdvancements(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return getAllAdvancements();
        }

        List<String> validKeywords = Arrays.stream(keywords)
                .filter(StringUtils::isNotNullOrEmpty)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        if (validKeywords.isEmpty()) {
            return getAllAdvancements();
        }

        return getAllAdvancements().stream()
                .filter(data -> {
                    if (data == null) return false;
                    // 所有关键字都必须匹配
                    for (String keyword : validKeywords) {
                        if (!matchesKeyword(data, keyword)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据注册ID查找进度
     *
     * @param registry 注册ID字符串
     */
    public static Optional<AdvancementData> findAdvancementByRegistry(String registry) {
        if (StringUtils.isNullOrEmpty(registry)) {
            return Optional.empty();
        }

        return getAllAdvancements().stream()
                .filter(data -> data != null && data.getId().toString().equals(registry))
                .findFirst();
    }

    // endregion 所有进度

}
