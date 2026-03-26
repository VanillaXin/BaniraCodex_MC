package xin.vanilla.banira.common.config;

import lombok.Getter;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 配置持有者，封装 ModConfigSpec 与元数据，提供统一访问接口
 */
@Getter
public class ConfigHolder {

    /**
     * 注册配置时传入的 Mod ID，用于 {@link ConfigEntryDescriptor.ConfigTooltipGuiKind#TRANSLATION_KEY} 等
     */
    private final String modId;

    private final String configName;
    private final ModConfig.Type configType;
    private final ModConfigSpec spec;
    private final List<ConfigEntryDescriptor> descriptors;
    private final Map<String, ModConfigSpec.ConfigValue<?>> valueMap;
    /**
     * 分类路径 -> 显示名（用于 GUI 层级展示，兼容旧逻辑；配置编辑器折叠标题优先 {@link #categoryTitleSpecs}）
     */
    private final Map<String, String> categoryTooltips;

    /**
     * 分类路径 -> 折叠面板标题元数据（翻译键 / 多语言硬编码 / 字面量）
     */
    private final Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs;

    @Nullable
    private ModConfig modConfig;

    ConfigHolder(String modId, String configName, ModConfig.Type configType, ModConfigSpec spec,
                 List<ConfigEntryDescriptor> descriptors, Map<String, ModConfigSpec.ConfigValue<?>> valueMap,
                 Map<String, String> categoryTooltips,
                 Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        this.modId = modId != null ? modId : "";
        this.configName = configName;
        this.configType = configType;
        this.spec = spec;
        this.descriptors = Collections.unmodifiableList(descriptors);
        this.valueMap = Collections.unmodifiableMap(valueMap);
        this.categoryTooltips = categoryTooltips != null ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTooltips)) : Collections.emptyMap();
        this.categoryTitleSpecs = categoryTitleSpecs != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTitleSpecs))
                : Collections.emptyMap();
    }

    /**
     * 获取某分类路径的折叠标题元数据；无记录时配置编辑器可回退 {@link CategoryTreeNode#getDisplayName()}。
     */
    @Nullable
    public ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath) {
        return categoryTitleSpecs.get(categoryPath);
    }

    void setModConfig(@Nullable ModConfig modConfig) {
        this.modConfig = modConfig;
    }

    /**
     * 保存配置到文件
     */
    public void save() {
        if (modConfig != null) {
            modConfig.save();
        }
    }

    /**
     * 获取配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path) {
        ModConfigSpec.ConfigValue<?> cv = valueMap.get(path);
        return cv != null ? (T) cv.get() : null;
    }

    /**
     * 设置配置值（仅内存，需调用 save 持久化）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void set(String path, Object value) {
        ModConfigSpec.ConfigValue cv = valueMap.get(path);
        if (cv != null) {
            cv.set(value);
        }
    }

    /**
     * 获取配置项描述符
     */
    public ConfigEntryDescriptor getDescriptor(String path) {
        return descriptors.stream()
                .filter(d -> d.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否为服务端配置
     */
    public boolean isServerConfig() {
        return configType == ModConfig.Type.SERVER;
    }

    /**
     * 是否可同步至服务器（Common 与 Server 配置均可）
     */
    public boolean canSyncToServer() {
        return configType == ModConfig.Type.SERVER || configType == ModConfig.Type.COMMON;
    }

    /**
     * 按分类分组获取配置项，用于 GUI 层级展示（兼容旧逻辑，仅深度1）。
     * 返回顺序：先按 categoryTooltips 中的分类顺序，再按 descriptors 中的顺序。
     */
    public List<CategoryGroup> getDescriptorsGroupedByCategory() {
        Map<String, List<ConfigEntryDescriptor>> byCategory = new LinkedHashMap<>();
        for (ConfigEntryDescriptor d : descriptors) {
            String path = d.getPath();
            int dot = path.indexOf('.');
            String category = dot > 0 ? path.substring(0, dot) : "";
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(d);
        }
        List<CategoryGroup> result = new ArrayList<>();
        Set<String> orderedCategories = new LinkedHashSet<>(categoryTooltips.keySet());
        orderedCategories.addAll(byCategory.keySet());
        for (String cat : orderedCategories) {
            List<ConfigEntryDescriptor> entries = byCategory.get(cat);
            if (entries != null && !entries.isEmpty()) {
                String displayName = categoryTooltips.getOrDefault(cat, cat);
                result.add(new CategoryGroup(cat, displayName, entries));
            }
        }
        return result;
    }

    /**
     * 虚拟根节点路径，不渲染该节点本身，仅作为树的根
     */
    private static final String VIRTUAL_ROOT_PATH = "";

    /**
     * 获取多层级分类树，支持任意深度嵌套。
     * 所有节点统一挂在虚拟根节点下，虚拟根不渲染，仅渲染其 entries 与 children。
     */
    public List<CategoryTreeNode> getCategoryTree() {
        Map<String, List<ConfigEntryDescriptor>> byCategory = new LinkedHashMap<>();
        for (ConfigEntryDescriptor d : descriptors) {
            String path = d.getPath();
            int lastDot = path.lastIndexOf('.');
            String categoryPath = lastDot > 0 ? path.substring(0, lastDot) : VIRTUAL_ROOT_PATH;
            byCategory.computeIfAbsent(categoryPath, k -> new ArrayList<>()).add(d);
        }
        Set<String> allCategories = new LinkedHashSet<>(categoryTooltips.keySet());
        allCategories.addAll(byCategory.keySet());
        allCategories.add(VIRTUAL_ROOT_PATH);
        Map<String, CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        for (String cat : allCategories) {
            String displayName = categoryTooltips.getOrDefault(cat, cat);
            List<ConfigEntryDescriptor> entries = byCategory.getOrDefault(cat, Collections.emptyList());
            nodeMap.put(cat, new CategoryTreeNode(cat, displayName, entries));
        }
        for (CategoryTreeNode node : nodeMap.values()) {
            String parentPath = getParentPath(node.getCategoryPath());
            CategoryTreeNode parent = nodeMap.get(parentPath);
            if (parent != null && parent != node) {
                parent.addChild(node);
            }
        }
        CategoryTreeNode virtualRoot = nodeMap.get(VIRTUAL_ROOT_PATH);
        return virtualRoot != null ? Collections.singletonList(virtualRoot) : Collections.emptyList();
    }

    private static String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : "";
    }

    /**
     * 配置分类组（兼容旧 API）
     */
    @Getter
    public static class CategoryGroup {
        private final String categoryPath;
        private final String displayName;
        private final List<ConfigEntryDescriptor> entries;

        public CategoryGroup(String categoryPath, String displayName, List<ConfigEntryDescriptor> entries) {
            this.categoryPath = categoryPath;
            this.displayName = displayName;
            this.entries = List.copyOf(entries);
        }
    }

    /**
     * 多层级分类树节点
     */
    @Getter
    public static class CategoryTreeNode {
        private final String categoryPath;
        private final String displayName;
        private final List<ConfigEntryDescriptor> entries;
        private final List<CategoryTreeNode> children = new ArrayList<>();

        public CategoryTreeNode(String categoryPath, String displayName, List<ConfigEntryDescriptor> entries) {
            this.categoryPath = categoryPath;
            this.displayName = displayName;
            this.entries = List.copyOf(entries);
        }

        void addChild(CategoryTreeNode child) {
            children.add(child);
        }
    }
}
