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

    public static CompoundNBT readCompressed(InputStream stream) {
        try {
            return CompressedStreamTools.readCompressed(stream);
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed stream", e);
            return new CompoundNBT();
        }
    }

    public static CompoundNBT readCompressed(File file) {
        try {
            return CompressedStreamTools.readCompressed(file);
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed file: {}", file.getAbsolutePath(), e);
            return new CompoundNBT();
        }
    }

    public static boolean writeCompressed(CompoundNBT tag, File file) {
        boolean result = false;
        try {
            CompressedStreamTools.writeCompressed(tag, file);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed file: {}", file.getAbsolutePath(), e);
        }
        return result;
    }

    public static boolean writeCompressed(CompoundNBT tag, OutputStream stream) {
        boolean result = false;
        try {
            CompressedStreamTools.writeCompressed(tag, stream);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed stream", e);
        }
        return result;
    }

    // endregion compressed

    // region path

    public static INBT getTagByPath(INBT root, String path) {
        String[] parts = path.split("\\.");
        INBT current = root;

        for (String part : parts) {
            if (current == null) return null;
            Matcher matcher = PATH_PATTERN.matcher(part);
            if (!matcher.matches()) return null;

            String key = matcher.group(1);
            String indexStr = matcher.group(3);

            if (current instanceof CompoundNBT) {
                CompoundNBT compound = (CompoundNBT) current;
                if (!compound.contains(key)) return null;
                current = compound.get(key);
            } else {
                return null;
            }

            if (indexStr != null && current instanceof CollectionNBT) {
                int index = Integer.parseInt(indexStr);
                CollectionNBT<?> list = (CollectionNBT<?>) current;
                if (index < 0 || index >= list.size()) {
                    return null;
                }
                current = list.get(index);
            }
        }

        return current;
    }

    public static String getString(INBT root, String path, String defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof StringNBT) ? tag.getAsString() : defaultVal;
    }

    public static boolean getBoolean(INBT root, String path, boolean defaultVal) {
        INBT tag = getTagByPath(root, path);
        if (tag instanceof ByteNBT) {
            return ((ByteNBT) tag).getAsByte() != 0;
        } else if (tag instanceof IntNBT) {
            return ((IntNBT) tag).getAsInt() != 0;
        }
        return defaultVal;
    }

    public static int getByte(INBT root, String path, int defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsByte() : defaultVal;
    }

    public static int getShort(INBT root, String path, int defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsShort() : defaultVal;
    }

    public static int getInt(INBT root, String path, int defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsInt() : defaultVal;
    }

    public static float getFloat(INBT root, String path, float defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsFloat() : defaultVal;
    }

    public static long getLong(INBT root, String path, long defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsLong() : defaultVal;
    }

    public static double getDouble(INBT root, String path, double defaultVal) {
        INBT tag = getTagByPath(root, path);
        return (tag instanceof NumberNBT) ? ((NumberNBT) tag).getAsDouble() : defaultVal;
    }

    public static boolean has(INBT root, String path) {
        return getTagByPath(root, path) != null;
    }

    // endregion path

}
