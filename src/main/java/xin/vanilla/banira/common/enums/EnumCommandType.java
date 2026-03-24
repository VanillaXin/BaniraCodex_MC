package xin.vanilla.banira.common.enums;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.commands.CommandSourceStack;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.command.impl.HelpCommand;
import xin.vanilla.banira.command.impl.LanguageCommand;
import xin.vanilla.banira.command.impl.VirtualOpCommand;
import xin.vanilla.banira.common.api.IVirtualPermissionType;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
public enum EnumCommandType implements IVirtualPermissionType {
    HELP(HelpCommand::create, false, false),
    LANGUAGE(LanguageCommand::create, false, false),
    LANGUAGE_CONCISE(),
    EDIT_SERVER_CONFIG(true, true),
    VIRTUAL_OP(VirtualOpCommand::create),
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

    @Nullable
    private final Supplier<LiteralArgumentBuilder<CommandSourceStack>> instance;

    EnumCommandType() {
        this.instance = null;
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig) {
        this.instance = null;
        this.ignore = ig;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig, boolean op) {
        this.instance = null;
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    EnumCommandType(@Nullable Supplier<LiteralArgumentBuilder<CommandSourceStack>> instance) {
        this.instance = instance;
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(@Nullable Supplier<LiteralArgumentBuilder<CommandSourceStack>> instance, boolean ig, boolean op) {
        this.instance = instance;
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
