package xin.vanilla.banira.client.enums;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 逻辑运算符枚举
 */
@Accessors(fluent = true)
public enum EnumLogicalOperator {
    AND("&&"),
    OR("||"),
    NOT("!"),
    ;

    @Getter
    private final String operator;

    EnumLogicalOperator(String operator) {
        this.operator = operator;
    }
}
