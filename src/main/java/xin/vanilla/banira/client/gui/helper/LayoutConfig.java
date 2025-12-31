package xin.vanilla.banira.client.gui.helper;

/**
 * 屏幕布局配置常量
 */
public final class LayoutConfig {
    private LayoutConfig() {
    }

    /**
     * 物品选择界面配置
     */
    public static final class ItemSelect {
        public static final int ITEMS_PER_LINE = 9;
        public static final int MAX_LINES = 5;
        public static final double MARGIN = 3.0;
        public static final int PANEL_WIDTH = 180;
        public static final int PANEL_HEIGHT_OFFSET = 20;
        public static final int BUTTON_WIDTH = 90;
        public static final int BUTTON_HEIGHT = 20;
        public static final int INPUT_FIELD_HEIGHT = 12;
        public static final int BG_X_OFFSET = 92;
        public static final int BG_Y_OFFSET = 65;
    }

    /**
     * 进度选择界面配置
     */
    public static final class AdvancementSelect {
        public static final int MAX_LINES = 5;
        public static final double MARGIN = 3.0;
        public static final int PANEL_WIDTH = 112;
        public static final int PANEL_HEIGHT_OFFSET = 20;
        public static final int BUTTON_WIDTH = 56;
        public static final int BUTTON_HEIGHT = 20;
        public static final int INPUT_FIELD_HEIGHT = 15;
        public static final int BG_X_OFFSET = 56;
        public static final int BG_Y_OFFSET = 63;
        public static final int LIST_WIDTH = 104;
    }

    /**
     * 通用按钮配置
     */
    public static final class Button {
        public static final int OPERATION_BUTTON_SIZE_OFFSET = 4;
        public static final int OPERATION_BUTTON_SPACING = 1;
        public static final int OPERATION_BUTTON_X_OFFSET = 3;
    }

    /**
     * 字符串输入界面配置
     */
    public static final class StringInput {
        public static final int INPUT_FIELD_HEIGHT = 12;
        public static final int INPUT_FIELD_SPACING = 25;
        public static final int TITLE_HEIGHT = 12;
        public static final int BUTTON_HEIGHT = 20;
        public static final int BUTTON_WIDTH = 95;
        public static final int BUTTON_MARGIN = 5;
        public static final int SCROLL_BAR_WIDTH = 5;
        public static final int SCROLL_BAR_MARGIN = 2;
        public static final int SCROLL_SPEED = 10;
    }

    /**
     * 颜色常量
     */
    public static final class Colors {
        public static final int BACKGROUND = 0xCCC6C6C6;
        public static final int BORDER = 0xFF000000;
        public static final int BUTTON_BACKGROUND = 0xEE707070;
        public static final int BUTTON_BACKGROUND_HOVER = 0xEEFFFFFF;
        public static final int BUTTON_BORDER = 0xEE000000;
        public static final int BUTTON_BORDER_HOVER = 0xEEFFFFFF;
        public static final int SELECTED_BACKGROUND = 0xEE7CAB7C;
        public static final int LOADING_TEXT = 0xFFFFFFFF;
    }
}
