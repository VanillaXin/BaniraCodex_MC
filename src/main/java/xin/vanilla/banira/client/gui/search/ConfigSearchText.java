package xin.vanilla.banira.client.gui.search;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Component;

/**
 * 使用当前主题语义色生成配置搜索结果富文本。
 */
public final class ConfigSearchText {

    private ConfigSearchText() {
    }

    public static Text highlight(String value, ConfigSearchQuery query, int normalColor, int matchColor) {
        String display = value == null ? "" : value;
        int index = query.indexIn(display);
        if (query.isEmpty() || index < 0) {
            return Text.literal(display).color(normalColor);
        }

        Component root = BaniraComponent.get().literal("");
        int cursor = 0;
        while (index >= 0) {
            if (index > cursor) {
                root.append(normalSegment(display.substring(cursor, index), normalColor));
            }
            int matchEnd = Math.min(display.length(), index + query.length());
            root.append(BaniraComponent.get().literal(display.substring(index, matchEnd))
                    .color(matchColor).bold(false).underlined(true));
            cursor = matchEnd;
            index = query.indexIn(display, cursor);
        }
        if (cursor < display.length()) {
            root.append(normalSegment(display.substring(cursor), normalColor));
        }
        return Text.from(root);
    }

    private static Component normalSegment(String value, int normalColor) {
        return BaniraComponent.get().literal(value)
                .color(normalColor).bold(false).underlined(false);
    }
}
