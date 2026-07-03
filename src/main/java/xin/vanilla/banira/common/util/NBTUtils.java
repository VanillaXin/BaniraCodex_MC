package xin.vanilla.banira.common.util;

import net.minecraft.nbt.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NBTUtils {
    private NBTUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern PATH_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)(\\[(\\d+)])?");


    // region compressed

    public static CompoundTag readCompressed(InputStream stream) {
        try {
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed stream", e);
            return new CompoundTag();
        }
    }

    public static CompoundTag readCompressed(File file) {
        try {
            return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed file: {}", file.getAbsolutePath(), e);
            return new CompoundTag();
        }
    }

    public static boolean writeCompressed(CompoundTag tag, File file) {
        boolean result = false;
        try {
            NbtIo.writeCompressed(tag, file.toPath());
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed file: {}", file.getAbsolutePath(), e);
        }
        return result;
    }

    public static boolean writeCompressed(CompoundTag tag, OutputStream stream) {
        boolean result = false;
        try {
            NbtIo.writeCompressed(tag, stream);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed stream", e);
        }
        return result;
    }

    // endregion compressed

    // region path

    public static Tag getTagByPath(Tag root, String path) {
        String[] parts = path.split("\\.");
        Tag current = root;

        for (String part : parts) {
            if (current == null) return null;
            Matcher matcher = PATH_PATTERN.matcher(part);
            if (!matcher.matches()) return null;

            String key = matcher.group(1);
            String indexStr = matcher.group(3);

            if (current instanceof CompoundTag compound) {
                if (!compound.contains(key)) return null;
                current = compound.get(key);
            } else {
                return null;
            }

            if (indexStr != null && current instanceof CollectionTag<?> list) {
                int index = Integer.parseInt(indexStr);
                if (index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
            }
        }

        return current;
    }

    public static String getString(Tag root, String path, String defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof StringTag) ? tag.getAsString() : defaultVal;
    }

    public static boolean getBoolean(Tag root, String path, boolean defaultVal) {
        Tag tag = getTagByPath(root, path);
        if (tag instanceof ByteTag byteTag) {
            return byteTag.getAsByte() != 0;
        } else if (tag instanceof IntTag intTag) {
            return intTag.getAsInt() != 0;
        }
        return defaultVal;
    }

    public static int getByte(Tag root, String path, int defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsByte() : defaultVal;
    }

    public static int getShort(Tag root, String path, int defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsShort() : defaultVal;
    }

    public static int getInt(Tag root, String path, int defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsInt() : defaultVal;
    }

    public static float getFloat(Tag root, String path, float defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsFloat() : defaultVal;
    }

    public static long getLong(Tag root, String path, long defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsLong() : defaultVal;
    }

    public static double getDouble(Tag root, String path, double defaultVal) {
        Tag tag = getTagByPath(root, path);
        return (tag instanceof NumericTag numericTag) ? numericTag.getAsDouble() : defaultVal;
    }

    public static boolean has(Tag root, String path) {
        return getTagByPath(root, path) != null;
    }

    // endregion path

}
