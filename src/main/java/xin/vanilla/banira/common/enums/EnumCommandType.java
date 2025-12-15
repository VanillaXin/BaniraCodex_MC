package xin.vanilla.banira.common.enums;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.IVirtualPermissionType;

@Getter
@Accessors(fluent = true)
public enum EnumCommandType implements IVirtualPermissionType {
    HELP(false, false),
    LANGUAGE(false, false),
    LANGUAGE_CONCISE(),
    VIRTUAL_OP(),
    VIRTUAL_OP_CONCISE(),
    ;

    /**
     * 是否在帮助信息中忽略
     */
    private final boolean ignore;
    /**
     * 是否简短指令
     */
    private final boolean concise = this.name().endsWith("_CONCISE");
    /**
     * 是否被虚拟权限管理
     */
    private final boolean op;

    EnumCommandType() {
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig) {
        this.ignore = ig;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig, boolean op) {
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    public int sort() {
        return this.ordinal();
    }

    // region IVirtualPermissionType 实现

    @Override
    public String modId() {
        return BaniraCodex.MODID;
    }

    /**
     * 使用非精简形式的名字作为逻辑 id，
     * 这样同一指令的精简 / 非精简版本共用一条虚拟权限。
     */
    @Override
    public String id() {
        return this.replaceConcise().name();
    }

    // endregion

    public EnumCommandType replaceConcise() {
        if (this.name().endsWith("_CONCISE")) {
            return EnumCommandType.valueOf(this.name().replace("_CONCISE", ""));
        }
        return this;
    }
}
