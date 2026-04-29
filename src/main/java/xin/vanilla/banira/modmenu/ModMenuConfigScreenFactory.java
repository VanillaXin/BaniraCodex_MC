package xin.vanilla.banira.modmenu;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.editable.ConfigEntryDescriptor;
import xin.vanilla.banira.editable.ConfigListSpecHelper;
import xin.vanilla.banira.editable.EditableConfigHolder;
import xin.vanilla.banira.editable.EditableConfigRegistry;

import java.util.*;

/**
 * Mod Menu（Cloth）配置页：对已注册 {@link EditableConfigRegistry} 的配置类自动生成条目，用法与 {@link xin.vanilla.banira.client.gui.ConfigEditorScreen#open(Class, net.minecraft.client.gui.screens.Screen)} 类似，
 * 通过配置类列表描述要并排展示的几份配置文件。
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

    /**
     * 分类折叠标题：{@code text.autoconfig.<modId>.navigation.<后缀>_config}，
     * 其中后缀为 {@link EditableConfigHolder#getConfigName()} 最后一个 {@code '-'} 之后一段（本项目：{@code banira_codex-client} → {@code client_config}）。
     */
    private static Component deriveCategoryTitle(EditableConfigHolder holder) {
        String name = holder.getConfigName();
        String modId = holder.getModId();
        int lastHyphen = name.lastIndexOf('-');
        String tail = lastHyphen >= 0 ? name.substring(lastHyphen + 1) : name.replace('-', '_');
        return Component.translatable("text.autoconfig." + modId + ".navigation." + tail + "_config");
    }

    // region 从 EditableConfigHolder 动态生成 Cloth 条目

    private static void appendHolderCategory(ConfigBuilder builder, ConfigEntryBuilder entries, EditableConfigHolder holder,
                                             Component categoryTitle) {
        ConfigCategory category = builder.getOrCreateCategory(categoryTitle);
        for (ConfigEntryDescriptor desc : holder.getDescriptors()) {
            addDescriptorEntry(category, entries, holder, desc);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addDescriptorEntry(ConfigCategory category, ConfigEntryBuilder entries, EditableConfigHolder holder,
                                           ConfigEntryDescriptor desc) {
        String path = desc.getPath();
        String configName = holder.getConfigName();
        Component label = Component.translatable("text.autoconfig." + configName + ".option." + path);

        if (ConfigEntryDescriptor.isListValueType(desc.getValueType())) {
            addListDescriptorEntry(category, entries, holder, desc, label);
            return;
        }

        switch (desc.getValueType()) {
            case STRING -> {
                String cur = (String) nonNullCurrent(holder, path, desc);
                String def = (String) desc.getDefaultValue();
                category.addEntry(entries.startStrField(label, cur)
                        .setDefaultValue(def != null ? def : "")
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build());
            }
            case BOOLEAN -> {
                boolean cur = (Boolean) nonNullCurrent(holder, path, desc);
                boolean def = desc.getDefaultValue() instanceof Boolean b ? b : false;
                category.addEntry(entries.startBooleanToggle(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build());
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
                category.addEntry(b.setSaveConsumer(v -> holder.set(path, v)).build());
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
                category.addEntry(b.setSaveConsumer(v -> holder.set(path, v)).build());
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
                category.addEntry(b.setSaveConsumer(v -> holder.set(path, v)).build());
            }
            case ENUM -> {
                Class enumClass = desc.getEnumClass();
                if (enumClass == null) {
                    return;
                }
                Object[] constants = enumClass.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    return;
                }
                Object raw = nonNullCurrent(holder, path, desc);
                Enum<?> cur = raw instanceof Enum<?> e ? e : (Enum<?>) constants[0];
                Object defObj = desc.getDefaultValue();
                Enum<?> def = defObj instanceof Enum<?> ev ? ev : (Enum<?>) constants[0];
                category.addEntry(entries.startEnumSelector(label, enumClass, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> holder.set(path, v))
                        .build());
            }
            default -> {
            }
        }
    }

    // region 列表（Cloth List + ConfigListSpecHelper，与 Banira 配置编辑器语义一致）

    private static void addListDescriptorEntry(ConfigCategory category, ConfigEntryBuilder entries, EditableConfigHolder holder,
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
                category.addEntry(entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
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
                category.addEntry(b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
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
                category.addEntry(b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
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
                category.addEntry(b.setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
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
                category.addEntry(entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
            }
            case ENUM_LIST -> {
                Class<? extends Enum<?>> ec = desc.getEnumClass();
                if (ec == null) {
                    return;
                }
                List<String> cur = enumNameStrings(curNorm);
                List<String> def = enumNameStrings(defNorm);
                category.addEntry(entries.startStrList(label, cur)
                        .setDefaultValue(def)
                        .setSaveConsumer(v -> saveListFromGuiObjects(holder, path, desc, asObjectList(v)))
                        .build());
            }
            default -> {
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

    /**
     * Cloth 无相邻布尔列表控件，沿用文本格并在保存时经由 {@link ConfigListSpecHelper#listFromGuiItems} 解析。
     */
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

    // endregion 列表（Cloth List + ConfigListSpecHelper，与 Banira 配置编辑器语义一致）

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

    /**
     * {@link xin.vanilla.banira.editable.ConfigFieldStructure} 对未标注 {@code @BoundedDouble} 的 double 使用
     * {@code Double.MIN_VALUE} / {@code Double.MAX_VALUE} 作为占位，不宜交给 Cloth 作为实际范围。
     */
    private static boolean isPlaceholderUnboundedDoubleBounds(Number min, Number max) {
        if (min == null || max == null) {
            return false;
        }
        return min.doubleValue() == Double.MIN_VALUE && max.doubleValue() == Double.MAX_VALUE;
    }

    // endregion 从 EditableConfigHolder 动态生成 Cloth 条目
}
