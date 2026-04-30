package xin.vanilla.banira.modmenu;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.editable.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Mod Menu（Cloth）配置页。
 * <ul>
 *   <li>多份配置（如 client / common）对应 Cloth 左侧<strong>分类 Tab</strong>。
 *     {@link me.shedaniel.autoconfig.AutoConfig#getConfigScreen(Class, Screen)} 仅针对单个 {@link ConfigData}；
 *     原生无法在一条 API 下把多份配置做成多 Tab，多配置仍由此工厂处理。</li>
 *   <li>每份配置内按 {@link EditableConfigHolder#getCategoryTree()} 使用 {@code startSubCategory}，
 *     与 AutoConfig 默认界面中嵌套分组（如 {@code @Gui.CollapsibleObject}）的层级一致。</li>
 * </ul>
 */
public final class ModMenuConfigScreenFactory {
    private ModMenuConfigScreenFactory() {
    }

    @SafeVarargs
    public static Screen create(Screen parent, Class<? extends ConfigData>... configClasses) {
        return create(parent, Arrays.asList(configClasses));
    }

    public static Screen create(Screen parent, List<Class<? extends ConfigData>> configClasses) {
        if (configClasses == null || configClasses.isEmpty()) {
            throw new IllegalArgumentException("configClasses must not be empty");
        }
        LinkedHashSet<Class<? extends ConfigData>> unique = new LinkedHashSet<>(configClasses);
        List<EditableConfigHolder> holders = new ArrayList<>(unique.size());
        for (Class<? extends ConfigData> c : unique) {
            holders.add(EditableConfigRegistry.getRequired(c));
        }
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.autoconfig." + holders.get(0).getModId() + ".menu.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        for (EditableConfigHolder holder : holders) {
            appendHolderCategory(builder, entries, holder, deriveCategoryTitle(holder));
        }
        builder.setSavingRunnable(() -> {
            for (EditableConfigHolder holder : holders) {
                holder.save();
            }
        });
        return builder.build();
    }

    private static Component deriveCategoryTitle(EditableConfigHolder holder) {
        String name = holder.getConfigName();
        String modId = holder.getModId();
        int lastHyphen = name.lastIndexOf('-');
        String tail = lastHyphen >= 0 ? name.substring(lastHyphen + 1) : name.replace('-', '_');
        return Component.translatable("text.autoconfig." + modId + ".navigation." + tail + "_config");
    }

    private static void appendHolderCategory(ConfigBuilder builder, ConfigEntryBuilder entries, EditableConfigHolder holder,
                                             Component categoryTitle) {
        ConfigCategory category = builder.getOrCreateCategory(categoryTitle);
        List<EditableConfigHolder.CategoryTreeNode> roots = holder.getCategoryTree();
        if (roots.isEmpty()) {
            for (ConfigEntryDescriptor desc : holder.getDescriptors()) {
                AbstractConfigListEntry e = buildDescriptorEntry(entries, holder, desc);
                if (e != null) {
                    category.addEntry(e);
                }
            }
            return;
        }
        EditableConfigHolder.CategoryTreeNode root = roots.get(0);
        for (ConfigEntryDescriptor desc : root.getEntries()) {
            AbstractConfigListEntry e = buildDescriptorEntry(entries, holder, desc);
            if (e != null) {
                category.addEntry(e);
            }
        }
        for (EditableConfigHolder.CategoryTreeNode child : root.getChildren()) {
            category.addEntry(buildSubCategoryTree(entries, holder, child));
        }
    }

    private static Component resolveCategoryNodeTitle(EditableConfigHolder holder, EditableConfigHolder.CategoryTreeNode node) {
        String catPath = node.getCategoryPath();
        if (catPath == null || catPath.isEmpty()) {
            return Component.literal(holder.getConfigName());
        }
        ConfigCategoryTitleSpec spec = holder.getCategoryTitleSpec(catPath);
        if (spec != null) {
            switch (spec.getKind()) {
                case TRANSLATION_KEY -> {
                    String k = spec.getTranslationKey();
                    if (k != null && !k.isEmpty()) {
                        return Component.translatable(k);
                    }
                }
                case LITERAL -> {
                    if (!spec.getLiteral().isEmpty()) {
                        return Component.literal(spec.getLiteral());
                    }
                }
                case LOCALIZED_STATIC -> {
                    for (String v : spec.getLocalizedByLang().values()) {
                        if (v != null && !v.trim().isEmpty()) {
                            return Component.literal(v);
                        }
                    }
                }
            }
        }
        return Component.translatable("text.autoconfig." + holder.getConfigName() + ".option." + catPath);
    }

    private static AbstractConfigListEntry buildSubCategoryTree(ConfigEntryBuilder entries, EditableConfigHolder holder,
                                                                EditableConfigHolder.CategoryTreeNode node) {
        var sub = entries.startSubCategory(resolveCategoryNodeTitle(holder, node));
        sub.setExpanded(false);
        for (ConfigEntryDescriptor desc : node.getEntries()) {
            AbstractConfigListEntry e = buildDescriptorEntry(entries, holder, desc);
            if (e != null) {
                sub.add(e);
            }
        }
        for (EditableConfigHolder.CategoryTreeNode child : node.getChildren()) {
            sub.add(buildSubCategoryTree(entries, holder, child));
        }
        return sub.build();
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AbstractConfigListEntry buildDescriptorEntry(ConfigEntryBuilder entries, EditableConfigHolder holder,
                                                                ConfigEntryDescriptor desc) {
        String path = desc.getPath();
        String configName = holder.getConfigName();
        Component label = Component.translatable("text.autoconfig." + configName + ".option." + path);

        if (ConfigEntryDescriptor.isListValueType(desc.getValueType())) {
            return buildListDescriptorEntry(entries, holder, desc, label);
        }

        switch (desc.getValueType()) {
            case STRING -> {
                String cur = (String) nonNullCurrent(holder, path, desc);
                String def = (String) desc.getDefaultValue();
                return entries.startStrField(label, cur)
                        .setDefaultValue(def != null ? def : "")
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build();
            }
            case BOOLEAN -> {
                boolean cur = (Boolean) nonNullCurrent(holder, path, desc);
                boolean def = desc.getDefaultValue() instanceof Boolean b ? b : false;
                return entries.startBooleanToggle(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build();
            }
            case INTEGER -> {
                int cur = numberToInt(nonNullCurrent(holder, path, desc));
                int def = numberToInt(desc.getDefaultValue());
                var b = entries.startIntField(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isUnboundedIntPair(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().intValue()).setMax(desc.getMaxValue().intValue());
                }
                return b.setSaveConsumer(v -> holder.set(path, v)).build();
            }
            case LONG -> {
                long cur = numberToLong(nonNullCurrent(holder, path, desc));
                long def = numberToLong(desc.getDefaultValue());
                var b = entries.startLongField(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isUnboundedLongPair(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().longValue()).setMax(desc.getMaxValue().longValue());
                }
                return b.setSaveConsumer(v -> holder.set(path, v)).build();
            }
            case DOUBLE -> {
                double cur = numberToDouble(nonNullCurrent(holder, path, desc));
                double def = numberToDouble(desc.getDefaultValue());
                var b = entries.startDoubleField(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isPlaceholderUnboundedDoubleBounds(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().doubleValue()).setMax(desc.getMaxValue().doubleValue());
                }
                return b.setSaveConsumer(v -> holder.set(path, v)).build();
            }
            case ENUM -> {
                Class enumClass = desc.getEnumClass();
                if (enumClass == null) {
                    return null;
                }
                Object[] constants = enumClass.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    return null;
                }
                Object raw = nonNullCurrent(holder, path, desc);
                Enum<?> cur = raw instanceof Enum<?> e ? e : (Enum<?>) constants[0];
                Object defObj = desc.getDefaultValue();
                Enum<?> def = defObj instanceof Enum<?> ev ? ev : (Enum<?>) constants[0];
                return entries.startEnumSelector(label, enumClass, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build();
            }
            default -> {
                return null;
            }
        }
    }

    // region 列表

    @Nullable
    private static AbstractConfigListEntry buildListDescriptorEntry(ConfigEntryBuilder entries, EditableConfigHolder holder,
                                                                    ConfigEntryDescriptor desc, Component label) {
        String path = desc.getPath();
        List<?> curNorm = normalizedRuntimeList(holder, path, desc);
        List<?> defNorm = normalizedDefaultList(desc);

        switch (desc.getValueType()) {
            case STRING_LIST -> {
                List<String> cur = new ArrayList<>(curNorm.size());
                for (Object o : curNorm) {
                    cur.add(o != null ? String.valueOf(o) : "");
                }
                List<String> def = new ArrayList<>(defNorm.size());
                for (Object o : defNorm) {
                    def.add(o != null ? String.valueOf(o) : "");
                }
                return entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            case INTEGER_LIST -> {
                List<Integer> cur = toMutableIntegerList(curNorm);
                List<Integer> def = toMutableIntegerList(defNorm);
                var b = entries.startIntList(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isUnboundedIntPair(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().intValue()).setMax(desc.getMaxValue().intValue());
                }
                return b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            case LONG_LIST -> {
                List<Long> cur = toMutableLongList(curNorm);
                List<Long> def = toMutableLongList(defNorm);
                var b = entries.startLongList(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isUnboundedLongPair(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().longValue()).setMax(desc.getMaxValue().longValue());
                }
                return b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            case DOUBLE_LIST -> {
                List<Double> cur = toMutableDoubleList(curNorm);
                List<Double> def = toMutableDoubleList(defNorm);
                var b = entries.startDoubleList(label, cur)
                        .setDefaultValue(def);
                if (desc.getMinValue() != null && desc.getMaxValue() != null
                        && !isPlaceholderUnboundedDoubleBounds(desc.getMinValue(), desc.getMaxValue())) {
                    b.setMin(desc.getMinValue().doubleValue()).setMax(desc.getMaxValue().doubleValue());
                }
                return b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            case BOOLEAN_LIST -> {
                List<String> cur = new ArrayList<>(curNorm.size());
                for (Object o : curNorm) {
                    cur.add(coerceBoolListCell(o));
                }
                List<String> def = new ArrayList<>(defNorm.size());
                for (Object o : defNorm) {
                    def.add(coerceBoolListCell(o));
                }
                return entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            case ENUM_LIST -> {
                Class<? extends Enum<?>> ec = desc.getEnumClass();
                if (ec == null) {
                    return null;
                }
                List<String> cur = enumNameStrings(curNorm);
                List<String> def = enumNameStrings(defNorm);
                return entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build();
            }
            default -> {
                return null;
            }
        }
    }

    private static void saveListFromGuiObjects(EditableConfigHolder holder, String path, ConfigEntryDescriptor desc,
                                               List<Object> guiItems) {
        holder.set(path, ConfigListSpecHelper.listFromGuiItems(guiItems, desc));
    }

    private static List<Object> asObjectList(List<?> src) {
        List<Object> out = new ArrayList<>(src.size());
        out.addAll(src);
        return out;
    }

    private static List<?> normalizedRuntimeList(EditableConfigHolder holder, String path, ConfigEntryDescriptor desc) {
        Object raw = holder.get(path);
        if (!(raw instanceof List<?> listRaw)) {
            return Collections.emptyList();
        }
        return ConfigListSpecHelper.normalizeListForRuntime(new ArrayList<>(listRaw), desc);
    }

    private static List<?> normalizedDefaultList(ConfigEntryDescriptor desc) {
        Object def = desc.getDefaultValue();
        if (!(def instanceof List<?> dl)) {
            return Collections.emptyList();
        }
        return ConfigListSpecHelper.normalizeListForRuntime(new ArrayList<>(dl), desc);
    }

    private static List<Integer> toMutableIntegerList(List<?> normalized) {
        List<Integer> out = new ArrayList<>(normalized.size());
        for (Object o : normalized) {
            if (o instanceof Number n) {
                out.add(n.intValue());
            } else if (o != null) {
                try {
                    out.add(Integer.parseInt(o.toString().trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private static List<Long> toMutableLongList(List<?> normalized) {
        List<Long> out = new ArrayList<>(normalized.size());
        for (Object o : normalized) {
            if (o instanceof Number n) {
                out.add(n.longValue());
            } else if (o != null) {
                try {
                    out.add(Long.parseLong(o.toString().trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private static List<Double> toMutableDoubleList(List<?> normalized) {
        List<Double> out = new ArrayList<>(normalized.size());
        for (Object o : normalized) {
            if (o instanceof Number n) {
                out.add(n.doubleValue());
            } else if (o != null) {
                try {
                    out.add(Double.parseDouble(o.toString().trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private static List<String> enumNameStrings(List<?> normalized) {
        List<String> out = new ArrayList<>(normalized.size());
        for (Object o : normalized) {
            if (o instanceof Enum<?> en) {
                out.add(en.name());
            }
        }
        return out;
    }

    private static String coerceBoolListCell(Object o) {
        if (o instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (o instanceof String s) {
            String t = s.trim();
            if ("true".equalsIgnoreCase(t)) {
                return "true";
            }
            if ("false".equalsIgnoreCase(t)) {
                return "false";
            }
        }
        return "false";
    }

    // endregion 列表

    private static Object nonNullCurrent(EditableConfigHolder holder, String path, ConfigEntryDescriptor desc) {
        Object v = holder.get(path);
        if (v != null) {
            return v;
        }
        return desc.getDefaultValue();
    }

    private static int numberToInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static long numberToLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private static double numberToDouble(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private static boolean isUnboundedIntPair(Number min, Number max) {
        return min.intValue() == Integer.MIN_VALUE && max.intValue() == Integer.MAX_VALUE;
    }

    private static boolean isUnboundedLongPair(Number min, Number max) {
        return min.longValue() == Long.MIN_VALUE && max.longValue() == Long.MAX_VALUE;
    }

    private static boolean isPlaceholderUnboundedDoubleBounds(Number min, Number max) {
        if (min == null || max == null) {
            return false;
        }
        return min.doubleValue() == Double.MIN_VALUE && max.doubleValue() == Double.MAX_VALUE;
    }
}
