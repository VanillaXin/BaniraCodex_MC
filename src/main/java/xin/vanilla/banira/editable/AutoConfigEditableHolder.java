package xin.vanilla.banira.editable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 基于 AutoConfig / Cloth Config 的 {@link ConfigData} 实例，为 Banira 配置编辑器提供与路径字段链绑定的读写与分类树。
 */
public final class AutoConfigEditableHolder implements EditableConfigHolder {

    private static final String VIRTUAL_ROOT_PATH = "";

    private final String modId;
    private final String configName;
    private final boolean syncToServer;
    private final ConfigHolder<?> autoHolder;
    private final List<ConfigEntryDescriptor> descriptors;
    private final Map<String, ConfigEntryDescriptor> descriptorByPath;
    private final Map<String, Field[]> bindingsByPath;
    private final Map<String, String> categoryTooltips;
    private final Map<String, ConfigCategoryTitleSpec> categoryTitleSpecs;

    public AutoConfigEditableHolder(
            String modId,
            String configName,
            boolean syncToServer,
            ConfigHolder<?> autoHolder,
            ConfigFieldStructure.Result structure
    ) {
        this.modId = modId != null ? modId : "";
        this.configName = configName;
        this.syncToServer = syncToServer;
        this.autoHolder = autoHolder;
        this.descriptors = structure.descriptors();
        this.categoryTooltips = structure.categoryTooltips();
        this.categoryTitleSpecs = structure.categoryTitleSpecs();
        this.bindingsByPath = structure.bindingsByPath();
        Map<String, ConfigEntryDescriptor> byPath = new LinkedHashMap<>();
        for (ConfigEntryDescriptor d : descriptors) {
            byPath.put(d.getPath(), d);
        }
        this.descriptorByPath = Collections.unmodifiableMap(byPath);
    }

    public ConfigHolder<?> autoHolder() {
        return autoHolder;
    }

    @Override
    public String getModId() {
        return modId;
    }

    @Override
    public String getConfigName() {
        return configName;
    }

    @Override
    public boolean canSyncToServer() {
        return syncToServer;
    }

    @Override
    public void save() {
        autoHolder.save();
    }

    @Override
    public void validateAfterChanges() {
        Object c = autoHolder.getConfig();
        if (c instanceof ConfigData cd) {
            try {
                cd.validatePostLoad();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String path) {
        Field[] chain = bindingsByPath.get(path);
        if (chain == null) {
            return null;
        }
        try {
            Object parent = autoHolder.getConfig();
            for (int i = 0; i < chain.length - 1; i++) {
                Field f = chain[i];
                f.setAccessible(true);
                Object next = f.get(parent);
                if (next == null) {
                    return null;
                }
                parent = next;
            }
            Field leaf = chain[chain.length - 1];
            leaf.setAccessible(true);
            Object v = leaf.get(parent);
            ConfigEntryDescriptor desc = descriptorByPath.get(path);
            if (desc != null && desc.isListType() && v instanceof List) {
                v = ConfigListSpecHelper.normalizeListForRuntime((List<?>) v, desc);
            }
            if (desc != null && desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE && v instanceof Number n) {
                v = n.doubleValue();
            }
            return (T) v;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(String path, Object value) {
        Field[] chain = bindingsByPath.get(path);
        if (chain == null) {
            return;
        }
        try {
            Object parent = autoHolder.getConfig();
            for (int i = 0; i < chain.length - 1; i++) {
                Field f = chain[i];
                f.setAccessible(true);
                Object next = f.get(parent);
                if (next == null) {
                    Class<?> ft = f.getType();
                    next = tryNewInstance(ft);
                    if (next == null) {
                        return;
                    }
                    f.set(parent, next);
                }
                parent = next;
            }
            Field leaf = chain[chain.length - 1];
            leaf.setAccessible(true);
            Object coerced = coerceForLeaf(leaf, descriptorByPath.get(path), value);
            leaf.set(parent, coerced);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object coerceForLeaf(Field leaf, @Nullable ConfigEntryDescriptor desc, Object value) {
        Class<?> t = leaf.getType();
        if (value == null) {
            return null;
        }
        if (t == float.class || t == Float.class) {
            if (value instanceof Number n) {
                return n.floatValue();
            }
            if (value instanceof String s) {
                return Float.parseFloat(s);
            }
        }
        if (t == double.class || t == Double.class) {
            if (value instanceof Number n) {
                return n.doubleValue();
            }
        }
        if (t == int.class || t == Integer.class) {
            if (value instanceof Number n) {
                return n.intValue();
            }
        }
        if (t == long.class || t == Long.class) {
            if (value instanceof Number n) {
                return n.longValue();
            }
        }
        if (desc != null && desc.isListType() && value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) value;
            return ConfigListSpecHelper.listFromGuiItems(items, desc);
        }
        return value;
    }

    private static Object tryNewInstance(Class<?> ft) throws ReflectiveOperationException {
        if (ft.isInterface() || java.lang.reflect.Modifier.isAbstract(ft.getModifiers())) {
            return null;
        }
        var ctor = ft.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Override
    public ConfigEntryDescriptor getDescriptor(String path) {
        return descriptorByPath.get(path);
    }

    @Override
    public List<ConfigEntryDescriptor> getDescriptors() {
        return descriptors;
    }

    @Nullable
    @Override
    public ConfigCategoryTitleSpec getCategoryTitleSpec(String categoryPath) {
        return categoryTitleSpecs.get(categoryPath);
    }

    @Override
    public List<EditableConfigHolder.CategoryTreeNode> getCategoryTree() {
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
        Map<String, EditableConfigHolder.CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        for (String cat : allCategories) {
            String displayName = categoryTooltips.getOrDefault(cat, cat);
            List<ConfigEntryDescriptor> entries = byCategory.getOrDefault(cat, Collections.emptyList());
            nodeMap.put(cat, new EditableConfigHolder.CategoryTreeNode(cat, displayName, entries));
        }
        for (EditableConfigHolder.CategoryTreeNode node : nodeMap.values()) {
            String parentPath = parentPath(node.getCategoryPath());
            EditableConfigHolder.CategoryTreeNode parent = nodeMap.get(parentPath);
            if (parent != null && parent != node) {
                parent.addChild(node);
            }
        }
        EditableConfigHolder.CategoryTreeNode virtualRoot = nodeMap.get(VIRTUAL_ROOT_PATH);
        return virtualRoot != null ? Collections.singletonList(virtualRoot) : Collections.emptyList();
    }

    private static String parentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(0, lastDot) : VIRTUAL_ROOT_PATH;
    }
}
