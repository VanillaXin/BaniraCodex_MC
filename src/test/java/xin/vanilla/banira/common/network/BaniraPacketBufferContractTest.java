package xin.vanilla.banira.common.network;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * 锁定子 mod 实际协议需要的加载器无关 buffer 能力。
 */
public class BaniraPacketBufferContractTest {

    @Test
    public void exposesStablePrimitiveAndJavaValueMethods() throws Exception {
        assertEquals(byte.class, BaniraPacketBuffer.class.getMethod("readByte").getReturnType());
        BaniraPacketBuffer.class.getMethod("writeByte", int.class);
        assertEquals(double.class, BaniraPacketBuffer.class.getMethod("readDouble").getReturnType());
        BaniraPacketBuffer.class.getMethod("writeDouble", double.class);
        assertEquals(UUID.class, BaniraPacketBuffer.class.getMethod("readUuid").getReturnType());
        BaniraPacketBuffer.class.getMethod("writeUuid", UUID.class);
        BaniraPacketBuffer.class.getMethod("readEnum", Class.class);
        BaniraPacketBuffer.class.getMethod("writeEnum", Enum.class);
    }
}
