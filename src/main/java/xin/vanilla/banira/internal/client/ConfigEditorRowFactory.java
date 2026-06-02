package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigEntryTooltipTexts;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 根据 Banira 配置描述符创建编辑行，避免 Screen 直接绑定具体控件细节。
 */
public final class ConfigEditorRowFactory {
    private static final double LABEL_COLUMN_WIDTH_RATIO = 0.32;
    private static final double LABEL_COLUMN_MIN_WIDTH = 64;
    private static final int GAP_LABEL_TO_VALUE = 4;
    private static final double VALUE_AREA_MIN_WIDTH = 56;
    private static final int RESET_BTN_SIZE = 18;
    private static final int RESET_BTN_GAP = 2;

    private final BaniraScreen screen;
    private final ConfigHolder holder;
    private final ConfigEditorState editorState;
    private final Runnable contentHeightChanged;

    public ConfigEditorRowFactory(BaniraScreen screen, ConfigHolder holder, ConfigEditorState editorState,
                                  Runnable contentHeightChanged) {
        this.screen = screen;
        this.holder = holder;
        this.editorState = editorState;
        this.contentHeightChanged = contentHeightChanged;
    }

    /**
     * 创建一个配置项编辑行；无法识别的值类型会返回 null。
     */
    public ConfigEditorEntryWidget createEntryRow(ConfigEntryDescriptor desc, double w, int rowH) {
        switch (desc.getValueType()) {
            case STRING:
                return createStringRow(desc, w, rowH);
            case BOOLEAN:
                return createBooleanRow(desc, w, rowH);
            case INTEGER:
            case LONG:
            case DOUBLE:
                return createNumberRow(desc, w, rowH);
            case ENUM:
                return createEnumRow(desc, w, rowH);
            case STRING_LIST:
            case INTEGER_LIST:
            case LONG_LIST:
            case DOUBLE_LIST:
            case BOOLEAN_LIST:
            case ENUM_LIST:
                return createListRow(desc, w, rowH);
            default:
                return null;
        }
    }

    private ConfigEditorEntryWidget createStringRow(ConfigEntryDescriptor desc, double w, int rowH) {
        ConfigEditorEntryRowWidget row = createRow(w, rowH);
        LabelWidget label = createLabel(desc, w, rowH);

        InputWidget input = new InputWidget(screen);
        input.id("cfg_" + idSuffix(desc));
        input.bounds(new ScreenCoordinate(valueStartX(w), 0, valueWidgetWidth(w), rowH));
        Object raw = holder.get(desc.getPath());
        input.value(raw instanceof String ? (String) raw : raw != null ? raw.toString() : "");
        input.maxLength(256);
        input.onTextChanged(v -> editorState.markModified(desc.getPath(), v));

        row.addChild(label);
        row.addChild(input);
        addResetButton(row, desc, w, rowH, v -> input.value(String.valueOf(v)));
        addTooltip(row, desc, 0, 0, w, rowH);
        return new ConfigEditorEntryWidgetAdapter(row, input::value, v -> input.value(String.valueOf(v)));
    }

    private ConfigEditorEntryWidget createBooleanRow(ConfigEntryDescriptor desc, double w, int rowH) {
        ConfigEditorEntryRowWidget row = createRow(w, rowH);
        LabelWidget label = createLabel(desc, w, rowH);

        boolean val = Boolean.TRUE.equals(holder.get(desc.getPath()));
        final boolean[] currentValue = {val};
        ButtonWidget btn = new ButtonWidget(screen);
        btn.id("cfg_" + idSuffix(desc));
        btn.bounds(new ScreenCoordinate(valueStartX(w), 0, valueWidgetWidth(w), rowH));
        btn.text(booleanText(val));
        btn.onClick(b -> {
            boolean newVal = !currentValue[0];
            currentValue[0] = newVal;
            editorState.markModified(desc.getPath(), newVal);
            btn.text(booleanText(newVal));
        });

        row.addChild(label);
        row.addChild(btn);
        addResetButton(row, desc, w, rowH, v -> {
            currentValue[0] = Boolean.TRUE.equals(v);
            btn.text(booleanText(currentValue[0]));
        });
        addTooltip(row, desc, 0, 0, w, rowH);
        return new ConfigEditorEntryWidgetAdapter(row, () -> currentValue[0], v -> {
            currentValue[0] = Boolean.TRUE.equals(v);
            btn.text(booleanText(currentValue[0]));
        });
    }

    private ConfigEditorEntryWidget createNumberRow(ConfigEntryDescriptor desc, double w, int rowH) {
        ConfigEditorEntryRowWidget row = createRow(w, rowH);
        LabelWidget label = createLabel(desc, w, rowH);

        double min = desc.getMinValue() != null ? desc.getMinValue().doubleValue() : 0;
        double max = desc.getMaxValue() != null ? desc.getMaxValue().doubleValue() : 100;
        double step = desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE
                ? Math.max(1.0 / Math.pow(10, desc.getDecimalPlaces()), (max - min) / 100)
                : 1.0;
        double initVal = initialNumberValue(desc, min, max);

        SliderWidget slider = new SliderWidget(screen);
        slider.id("cfg_" + idSuffix(desc));
        slider.bounds(new ScreenCoordinate(valueStartX(w), 0, valueWidgetWidth(w), rowH));
        slider.minValue(min).maxValue(max).step(step).value(initVal);
        slider.decimalPlaces(desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE
                ? desc.getDecimalPlaces() : 0);
        slider.onValueChanged(v -> {
            Object parsed = convertSliderValue(desc, v);
            if (parsed != null && !Objects.equals(parsed, holder.get(desc.getPath()))) {
                editorState.markModified(desc.getPath(), parsed);
            }
        });

        row.addChild(label);
        row.addChild(slider);
        Consumer<Object> setter = v -> {
            double d = v instanceof Number ? ((Number) v).doubleValue() : 0;
            slider.setValue(Math.max(slider.minValue(), Math.min(slider.maxValue(), d)));
        };
        addResetButton(row, desc, w, rowH, setter);
        addTooltip(row, desc, 0, 0, w, rowH);
        return new ConfigEditorEntryWidgetAdapter(row, () -> convertSliderValue(desc, slider.value()), setter, () -> true);
    }

    private ConfigEditorEntryWidget createEnumRow(ConfigEntryDescriptor desc, double w, int rowH) {
        ConfigEditorEntryRowWidget row = createRow(w, rowH);
        LabelWidget label = createLabel(desc, w, rowH);

        Object current = holder.get(desc.getPath());
        Class<? extends Enum<?>> enumClass = desc.getEnumClass();
        List<String> options = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList());

        DropdownSelectWidget dropdown = new DropdownSelectWidget(screen);
        dropdown.id("cfg_" + idSuffix(desc));
        dropdown.bounds(new ScreenCoordinate(valueStartX(w), 0, valueWidgetWidth(w), rowH));
        dropdown.optionsEnum(enumClass);
        dropdown.selectedValues(Collections.singletonList(current != null ? current.toString() : options.get(0)));
        dropdown.onSelectionChanged(v -> {
            if (!v.isEmpty()) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), v.get(0));
                    editorState.markModified(desc.getPath(), e);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });

        row.addChild(label);
        row.addChild(dropdown);
        Consumer<Object> setter = v -> dropdown.selectedValues(Collections.singletonList(v != null ? v.toString() : options.get(0)));
        addResetButton(row, desc, w, rowH, setter);
        addTooltip(row, desc, 0, 0, w, rowH);
        return new ConfigEditorEntryWidgetAdapter(row, () -> selectedEnumValue(desc, dropdown), setter);
    }

    private ConfigEditorEntryWidget createListRow(ConfigEntryDescriptor desc, double w, int rowH) {
        ConfigEditorEntryRowWidget row = new ConfigEditorEntryRowWidget(screen);
        LabelWidget label = createLabel(desc, w, rowH);

        Object raw = holder.get(desc.getPath());
        List<?> list = raw instanceof List ? (List<?>) raw : null;
        List<Object> items = ConfigListSpecHelper.normalizeListForGui(list, desc);

        TagListEditorWidget tagList = new TagListEditorWidget(screen);
        tagList.id("cfg_" + idSuffix(desc));
        tagList.bounds(new ScreenCoordinate(valueStartX(w), 0, valueWidgetWidth(w), TagListEditorWidget.DEFAULT_EXPANDED_HEIGHT));
        tagList.itemType(tagListItemType(desc));
        applyListTagNumericOptions(tagList, desc);
        if (desc.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST && desc.getEnumClass() != null) {
            tagList.enumOptionsList(Arrays.stream(desc.getEnumClass().getEnumConstants()).map(Enum::name).collect(Collectors.toList()));
        }
        tagList.items(items);
        tagList.expanded(false);
        tagList.refreshBounds();
        row.bounds(new ScreenCoordinate(0, 0, w, tagList.effectiveHeight()));
        // 列表展开/收起会改变整棵配置树高度，需要向外通知视口重新布局。
        tagList.onBoundsHeightChanged(t -> {
            IWidget rowWidget = t.parent();
            if (rowWidget instanceof BaseWidget) {
                double newH = t.effectiveHeight();
                ScreenCoordinate b = rowWidget.bounds();
                if (b != null) {
                    ((BaseWidget) rowWidget).bounds(new ScreenCoordinate(b.x(), b.y(), b.width(), newH));
                }
                IWidget panel = rowWidget.parent();
                if (panel instanceof CollapsiblePanelWidget) {
                    ((CollapsiblePanelWidget) panel).refreshLayoutFromChild(rowWidget);
                }
            }
            contentHeightChanged.run();
        });
        tagList.onListChanged(v -> editorState.markModified(desc.getPath(), ConfigListSpecHelper.listFromGuiItems(v, desc)));

        int tagRowH = (int) tagList.effectiveHeight();
        row.addChild(label);
        row.addChild(tagList);
        Consumer<Object> setter = v -> {
            if (v instanceof List) {
                tagList.items(ConfigListSpecHelper.normalizeListForGui((List<?>) v, desc));
            }
        };
        addResetButton(row, desc, w, tagRowH, setter);
        addTooltip(row, desc, 0, 0, w, rowH);
        return new ConfigEditorEntryWidgetAdapter(row,
                () -> ConfigListSpecHelper.listFromGuiItems(new ArrayList<>(tagList.items()), desc), setter);
    }

    private ConfigEditorEntryRowWidget createRow(double w, int rowH) {
        ConfigEditorEntryRowWidget row = new ConfigEditorEntryRowWidget(screen);
        row.bounds(new ScreenCoordinate(0, 0, w, rowH));
        return row;
    }

    private LabelWidget createLabel(ConfigEntryDescriptor desc, double w, int rowH) {
        LabelWidget label = new LabelWidget(screen);
        label.id("lbl_" + idSuffix(desc));
        label.bounds(new ScreenCoordinate(0, 0, labelTextWidth(w), rowH));
        label.text(desc.getDisplayName());
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);
        return label;
    }

    private void addResetButton(ConfigEditorEntryRowWidget row, ConfigEntryDescriptor desc, double rowW, int rowH,
                                Consumer<Object> setValue) {
        int btnY = (rowH - RESET_BTN_SIZE) / 2;
        ButtonWidget btn = new ButtonWidget(screen);
        btn.id("reset_" + idSuffix(desc));
        btn.presetStyle(ButtonWidget.PresetStyle.RESET);
        btn.bounds(new ScreenCoordinate(resetBtnX(rowW), btnY, RESET_BTN_SIZE, RESET_BTN_SIZE));
        btn.onClick(b -> {
            Object def = desc.getDefaultValue();
            if (def != null) {
                holder.set(desc.getPath(), def);
                editorState.markModified(desc.getPath(), def);
                setValue.accept(def);
            }
        });
        TooltipWidget resetTip = new TooltipWidget(screen, new ScreenCoordinate(resetBtnX(rowW), btnY, RESET_BTN_SIZE, RESET_BTN_SIZE));
        resetTip.id("reset_tip_" + idSuffix(desc));
        resetTip.text(BaniraComponent.get().transClientAuto("config_editor_reset_tooltip"));
        resetTip.popupAtScreenCoords(true);
        row.addChild(btn);
        row.addChild(resetTip);
    }

    private void addTooltip(ConfigEditorEntryRowWidget row, ConfigEntryDescriptor desc, double x, double y, double w, int rowH) {
        if (!ConfigEntryTooltipTexts.hasGuiTooltip(desc)) {
            return;
        }
        TooltipWidget tooltip = new TooltipWidget(screen, new ScreenCoordinate(x, y, labelTextWidth(w), rowH));
        tooltip.id("tip_" + idSuffix(desc));
        tooltip.text(ConfigEntryTooltipTexts.guiTooltipComponent(desc, configModId()));
        tooltip.popupAtScreenCoords(true);
        row.addChild(tooltip);
    }

    private double initialNumberValue(ConfigEntryDescriptor desc, double min, double max) {
        Object raw = holder.get(desc.getPath());
        double initVal = 0;
        if (raw instanceof Number) {
            initVal = ((Number) raw).doubleValue();
        } else if (raw != null) {
            try {
                initVal = Double.parseDouble(raw.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return Math.max(min, Math.min(max, initVal));
    }

    private Object selectedEnumValue(ConfigEntryDescriptor desc, DropdownSelectWidget dropdown) {
        List<String> sel = dropdown.getSelectedValues();
        if (sel.isEmpty()) {
            return holder.get(desc.getPath());
        }
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> e = Enum.valueOf((Class) desc.getEnumClass(), sel.get(0));
            return e;
        } catch (Exception ex) {
            return holder.get(desc.getPath());
        }
    }

    private static Object convertSliderValue(ConfigEntryDescriptor desc, double v) {
        switch (desc.getValueType()) {
            case INTEGER:
                return (int) Math.round(v);
            case LONG:
                return (long) Math.round(v);
            case DOUBLE: {
                int dp = desc.getDecimalPlaces();
                double factor = Math.pow(10, dp);
                return Math.round(v * factor) / factor;
            }
            default:
                return v;
        }
    }

    private static TagListEditorWidget.ItemType tagListItemType(ConfigEntryDescriptor desc) {
        switch (desc.getValueType()) {
            case STRING_LIST:
                return TagListEditorWidget.ItemType.TEXT;
            case INTEGER_LIST:
            case LONG_LIST:
            case DOUBLE_LIST:
                return TagListEditorWidget.ItemType.NUMBER;
            case BOOLEAN_LIST:
                return TagListEditorWidget.ItemType.BOOLEAN;
            case ENUM_LIST:
                return TagListEditorWidget.ItemType.ENUM;
            default:
                return TagListEditorWidget.ItemType.TEXT;
        }
    }

    private static void applyListTagNumericOptions(TagListEditorWidget tagList, ConfigEntryDescriptor desc) {
        switch (desc.getValueType()) {
            case INTEGER_LIST:
            case LONG_LIST:
                tagList.listNumberIntegerOnly(true);
                tagList.listNumberDecimalPlaces(0);
                tagList.listNumberMin(desc.getMinValue() != null ? desc.getMinValue().doubleValue() : null);
                tagList.listNumberMax(desc.getMaxValue() != null ? desc.getMaxValue().doubleValue() : null);
                break;
            case DOUBLE_LIST:
                tagList.listNumberIntegerOnly(false);
                tagList.listNumberDecimalPlaces(desc.getDecimalPlaces());
                tagList.listNumberMin(desc.getMinValue() != null ? desc.getMinValue().doubleValue() : null);
                tagList.listNumberMax(desc.getMaxValue() != null ? desc.getMaxValue().doubleValue() : null);
                break;
            default:
                tagList.listNumberIntegerOnly(false);
                tagList.listNumberDecimalPlaces(2);
                tagList.listNumberMin(null);
                tagList.listNumberMax(null);
                break;
        }
    }

    public String configModId() {
        String id = holder.getModId();
        return id == null || id.isEmpty() ? BaniraCodex.MODID : id;
    }

    private double labelColumnEndX(double rowWidth) {
        if (rowWidth <= 1) {
            return 1;
        }
        double reservedRight = RESET_BTN_GAP + RESET_BTN_SIZE;
        double maxEnd = rowWidth - reservedRight - VALUE_AREA_MIN_WIDTH;
        if (maxEnd < 1) {
            return Math.max(1, rowWidth * 0.2);
        }
        double fromRatio = rowWidth * LABEL_COLUMN_WIDTH_RATIO;
        double inner = Math.min(fromRatio, maxEnd);
        double end = Math.max(LABEL_COLUMN_MIN_WIDTH, inner);
        return end > maxEnd ? Math.max(1, maxEnd) : end;
    }

    private double labelTextWidth(double rowWidth) {
        return Math.max(1, labelColumnEndX(rowWidth) - GAP_LABEL_TO_VALUE);
    }

    private double valueStartX(double rowWidth) {
        return labelColumnEndX(rowWidth);
    }

    private double valueWidgetWidth(double rowWidth) {
        double vw = rowWidth - labelColumnEndX(rowWidth) - RESET_BTN_GAP - RESET_BTN_SIZE;
        return Math.max(1, vw);
    }

    private double resetBtnX(double rowWidth) {
        return rowWidth - RESET_BTN_SIZE;
    }

    private static String idSuffix(ConfigEntryDescriptor desc) {
        return desc.getPath().replace(".", "_");
    }

    private static String booleanText(boolean enabled) {
        return enabled ? "§aON" : "§cOFF";
    }
}
