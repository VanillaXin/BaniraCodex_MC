package xin.vanilla.banira.common.network;

import xin.vanilla.banira.api.BaniraIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 测试用内存 buffer，只验证 Banira 公共协议的读写顺序。
 */
public final class TestBaniraPacketBuffer implements BaniraPacketBuffer {
    private final List<Object> values = new ArrayList<>();
    private int cursor;

    public TestBaniraPacketBuffer rewind() {
        cursor = 0;
        return this;
    }

    @Override
    public String readUtf() {
        return readUtf(32767);
    }

    @Override
    public String readUtf(int maxLength) {
        return (String) values.get(cursor++);
    }

    @Override
    public void writeUtf(String value) {
        writeUtf(value, 32767);
    }

    @Override
    public void writeUtf(String value, int maxLength) {
        values.add(value);
    }

    @Override
    public int readInt() {
        return (Integer) values.get(cursor++);
    }

    @Override
    public void writeInt(int value) {
        values.add(value);
    }

    @Override
    public int readVarInt() {
        return (Integer) values.get(cursor++);
    }

    @Override
    public void writeVarInt(int value) {
        values.add(value);
    }

    @Override
    public long readLong() {
        return (Long) values.get(cursor++);
    }

    @Override
    public void writeLong(long value) {
        values.add(value);
    }

    @Override
    public boolean readBoolean() {
        return (Boolean) values.get(cursor++);
    }

    @Override
    public void writeBoolean(boolean value) {
        values.add(value);
    }

    @Override
    public byte readByte() {
        return (Byte) values.get(cursor++);
    }

    @Override
    public void writeByte(int value) {
        values.add((byte) value);
    }

    @Override
    public double readDouble() {
        return (Double) values.get(cursor++);
    }

    @Override
    public void writeDouble(double value) {
        values.add(value);
    }

    @Override
    public UUID readUuid() {
        return (UUID) values.get(cursor++);
    }

    @Override
    public void writeUuid(UUID value) {
        values.add(value);
    }

    @Override
    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        return enumClass.cast(values.get(cursor++));
    }

    @Override
    public void writeEnum(Enum<?> value) {
        values.add(value);
    }

    @Override
    public BaniraIdentifier readIdentifier() {
        return (BaniraIdentifier) values.get(cursor++);
    }

    @Override
    public void writeIdentifier(BaniraIdentifier value) {
        values.add(value);
    }
}
