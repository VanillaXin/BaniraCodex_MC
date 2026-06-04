package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 字符输入事件。与按键事件分开，专用于文本输入内容。
 */
@Data
@Accessors(chain = true, fluent = true)
public class CharInputEvent {
    private char codePoint;
    private int modifiers;

    public static CharInputEvent of(char codePoint, int modifiers) {
        return new CharInputEvent().codePoint(codePoint).modifiers(modifiers);
    }

    public String text() {
        return String.valueOf(codePoint);
    }
}
