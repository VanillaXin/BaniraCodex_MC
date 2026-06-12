package xin.vanilla.banira.common.config;

import lombok.Getter;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 配置持有者，封装配置值后端与元数据，提供统一访问接口。
 */
public class ConfigHolder implements BaniraConfigHandle {

    /**
     * 注册配置时传入的 Mod ID，用于 {@link ConfigEntryDescriptor.ConfigTooltipGuiKind#TRANSLATION_KEY} 等
     */
    @Getter
    private final String modId;

    @Getter
    private final String configName;
    @Getter
    private final ConfigScope configScope;
    @Getter
    private final List<ConfigEntryDescriptor> descriptors;
    private final ConfigValueStore valueStore;
    /**
     * 分类路径 -> 显示名（用于 GUI 层级展示，兼容旧逻辑；配置编辑器折叠标题优先 {@link #categoryTitleSpecs}）
     */
    private final Map<String, String> categoryTooltips;

    /**
     * 分类路径 -> 折叠面板标题元数据（翻译键 / 多语言硬编码 / 字面量）
     */
    private final Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs;

    /**
     * 配置路径 -> 描述符（与 {@link #descriptors} 一致，用于 {@link #get} 对列表做运行时类型归一化）
     */
    private final Map<String, ConfigEntryDescriptor> descriptorByPath;

    /**
     * 供各加载器配置服务创建统一 holder。
     */
    public static ConfigHolder create(String modId, String configName, ConfigScope configScope, ConfigValueStore valueStore,
                                      List<ConfigEntryDescriptor> descriptors,
                                      Map<String, String> categoryTooltips,
                                      Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        return new ConfigHolder(modId, configName, configScope, valueStore, descriptors, categoryTooltips, categoryTitleSpecs);
    }

    ConfigHolder(String modId, String configName, ConfigScope configScope, ConfigValueStore valueStore,
                 List<ConfigEntryDescriptor> descriptors,
                 Map<String, String> categoryTooltips,
                 Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        this.modId = modId != null ? modId : "";
        this.configName = configName;
        this.configScope = configScope;
        this.valueStore = valueStore;
        this.descriptors = Collections.unmodifiableList(descriptors);
        this.categoryTooltips = categoryTooltips != null ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTooltips)) : Collections.emptyMap();
        this.categoryTitleSpecs = categoryTitleSpecs != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTitleSpecs))
                : Collections.emptyMap();
        Map<String, ConfigEntryDescriptor> byPath = new LinkedHashMap<>();
        for (ConfigEntryDescriptor d : descriptors) {
            byPath.put(d.getPath(), d);
        }
        this.descriptorByPath = Collections.unmodifiableMap(byPath);
    }

    /**
     * 获取某分类路径的折叠标题元数据；无记录时配置编辑器可回退 {@link CategoryTreeNode#getDisplayName()}。
     */
    @Nullable
    public ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath) {
        return categoryTitleSpecs.get(categoryPath);
    }

    ConfigValueStore valueStore() {
        return valueStore;
    }

    /**
     * 保存配置到文件
     */
    public void save() {
        valueStore.save();
    }

    /**
     * 获取配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path) {
        if (!valueStore.paths().contains(path)) {
            return null;
        }
        Object v = valueStore.get(path);
        ConfigEntryDescriptor desc = descriptorByPath.get(path);
        if (desc != null && desc.isListType() && v instanceof List) {
            v = ConfigListSpecHelper.normalizeListForRuntime((List<?>) v, desc);
        }
        return (T) v;
    }

    /**
     * 设置配置值（仅内存，需调用 save 持久化）
     */
    public void set(String path, Object value) {
        if (valueStore.paths().contains(path)) {
            valueStore.set(path, value);
        }
    }

    /**
     * 获取配置项描述符
     */
    public ConfigEntryDescriptor getDescriptor(String path) {
        return descriptorByPath.get(path);
    }

    /**
     * 返回所有配置路径；用于指令补全和代理方法解析。
     */
    public Set<String> valuePaths() {
        return valueStore.paths();
    }

    public boolean hasValue(String path) {
        return valueStore.paths().contains(path);
    }

    /**
     * 精确匹配路径；若没有精确命中且模糊结果唯一，则返回该路径。
     */
    @Nullable
    public String findValuePath(String key) {
        if (key == null) {
            return null;
        }
        if (valueStore.paths().contains(key)) {
            return key;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        List<String> matches = valueStore.paths().stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).contains(lowerKey))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    public Class<?> valueClass(String path) {
        return valueStore.valueClass(path);
    }

    @Nullable
    public Object defaultValue(String path) {
        return valueStore.defaultValue(path);
    }

    public boolean validate(String path, Object value) {
        return valueStore.validate(path, value);
    }

    public boolean setIfValid(String path, Object value) {
        if (!validate(path, value)) {
            return false;
        }
        set(path, value);
        return true;
    }

    /**
     * 是否为服务端配置
     */
    public boolean isServerConfig() {
        return configScope == ConfigScope.SERVER;
    }

    /**
     * 是否可同步至服务器（Common 与 Server 配置均可）
     */
    public boolean canSyncToServer() {
        return configScope == ConfigScope.SERVER || configScope == ConfigScope.COMMON;
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
