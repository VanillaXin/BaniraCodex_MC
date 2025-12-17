package xin.vanilla.banira.client.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import xin.vanilla.banira.common.util.StringUtils;

import java.io.File;

import static org.lwjgl.BufferUtils.createByteBuffer;

/**
 * 对话框工具类
 */
@OnlyIn(Dist.CLIENT)
public final class DialogUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private DialogUtils() {
    }

    /**
     * 对话框图标类型
     */
    public enum DialogIconType {
        info,
        warning,
        error,
        question,
    }

    /**
     * 对话框按钮类型
     */
    public enum DialogButtonType {
        ok,
        okcancel,
        yesno,
        yesnocancel,
    }

    /**
     * 选择文件（返回字符串路径）
     *
     * @param desc       对话框描述
     * @param extensions 文件扩展名过滤（如 "*.png", "*.jpg"）
     * @return 选择的文件路径，如果取消则返回null
     */
    public static String chooseFileString(String desc, String... extensions) {
        if (StringUtils.isNullOrEmptyEx(desc)) desc = "Choose a file";

        String result;
        String defaultPath = SystemUtils.getUserHome().toAbsolutePath().toString();

        if (extensions.length > 0) {
            // 构建过滤器字符串，格式： "Description\next1\next2"
            String filterPattern = desc + "\n" + String.join("\n", extensions);
            result = TinyFileDialogs.tinyfd_openFileDialog(desc, defaultPath, null, filterPattern, false);
        } else {
            result = TinyFileDialogs.tinyfd_openFileDialog(desc, defaultPath, null, null, false);
        }
        return result;
    }

    /**
     * 选择文件（返回File对象）
     *
     * @param desc       对话框描述
     * @param extensions 文件扩展名过滤
     * @return 选择的文件，如果取消则返回null
     */
    public static File chooseFile(String desc, String... extensions) {
        String result = chooseFileString(desc, extensions);
        return result == null ? null : new File(result);
    }

    /**
     * 选择RGB颜色（返回十六进制字符串）
     *
     * @param title 对话框标题
     * @return 颜色的十六进制字符串（如 "#FFFFFF"），如果取消则返回null
     */
    public static String chooseRgbHex(String title) {
        if (StringUtils.isNullOrEmptyEx(title)) title = "Choose a color";
        return TinyFileDialogs.tinyfd_colorChooser(title, "#FFFFFF", null, createByteBuffer(3));
    }

    /**
     * 弹出通知
     *
     * @param title    标题
     * @param msg      消息内容
     * @param iconType 图标类型
     * @return 返回值（通常为0表示成功）
     */
    public static int popupNotify(String title, String msg, DialogIconType iconType) {
        return TinyFileDialogs.tinyfd_notifyPopup(title, msg, iconType.name());
    }

    /**
     * 弹出消息框
     *
     * @param title      标题
     * @param msg        消息内容
     * @param iconType   图标类型
     * @param buttonType 按钮类型
     * @return 用户的选择结果（true表示确认/是，false表示取消/否）
     */
    public static boolean openMessageBox(String title, String msg, DialogIconType iconType, DialogButtonType buttonType) {
        return TinyFileDialogs.tinyfd_messageBox(title, msg, buttonType.name(), iconType.name(), false);
    }
}
