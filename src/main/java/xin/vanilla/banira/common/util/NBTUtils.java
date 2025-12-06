package xin.vanilla.banira.common.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public final class NBTUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private NBTUtils() {
    }

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

}
