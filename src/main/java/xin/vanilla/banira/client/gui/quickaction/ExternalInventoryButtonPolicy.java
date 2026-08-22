package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

/** 解析外部背包按钮配置，并处理 FTB Library 缺失时的安全回退。 */
public final class ExternalInventoryButtonPolicy {
    private ExternalInventoryButtonPolicy() {
    }

    public static EnumExternalInventoryButtonHost resolve(
            EnumExternalInventoryButtonHost configuredHost,
            boolean ftbLibraryAvailable
    ) {
        EnumExternalInventoryButtonHost host = configuredHost == null
                ? EnumExternalInventoryButtonHost.BANIRA : configuredHost;
        if (host == EnumExternalInventoryButtonHost.FTB_LIBRARY && !ftbLibraryAvailable) {
            return EnumExternalInventoryButtonHost.BANIRA;
        }
        return host;
    }
}
