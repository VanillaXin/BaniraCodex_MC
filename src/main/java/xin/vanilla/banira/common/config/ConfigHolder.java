package xin.vanilla.banira.common.config;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 配置持有者，封装配置值后端与元数据，提供统一访问接口。
 */
public class ConfigHolder implements BaniraConfigHandle {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 注册配置时传入的 Mod ID，用于 {@link ConfigEntryDescriptor.ConfigTooltipGuiKind#TRANSLATION_KEY} 等。
     */
    @Getter
    private final String modId;

    @Getter
    private final String configName;

    @Getter
    private final ConfigScope configScope;

    private final ConfigValueStore valueStore;

    @Getter
    private final List<ConfigEntryDescriptor> descriptors;

    /**
     * 分类路径 -> 显示名；配置编辑器折叠标题优先使用 {@link #categoryTitleSpecs}。
     */
    private final Map<String, String> categoryTooltips;

    /**
     * 分类路径 -> 折叠面板标题元数据。
     */
    private final Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs;

    /**
     * 配置路径 -> 描述符，用于 GUI 和列表运行时归一化。
     */
    private final Map<String, ConfigEntryDescriptor> descriptorByPath;

    private final Set<String> pendingChangedPaths = new LinkedHashSet<>();
    private final List<Consumer<Set<String>>> savedListeners = new CopyOnWriteArrayList<>();

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
        this.configScope = configScope != null ? configScope : ConfigScope.COMMON;
        this.valueStore = valueStore;
        this.descriptors = Collections.unmodifiableList(new ArrayList<>(descriptors));
        this.categoryTooltips = categoryTooltips != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTooltips))
                : Collections.emptyMap();
        this.categoryTitleSpecs = categoryTitleSpecs != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(categoryTitleSpecs))
                : Collections.emptyMap();

        Map<String, ConfigEntryDescriptor> byPath = new LinkedHashMap<>();
        for (ConfigEntryDescriptor descriptor : descriptors) {
            byPath.put(descriptor.getPath(), descriptor);
        }
        this.descriptorByPath = Collections.unmodifiableMap(byPath);
    }

    ConfigValueStore valueStore() {
        return valueStore;
    }

    @Override
    public Set<String> valuePaths() {
        return valueStore.paths();
    }

    @Nullable
    public ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath) {
        return categoryTitleSpecs.get(categoryPath);
    }

    public synchronized void save() {
        valueStore.save();
        if (pendingChangedPaths.isEmpty()) {
            return;
        }
        Set<String> changedPaths = Collections.unmodifiableSet(new LinkedHashSet<>(pendingChangedPaths));
        pendingChangedPaths.clear();
        for (Consumer<Set<String>> listener : savedListeners) {
            try {
                listener.accept(changedPaths);
            } catch (RuntimeException ex) {
                LOGGER.error("Config saved listener failed for {}", configName, ex);
            }
        }
    }

    /**
     * 保存成功且存在实际写入时触发；返回值用于注销监听。
     */
    public Runnable onSaved(Consumer<Set<String>> listener) {
        Objects.requireNonNull(listener, "listener");
        savedListeners.add(listener);
        return () -> savedListeners.remove(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String path) {
        if (!valueStore.paths().contains(path)) {
            return null;
        }
        Object value = valueStore.get(path);
        ConfigEntryDescriptor desc = descriptorByPath.get(path);
        if (desc != null && desc.isListType() && value instanceof List) {
            value = ConfigListSpecHelper.normalizeListForRuntime((List<?>) value, desc);
        }
        return (T) value;
    }

    @Override
    public synchronized void set(String path, Object value) {
        if (valueStore.paths().contains(path)) {
            Object previous = valueStore.get(path);
            valueStore.set(path, value);
            if (!Objects.deepEquals(previous, valueStore.get(path))) {
                pendingChangedPaths.add(path);
            }
        }
    }

    @Override
    public boolean hasValue(String path) {
        return valueStore.paths().contains(path);
    }

    @Nullable
    @Override
    public String findValuePath(String key) {
        if (key == null) {
            return null;
        }
        if (valueStore.paths().contains(key)) {
            return key;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        String match = null;
        for (String path : valueStore.paths()) {
            if (!path.toLowerCase(Locale.ROOT).contains(lowerKey)) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = path;
        }
        return match;
    }

    @Override
    public Class<?> valueClass(String path) {
        return valueStore.valueClass(path);
    }

    @Nullable
    @Override
    public Object defaultValue(String path) {
        return valueStore.defaultValue(path);
    }

    @Override
    public boolean validate(String path, Object value) {
        return valueStore.validate(path, value);
    }

    @Override
    public boolean setIfValid(String path, Object value) {
        if (!validate(path, value)) {
            return false;
        }
        set(path, value);
        return true;
    }

    public ConfigEntryDescriptor getDescriptor(String path) {
        return descriptorByPath.get(path);
    }

    public boolean isServerConfig() {
        return configScope == ConfigScope.SERVER;
    }

    public boolean canSyncToServer() {
        return configScope == ConfigScope.SERVER || configScope == ConfigScope.COMMON;
    }

    public List<CategoryGroup> getDescriptorsGroupedByCategory() {
        Map<String, List<ConfigEntryDescriptor>> byCategory = new LinkedHashMap<>();
        for (ConfigEntryDescriptor descriptor : descriptors) {
            String path = descriptor.getPath();
            int dot = path.indexOf('.');
            String category = dot > 0 ? path.substring(0, dot) : "";
            byCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(descriptor);
        }

        List<CategoryGroup> result = new ArrayList<>();
        Set<String> orderedCategories = new LinkedHashSet<>(categoryTooltips.keySet());
        orderedCategories.addAll(byCategory.keySet());
        for (String category : orderedCategories) {
            List<ConfigEntryDescriptor> entries = byCategory.get(category);
            if (entries != null && !entries.isEmpty()) {
                String displayName = categoryTooltips.getOrDefault(category, category);
                result.add(new CategoryGroup(category, displayName, entries));
            }
        }
        return result;
    }

    public List<CategoryTreeNode> getCategoryTree() {
        Map<String, List<ConfigEntryDescriptor>> byCategory = new LinkedHashMap<>();
        for (ConfigEntryDescriptor descriptor : descriptors) {
            String path = descriptor.getPath();
            int lastDot = path.lastIndexOf('.');
            String categoryPath = lastDot > 0 ? path.substring(0, lastDot) : "";
            byCategory.computeIfAbsent(categoryPath, key -> new ArrayList<>()).add(descriptor);
        }

        Set<String> allCategories = new LinkedHashSet<>(categoryTooltips.keySet());
        allCategories.addAll(byCategory.keySet());
        allCategories.add("");

        Map<String, CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        for (String category : allCategories) {
            String displayName = categoryTooltips.getOrDefault(category, category);
            List<ConfigEntryDescriptor> entries = byCategory.getOrDefault(category, Collections.emptyList());
            nodeMap.put(category, new CategoryTreeNode(category, displayName, entries));
        }

        for (CategoryTreeNode node : nodeMap.values()) {
            String parentPath = getParentPath(node.getCategoryPath());
            CategoryTreeNode parent = nodeMap.get(parentPath);
            if (parent != null && parent != node) {
                parent.addChild(node);
            }
        }

        CategoryTreeNode virtualRoot = nodeMap.get("");
        return virtualRoot != null ? Collections.singletonList(virtualRoot) : Collections.emptyList();
    }

    private static String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : "";
    }

    @Getter
    public static class CategoryGroup {
        private final String categoryPath;
        private final String displayName;
        private final List<ConfigEntryDescriptor> entries;

        public CategoryGroup(String categoryPath, String displayName, List<ConfigEntryDescriptor> entries) {
            this.categoryPath = categoryPath;
            this.displayName = displayName;
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    @Getter
    public static class CategoryTreeNode {
        private final String categoryPath;
        private final String displayName;
        private final List<ConfigEntryDescriptor> entries;
        private final List<CategoryTreeNode> children = new ArrayList<>();

        public CategoryTreeNode(String categoryPath, String displayName, List<ConfigEntryDescriptor> entries) {
            this.categoryPath = categoryPath;
            this.displayName = displayName;
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        }

        void addChild(CategoryTreeNode child) {
            children.add(child);
        }
    }
}
