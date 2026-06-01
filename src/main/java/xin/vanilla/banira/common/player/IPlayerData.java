package xin.vanilla.banira.common.player;

import net.minecraft.nbt.CompoundNBT;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;

/**
 * 玩家数据
 */
public interface IPlayerData<T extends IPlayerData<T>> {

    boolean isDirty();

    void setDirty();

    void setDirty(boolean dirty);

    void writeToBuffer(BaniraPacketBuffer buffer);

    void readFromBuffer(BaniraPacketBuffer buffer);

    CompoundNBT serializeNBT();

    void deserializeNBT(CompoundNBT nbt, boolean dirty);

    void copyFrom(T playerData);

    void save();

    default void saveEx() {
        if (this.isDirty()) {
            this.setDirty(false);
            this.save();
        }
    }

}
