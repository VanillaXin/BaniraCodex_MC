package xin.vanilla.banira.client.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@OnlyIn(Dist.CLIENT)
public final class PNGUtils {
    private PNGUtils() {
    }

    // PNG 文件签名
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    // PNG 头部大小
    private static final int PNG_HEADER_SIZE = 8;
    // Chunk 类型大小
    private static final int CHUNK_TYPE_SIZE = 4;
    // CRC 大小
    private static final int CRC_SIZE = 4;
    // Chunk 类型常量
    private static final String CHUNK_TYPE_ZTXT = "zTXt";
    private static final String CHUNK_TYPE_IEND = "IEND";
    // 压缩方法常量
    private static final int COMPRESSION_METHOD_DEFLATE = 0;
    // 解压缩缓冲区大小
    private static final int INFLATE_BUFFER_SIZE = 1024;


    /**
     * 根据关键字读取PNG文件中的zTxt信息
     *
     * @param pngFile       PNG文件
     * @param targetKeyWord zTxt数据中目标关键字
     */
    public static String readZTxtByKey(File pngFile, String targetKeyWord) throws IOException {
        Map<String, String> ztxtMap = readAllZTxt(pngFile);
        return ztxtMap.getOrDefault(targetKeyWord, null);
    }

    /**
     * 读取PNG文件中的所有zTXt块
     *
     * @param pngFile PNG文件
     */
    public static Map<String, String> readAllZTxt(File pngFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(pngFile)) {
            return readAllZTxt(fis);
        }
    }

    /**
     * 根据关键字从输入流中读取PNG的zTXt信息
     *
     * @param inputStream   PNG输入流
     * @param targetKeyWord zTXt数据目标关键字
     */
    public static String readZTxtByKey(InputStream inputStream, String targetKeyWord) throws IOException {
        Map<String, String> ztxtMap = readAllZTxt(inputStream);
        return ztxtMap.getOrDefault(targetKeyWord, null);
    }

    /**
     * 读取输入流中的所有zTXt块
     *
     * @param inputStream PNG输入流
     */
    public static Map<String, String> readAllZTxt(InputStream inputStream) throws IOException {
        Map<String, String> ztxtMap = new LinkedHashMap<>();

        try (DataInputStream dis = new DataInputStream(inputStream)) {
            byte[] pngHeader = new byte[PNG_HEADER_SIZE];
            dis.readFully(pngHeader);

            if (!isPNGHeaderValid(pngHeader)) {
                throw new IOException("Invalid PNG stream.");
            }

            while (dis.available() > 0) {
                int length = dis.readInt();
                if (length < 0 || length > Integer.MAX_VALUE - CHUNK_TYPE_SIZE - CRC_SIZE) {
                    throw new IOException("Invalid chunk length: " + length);
                }

                byte[] chunkType = new byte[CHUNK_TYPE_SIZE];
                dis.readFully(chunkType);
                String chunkName = new String(chunkType, StandardCharsets.UTF_8);

                byte[] data = new byte[length];
                dis.readFully(data);
                dis.skipBytes(CRC_SIZE);

                if (CHUNK_TYPE_ZTXT.equals(chunkName)) {
                    processZTxtChunk(data, ztxtMap);
                }
            }
        }
        return ztxtMap;
    }

    /**
     * 处理 zTXt chunk 数据
     */
    private static void processZTxtChunk(byte[] data, Map<String, String> ztxtMap) throws IOException {
        String keyword = readNullTerminatedString(data);
        int index = keyword.length() + 1;

        if (index >= data.length) {
            throw new IOException("Invalid zTXt chunk: missing compression method");
        }

        int compressionMethod = data[index] & 0xFF;
        index += 1;

        if (compressionMethod != COMPRESSION_METHOD_DEFLATE) {
            throw new IOException("Unsupported compression method in zTXt block: " + compressionMethod);
        }

        if (index >= data.length) {
            throw new IOException("Invalid zTXt chunk: missing compressed data");
        }

        byte[] compressedText = new byte[data.length - index];
        System.arraycopy(data, index, compressedText, 0, compressedText.length);
        String decompressedText = inflateText(compressedText);
        ztxtMap.put(keyword, decompressedText);
    }


    /**
     * 根据关键字更新zTxt标签信息，并写入到新的文件
     *
     * @param pngFile    输入的PNG文件
     * @param outputFile 输出的PNG文件
     * @param keyWord    zTxt数据目标关键字
     * @param text       zTxt数据目标文本内容
     */
    public static void writeZTxtByKey(File pngFile, File outputFile, String keyWord, String text) throws IOException {
        Map<String, String> ztxtMap = readAllZTxt(pngFile);
        ztxtMap.put(keyWord, text);
        writeZTxt(pngFile, outputFile, ztxtMap);
    }

    /**
     * 向PNG文件中添加zTXT chunk
     *
     * @param pngFile    输入的PNG文件
     * @param outputFile 输出的PNG文件
     * @param zTxtData   包含zTXT数据的Map
     */
    public static void writeZTxt(File pngFile, File outputFile, Map<String, String> zTxtData) throws IOException {
        try (FileInputStream fis = new FileInputStream(pngFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            byte[] pngHeader = new byte[PNG_HEADER_SIZE];
            int bytesRead = fis.read(pngHeader);
            if (bytesRead != PNG_HEADER_SIZE) {
                throw new IOException("Failed to read PNG header");
            }
            dos.write(pngHeader);

            while (fis.available() > 0) {
                int length = readInt(fis);
                if (length < 0 || length > Integer.MAX_VALUE - CHUNK_TYPE_SIZE - CRC_SIZE) {
                    throw new IOException("Invalid chunk length: " + length);
                }

                byte[] chunkType = new byte[CHUNK_TYPE_SIZE];
                bytesRead = fis.read(chunkType);
                if (bytesRead != CHUNK_TYPE_SIZE) {
                    throw new IOException("Failed to read chunk type");
                }
                String chunkName = new String(chunkType, StandardCharsets.UTF_8);

                byte[] data = new byte[length];
                bytesRead = fis.read(data);
                if (bytesRead != length) {
                    throw new IOException("Failed to read chunk data");
                }
                int crc = readInt(fis);

                if (CHUNK_TYPE_IEND.equals(chunkName)) {
                    for (Map.Entry<String, String> entry : zTxtData.entrySet()) {
                        writeZTxtChunk(dos, entry.getKey(), entry.getValue());
                    }
                }

                writeChunk(dos, chunkName, data, crc);
            }
        }
    }

    /**
     * 向PNG文件中写入私有块
     *
     * @param pngFile    输入的PNG文件
     * @param outputFile 输出的PNG文件
     * @param chunkType  块类型
     * @param object     要写入的对象
     */
    public static void writePrivateChunk(File pngFile, File outputFile, String chunkType, Object object) throws IOException {
        writePrivateChunk(pngFile, outputFile, chunkType, object, false);
    }

    /**
     * 向PNG文件中写入私有块
     *
     * @param pngFile        输入的PNG文件
     * @param outputFile     输出的PNG文件
     * @param chunkType      要写入的块类型
     * @param object         要写入的对象
     * @param deleteExisting 是否删除已存在的相同类型块
     */
    public static void writePrivateChunk(File pngFile, File outputFile, String chunkType, Object object, boolean deleteExisting) throws IOException {
        byte[] data = serializeObject(object);

        try (FileInputStream fis = new FileInputStream(pngFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos)) {

            byte[] pngHeader = new byte[PNG_HEADER_SIZE];
            int bytesRead = fis.read(pngHeader);
            if (bytesRead != PNG_HEADER_SIZE) {
                throw new IOException("Failed to read PNG header");
            }
            dos.write(pngHeader);

            byte[] iendChunkData = null;
            int iendChunkCRC = 0;

            while (fis.available() > 0) {
                int length = readInt(fis);
                if (length < 0 || length > Integer.MAX_VALUE - CHUNK_TYPE_SIZE - CRC_SIZE) {
                    throw new IOException("Invalid chunk length: " + length);
                }

                byte[] typeBuffer = new byte[CHUNK_TYPE_SIZE];
                bytesRead = fis.read(typeBuffer);
                if (bytesRead != CHUNK_TYPE_SIZE) {
                    throw new IOException("Failed to read chunk type");
                }

                byte[] chunkData = new byte[length];
                bytesRead = fis.read(chunkData);
                if (bytesRead != length) {
                    throw new IOException("Failed to read chunk data");
                }
                int crc = readInt(fis);

                String currentChunkType = new String(typeBuffer, StandardCharsets.UTF_8);

                if (CHUNK_TYPE_IEND.equals(currentChunkType)) {
                    iendChunkData = chunkData;
                    iendChunkCRC = crc;
                    continue;
                }

                if (deleteExisting && currentChunkType.equals(chunkType)) {
                    continue;
                }

                writeChunk(dos, currentChunkType, chunkData, crc);
            }

            byte[] chunkTypeBytes = chunkType.getBytes(StandardCharsets.UTF_8);
            writeChunk(dos, chunkType, data, calculateCRC(chunkTypeBytes, data));

            if (iendChunkData != null) {
                writeChunk(dos, CHUNK_TYPE_IEND, iendChunkData, iendChunkCRC);
            }
        }
    }

    /**
     * 从PNG文件中读取第一个指定类型的私有chunk
     *
     * @param pngFile   要读取的PNG文件
     * @param chunkType 要读取的chunk类型
     */
    public static <T> T readFirstPrivateChunk(File pngFile, String chunkType) throws IOException, ClassNotFoundException {
        List<T> objects = readPrivateChunk(pngFile, chunkType, true);
        return objects.isEmpty() ? null : objects.get(0);
    }

    /**
     * 读取PNG文件中指定类型的最后一个私有块数据
     *
     * @param pngFile   PNG文件对象
     * @param chunkType 要读取的chunk类型
     */
    public static <T> T readLastPrivateChunk(File pngFile, String chunkType) throws IOException, ClassNotFoundException {
        List<T> objects = readPrivateChunk(pngFile, chunkType, true);
        return objects.isEmpty() ? null : objects.get(objects.size() - 1);
    }

    /**
     * 读取PNG文件中指定类型的私有块
     *
     * @param pngFile   PNG文件对象
     * @param chunkType 要读取的chunk类型
     */
    public static <T> List<T> readAllPrivateChunks(File pngFile, String chunkType) throws IOException, ClassNotFoundException {
        return readPrivateChunk(pngFile, chunkType, false);
    }

    /**
     * 从PNG输入流中读取第一个指定类型的私有chunk
     *
     * @param inputStream 要读取的PNG输入流
     * @param chunkType   要读取的chunk类型
     */
    public static <T> T readFirstPrivateChunk(InputStream inputStream, String chunkType) throws IOException, ClassNotFoundException {
        List<T> objects = readPrivateChunk(inputStream, chunkType, true);
        return objects.isEmpty() ? null : objects.get(0);
    }

    /**
     * 从PNG输入流中读取最后一个指定类型的私有chunk
     *
     * @param inputStream 要读取的PNG输入流
     * @param chunkType   要读取的chunk类型
     */
    public static <T> T readLastPrivateChunk(InputStream inputStream, String chunkType) throws IOException, ClassNotFoundException {
        List<T> objects = readPrivateChunk(inputStream, chunkType, true);
        return objects.isEmpty() ? null : objects.get(objects.size() - 1);
    }

    /**
     * 读取PNG输入流中指定类型的所有私有块
     *
     * @param inputStream 要读取的PNG输入流
     * @param chunkType   要读取的chunk类型
     */
    public static <T> List<T> readAllPrivateChunks(InputStream inputStream, String chunkType) throws IOException, ClassNotFoundException {
        return readPrivateChunk(inputStream, chunkType, false);
    }

    /**
     * 写入压缩的文本块数据
     *
     * @param dos     数据输出流
     * @param keyWord zTxt数据目标关键字
     * @param text    zTxt数据目标文本内容
     */
    private static void writeZTxtChunk(DataOutputStream dos, String keyWord, String text) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream chunkData = new DataOutputStream(baos)) {

            chunkData.writeBytes(keyWord);
            chunkData.writeByte(0);
            chunkData.writeByte(COMPRESSION_METHOD_DEFLATE);

            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            Deflater deflater = new Deflater();
            try (ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
                 DeflaterOutputStream dosCompress = new DeflaterOutputStream(compressedData, deflater)) {
                dosCompress.write(textBytes);
                dosCompress.finish();
                chunkData.write(compressedData.toByteArray());
            } finally {
                deflater.end();
            }

            byte[] chunkContent = baos.toByteArray();
            byte[] chunkTypeBytes = CHUNK_TYPE_ZTXT.getBytes(StandardCharsets.UTF_8);
            writeChunk(dos, CHUNK_TYPE_ZTXT, chunkContent, calculateCRC(chunkTypeBytes, chunkContent));
        }
    }

    /**
     * 读取空字节终止的字符串
     * 在字节数组中读取直到遇到空字节(0)为止的字节序列，并将其转换为字符串
     */
    private static String readNullTerminatedString(byte[] data) {
        int i = 0;
        while (i < data.length && data[i] != 0) {
            i++;
        }
        return new String(data, 0, i, StandardCharsets.UTF_8);
    }

    /**
     * 对压缩后的文本进行解压缩
     */
    private static String inflateText(byte[] compressedText) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedText);
             InflaterInputStream inflater = new InflaterInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[INFLATE_BUFFER_SIZE];
            int len;
            while ((len = inflater.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * 验证给定的字节数组是否为有效的PNG文件头
     */
    private static boolean isPNGHeaderValid(byte[] header) {
        if (header == null || header.length < PNG_HEADER_SIZE) {
            return false;
        }
        for (int i = 0; i < PNG_HEADER_SIZE; i++) {
            if (header[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从PNG文件中读取特定类型的私有块数据
     */
    private static <T> List<T> readPrivateChunk(File pngFile, String chunkType, boolean readFirst) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(pngFile)) {
            return readPrivateChunk(fis, chunkType, readFirst);
        }
    }

    /**
     * 读取PNG输入流中的指定类型的私有块
     */
    private static <T> List<T> readPrivateChunk(InputStream inputStream, String chunkType, boolean stopAtFirst) throws IOException, ClassNotFoundException {
        List<T> privateChunks = new ArrayList<>();

        try (DataInputStream dis = new DataInputStream(inputStream)) {
            byte[] pngHeader = new byte[PNG_HEADER_SIZE];
            dis.readFully(pngHeader);
            if (!isPNGHeaderValid(pngHeader)) {
                throw new IOException("Invalid PNG stream.");
            }

            while (dis.available() > 0) {
                int length = dis.readInt();
                if (length < 0 || length > Integer.MAX_VALUE - CHUNK_TYPE_SIZE - CRC_SIZE) {
                    throw new IOException("Invalid chunk length: " + length);
                }

                byte[] chunkTypeBytes = new byte[CHUNK_TYPE_SIZE];
                dis.readFully(chunkTypeBytes);
                String chunkName = new String(chunkTypeBytes, StandardCharsets.UTF_8);

                byte[] data = new byte[length];
                dis.readFully(data);
                dis.skipBytes(CRC_SIZE);

                if (chunkName.equals(chunkType)) {
                    T chunkObject = deserializeObject(data);
                    privateChunks.add(chunkObject);
                    if (stopAtFirst) {
                        break;
                    }
                }
            }
        }

        return privateChunks;
    }

    /**
     * 写入数据块到输出流中
     */
    private static void writeChunk(DataOutputStream dos, String chunkType, byte[] data, int crc) throws IOException {
        // 写入数据长度，以便接收方知道预期接收的数据量
        dos.writeInt(data.length);
        // 写入数据块类型，以便接收方可以根据类型处理数据
        dos.writeBytes(chunkType);
        // 写入实际数据
        dos.write(data);
        // 写入CRC校验码，以便接收方可以校验数据的完整性
        dos.writeInt(crc);
    }

    /**
     * 从输入流中读取一个整数（大端序）
     */
    private static int readInt(InputStream is) throws IOException {
        int b1 = is.read();
        int b2 = is.read();
        int b3 = is.read();
        int b4 = is.read();
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) {
            throw new IOException("Unexpected end of stream while reading integer");
        }
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    /**
     * 计算输入字节数组的CRC校验码
     */
    private static int calculateCRC(byte[] type, byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(type);
        crc32.update(data);
        return (int) crc32.getValue();
    }

    /**
     * 序列化对象为字节数组
     */
    private static byte[] serializeObject(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            // 将对象序列化到流中
            oos.writeObject(obj);
            // 返回流中的字节数组表示
            return baos.toByteArray();
        }
    }

    /**
     * 反序列化字节数组为对象
     */
    @SuppressWarnings("unchecked")
    private static <T> T deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }


    public static BufferedImage readImage(File file) throws IOException {
        return ImageIO.read(file);
    }

    public static void writeImage(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }

    public static Color getPixelColor(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        return new Color(rgb, true);
    }

    public static BufferedImage cropImage(BufferedImage src, int x, int y, int width, int height) {
        return src.getSubimage(x, y, width, height);
    }

    public static BufferedImage concatImagesHorizontally(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth() + img2.getWidth();
        int height = Math.max(img1.getHeight(), img2.getHeight());

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(img1, 0, 0, null);
            g.drawImage(img2, img1.getWidth(), 0, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    public static BufferedImage concatImagesVertically(BufferedImage img1, BufferedImage img2) {
        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = img1.getHeight() + img2.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(img1, 0, 0, null);
            g.drawImage(img2, 0, img1.getHeight(), null);
        } finally {
            g.dispose();
        }
        return result;
    }

    public static BufferedImage createBlankImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        return image;
    }
}
