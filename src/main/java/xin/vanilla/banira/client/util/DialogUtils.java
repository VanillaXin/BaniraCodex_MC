package xin.vanilla.banira.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.common.util.StringUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryStack.stackGet;

/**
 * 对话框工具类
 */
@Environment(EnvType.CLIENT)
public final class DialogUtils {
    private DialogUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ExecutorService DIALOG_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "BaniraCodex-DialogThread");
        thread.setDaemon(true);
        return thread;
    });


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
        ok_cancel,
        yes_no,
        yes_no_cancel,
    }

    /**
     * 选择文件
     * 返回字符串路径, 取消返回null
     *
     * @param desc       对话框描述
     * @param callback   回调函数
     * @param extensions 文件扩展名过滤, 如 "*.png", "*.jpg"
     */
    public static void chooseFileString(String desc, Consumer<String> callback, String... extensions) {
        DIALOG_EXECUTOR.execute(() -> {
            try {
                String result = chooseFileStringSyncInternal(desc, extensions);
                BaniraScheduler.schedule(0, () -> callback.accept(result));
            } catch (Exception e) {
                LOGGER.error("Error in chooseFileString", e);
                BaniraScheduler.schedule(0, () -> callback.accept(null));
            }
        });
    }

    /**
     * 选择文件
     * 返回File对象, 取消返回null
     *
     * @param desc       对话框描述
     * @param callback   回调函数
     * @param extensions 文件扩展名过滤
     */
    public static void chooseFile(String desc, Consumer<File> callback, String... extensions) {
        chooseFileString(desc, result -> {
            callback.accept(result == null ? null : new File(result));
        }, extensions);
    }

    /**
     * 选择RGB颜色
     * 返回十六进制字符串, 取消返回null
     *
     * @param title    对话框标题
     * @param callback 回调函数，参数为颜色的十六进制字符串（如 "#FFFFFF"）
     */
    public static void chooseRgbHex(String title, Consumer<String> callback) {
        DIALOG_EXECUTOR.execute(() -> {
            try {
                String result = chooseRgbHexSyncInternal(title);
                // 在主线程中执行回调
                BaniraScheduler.schedule(0, () -> callback.accept(result));
            } catch (Exception e) {
                LOGGER.error("Error in chooseRgbHex", e);
                BaniraScheduler.schedule(0, () -> callback.accept(""));
            }
        });
    }

    /**
     * 弹出通知
     *
     * @param title    标题
     * @param msg      消息内容
     * @param iconType 图标类型
     * @param callback 回调函数, 参数为返回值, 0表示成功
     */
    public static void popupNotify(String title, String msg, DialogIconType iconType, Consumer<Integer> callback) {
        DIALOG_EXECUTOR.execute(() -> {
            try {
                int result = popupNotifySyncInternal(title, msg, iconType);
                // 在主线程中执行回调
                BaniraScheduler.schedule(0, () -> callback.accept(result));
            } catch (Exception e) {
                LOGGER.error("Error in popupNotify", e);
                BaniraScheduler.schedule(0, () -> callback.accept(-1));
            }
        });
    }

    /**
     * 弹出消息框
     *
     * @param title      标题
     * @param msg        消息内容
     * @param iconType   图标类型
     * @param buttonType 按钮类型
     * @param callback   回调函数, 参数为用户的选择结果: 0表示cancel/no, 1表示ok/yes, 2表示no(在yes_no_cancel类型中)
     */
    public static void openMessageBox(String title, String msg, DialogIconType iconType, DialogButtonType buttonType, Consumer<Integer> callback) {
        DIALOG_EXECUTOR.execute(() -> {
            try {
                int result = openMessageBoxSyncInternal(title, msg, iconType, buttonType);
                // 在主线程中执行回调
                BaniraScheduler.schedule(0, () -> callback.accept(result));
            } catch (Exception e) {
                LOGGER.error("Error in openMessageBox", e);
                BaniraScheduler.schedule(0, () -> callback.accept(0));
            }
        });
    }

    /**
     * 选择文件
     * </br>
     * 此方法会阻塞游戏主线程, 直到对话框关闭
     * </br>
     * 推荐使用 {@link #chooseFileString(String, Consumer, String...)} 异步版本
     *
     * @param desc       对话框描述
     * @param extensions 文件扩展名过滤（如 "*.png", "*.jpg"）
     * @return 选择的文件路径, 取消返回null
     */
    public static String chooseFileStringSync(String desc, String... extensions) {
        return chooseFileStringSyncInternal(desc, extensions);
    }

    /**
     * 选择文件
     * </br>
     * 此方法会阻塞游戏主线程, 直到对话框关闭
     * </br>
     * 推荐使用 {@link #chooseFile(String, Consumer, String...)} 异步版本
     *
     * @param desc       对话框描述
     * @param extensions 文件扩展名过滤
     * @return 选择的文件, 取消返回null
     */
    public static File chooseFileSync(String desc, String... extensions) {
        String result = chooseFileStringSyncInternal(desc, extensions);
        return result == null ? null : new File(result);
    }

    /**
     * 选择RGB颜色
     * </br>
     * 此方法会阻塞游戏主线程, 直到对话框关闭
     * </br>
     * 推荐使用 {@link #chooseRgbHex(String, Consumer)} 异步版本
     *
     * @param title 对话框标题
     * @return 颜色的十六进制字符串（如 "#FFFFFF"），如果取消则返回null
     */
    public static String chooseRgbHexSync(String title) {
        return chooseRgbHexSyncInternal(title);
    }

    /**
     * 弹出通知
     * </br>
     * 此方法会阻塞游戏主线程, 直到对话框关闭
     * </br>
     * 推荐使用 {@link #popupNotify(String, String, DialogIconType, Consumer)} 异步版本
     *
     * @param title    标题
     * @param msg      消息内容
     * @param iconType 图标类型
     * @return 返回值（通常为0表示成功）
     */
    public static int popupNotifySync(String title, String msg, DialogIconType iconType) {
        return popupNotifySyncInternal(title, msg, iconType);
    }

    /**
     * 弹出消息框
     * </br>
     * 此方法会阻塞游戏主线程, 直到对话框关闭
     * </br>
     * 推荐使用 {@link #openMessageBox(String, String, DialogIconType, DialogButtonType, Consumer)} 异步版本
     *
     * @param title      标题
     * @param msg        消息内容
     * @param iconType   图标类型
     * @param buttonType 按钮类型
     * @return 用户的选择结果: 0表示cancel/no, 1表示ok/yes, 2表示no(在yes_no_cancel类型中)
     */
    public static int openMessageBoxSync(String title, String msg, DialogIconType iconType, DialogButtonType buttonType) {
        return openMessageBoxSyncInternal(title, msg, iconType, buttonType);
    }


    /**
     * 选择文件
     */
    private static String chooseFileStringSyncInternal(String desc, String... extensions) {
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
     * 选择RGB颜色
     */
    private static String chooseRgbHexSyncInternal(String title) {
        if (StringUtils.isNullOrEmptyEx(title)) title = "Choose a color";
        return TinyFileDialogs.tinyfd_colorChooser(title, "#FFFFFF", null, createByteBuffer(3));
    }

    /**
     * 弹出通知
     */
    private static int popupNotifySyncInternal(String title, String msg, DialogIconType iconType) {
        return TinyFileDialogs.tinyfd_notifyPopup(title, msg, iconType.name());
    }

    /**
     * 弹出消息框
     *
     * @return 0表示cancel/no, 1表示ok/yes, 2表示no(在yes_no_cancel类型中)
     */
    private static int openMessageBoxSyncInternal(String title, String msg, DialogIconType iconType, DialogButtonType buttonType) {
        String dialogType = buttonType.name().replace("_", "");

        try (MemoryStack stack = stackGet()) {
            int stackPointer = stack.getPointer();
            try {
                stack.nUTF8Safe(title, true);
                long titleEncoded = title == null ? 0L : stack.getPointerAddress();
                stack.nUTF8Safe(msg, true);
                long msgEncoded = msg == null ? 0L : stack.getPointerAddress();
                stack.nASCII(dialogType, true);
                long dialogTypeEncoded = stack.getPointerAddress();
                stack.nASCII(iconType.name(), true);
                long iconTypeEncoded = stack.getPointerAddress();

                return TinyFileDialogs.ntinyfd_messageBox(titleEncoded, msgEncoded, dialogTypeEncoded, iconTypeEncoded, 0);
            } finally {
                stack.setPointer(stackPointer);
            }
        }
    }
}
