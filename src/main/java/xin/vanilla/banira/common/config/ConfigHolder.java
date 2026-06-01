package xin.vanilla.banira.common.config;

import lombok.Getter;

import java.util.*;

/**
 * Loader-neutral config holder with metadata and value access.
 */
public class ConfigHolder {
    @Getter
    private final String modId;

    @Getter
    private final String configName;

    @Getter
    private final ConfigScope scope;
    private final ConfigValueBackend backend;

    @Getter
    private final List<ConfigEntryDescriptor> descriptors;

    private final Map<String, String> categoryTooltips;
    private final Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs;
    private final Map<String, ConfigEntryDescriptor> descriptorByPath;

    public ConfigHolder(String modId, String configName, ConfigScope scope, ConfigValueBackend backend,
                        List<ConfigEntryDescriptor> descriptors,
                        Map<String, String> categoryTooltips,
                        Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs) {
        this.modId = modId != null ? modId : "";
        this.configName = configName;
        this.scope = scope != null ? scope : ConfigScope.COMMON;
        this.backend = backend;
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

    public Set<String> getValuePaths() {
        return backend.getValuePaths();
    }

    public ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath) {
        return categoryTitleSpecs.get(categoryPath);
    }

    public void save() {
        backend.save();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String path) {
        if (!backend.getValuePaths().contains(path)) {
            return null;
        }
        Object value = backend.get(path);
        ConfigEntryDescriptor desc = descriptorByPath.get(path);
        if (desc != null && desc.isListType() && value instanceof List) {
            value = ConfigListSpecHelper.normalizeListForRuntime((List<?>) value, desc);
        }
        return (T) value;
    }

    public void set(String path, Object value) {
        if (backend.getValuePaths().contains(path)) {
            backend.set(path, value);
        }
    }

    public ConfigEntryDescriptor getDescriptor(String path) {
        return descriptorByPath.get(path);
    }

    public boolean isServerConfig() {
        return scope == ConfigScope.SERVER;
    }

    public boolean canSyncToServer() {
        return scope == ConfigScope.SERVER || scope == ConfigScope.COMMON;
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
