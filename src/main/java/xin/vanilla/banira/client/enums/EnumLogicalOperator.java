package xin.vanilla.banira.client.enums;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 逻辑运算符枚举
 */
@Accessors(fluent = true)
public enum EnumLogicalOperator implements IEnumDescribable {
    AND("&&"),
    OR("||"),
    NOT("!"),
    ;

    @Getter
    private final String operator;

    EnumLogicalOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
