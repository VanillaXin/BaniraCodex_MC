package xin.vanilla.banira.internal.fabric;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定完整功能基线在 Fabric 1.18.2 的实际启动与轮询接线。 */
public class FabricFullFeatureWiringContractTest {
    @Test
    public void quickActionsAndHotReloadAreWiredIntoRuntime() throws Exception {
        String network = source("src/main/java/xin/vanilla/banira/internal/network/NetworkInit.java");
        String bootstrap = source("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java");
        String client = source("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java");
        String common = source("src/main/java/xin/vanilla/banira/internal/common/BaniraCodexRuntime.java");
        String config = source("src/main/java/xin/vanilla/banira/internal/fabric/config/FabricConfigAdapter.java");
        String eventBridge = source("src/main/java/xin/vanilla/banira/internal/client/BaniraClientEventBridge.java");
        String creativeMixin = source("src/main/java/xin/vanilla/banira/internal/mixin/injections/CreativeScreenQuickActionMixin.java");

        assertTrue(network.contains("HANDLER.register(QuickActionCommandsToServer.class"));
        assertTrue(bootstrap.contains("customActions.reload()"));
        assertTrue(bootstrap.contains("ExternalInventoryButtonManager.get().refreshCurrentScreen()"));
        assertTrue(client.contains("CustomQuickActionManager.get().tickKeyBindings()"));
        assertTrue(client.contains("ManagedConfigFiles.poll(ManagedConfigFiles.Scope.CLIENT)"));
        assertTrue(common.contains("ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON)"));
        assertTrue(common.contains("BaniraVirtualPermissionRegistry.register(commandType)"));
        assertTrue(config.contains("ManagedConfigFiles.register(file, scope"));
        assertTrue(config.contains("store.reloadFromDisk()"));
        assertTrue(config.contains("holder.acceptExternalReload()"));
        assertTrue(eventBridge.contains("BaniraClientEventHub.Client.fireGuiChanged"));
        assertTrue(eventBridge.contains("BaniraClientEventHub.Client.fireDrawScreenPreNative"));
        assertTrue(eventBridge.contains("BaniraClientEventHub.Client.fireDrawScreenPostNative"));
        assertTrue(creativeMixin.contains("banira$suppressCoveredTabTooltip(GuiGraphics graphics"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
