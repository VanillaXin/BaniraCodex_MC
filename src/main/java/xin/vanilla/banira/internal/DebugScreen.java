package xin.vanilla.banira.internal;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.component.Text;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.util.*;
import xin.vanilla.banira.common.data.CircularList;
import xin.vanilla.banira.common.util.Component;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.RandomStringUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugScreen extends Screen {

    private static final CircularList<String> textures = CircularList.asList("textures/gui/sakura_cat.png", "textures/gui/aotake_cat.png");
    /**
     * 键盘与鼠标事件管理器
     */
    protected final KeyEventManager keyManager = new KeyEventManager();
    private final MouseHelper mouseHelper = new MouseHelper();
    private String content = "";
    private int contentLines = 2;
    private int contentLength = 20;
    private int fontSize = 9;
    private boolean warp = false;
    private int contentTextureIndex = 0;

    @Override
    protected void init() {
        super.init();
    }

    protected DebugScreen(ITextComponent textComponent) {
        super(textComponent);
    }

    protected DebugScreen(Component component) {
        super(component.toTextComponent());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        mouseHelper.tick(mouseX, mouseY);

        AbstractGuiUtils.fill(matrixStack, 10, 10, this.width - 20, this.height - 20, 0x44000000, 8);

        int hudY = 1;
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal("contentLines：" + contentLines)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal("contentLength：" + contentLength)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal("fontSize：" + fontSize)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal("warp：" + warp)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));

        if (StringUtils.isNullOrEmptyEx(content)) genContent();
        if (mouseHelper.isLeftPressing()) {
            AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal(content)
                            .stack(matrixStack)
                            .font(super.font))
                    .x(mouseX).y(mouseY).fontSize(fontSize)
                    .wrap(warp).maxWidth(warp ? AbstractGuiUtils.multilineTextWidth(this.content) / 2 : 0)
            );
        } else if (mouseHelper.isRightPressing()) {
            AbstractGuiUtils.drawPopupMessage(FontDrawArgs.of(Text.literal(content)
                            .stack(matrixStack)
                            .font(super.font))
                    .x(mouseX).y(mouseY).padding(0).fontSize(fontSize)
                    .wrap(warp).maxWidth(warp ? AbstractGuiUtils.multilineTextWidth(this.content) / 2 : 0)
                    .texture(TextureUtils.loadCustomTexture(BaniraCodex.resourceFactory(), textures.get(contentTextureIndex))));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (Minecraft.getInstance().screen == null && KeyboardHelper.isKeyPressing(GLFW.GLFW_KEY_DELETE)) {
                Minecraft.getInstance().setScreen(new DebugScreen(Component.empty()));
            }
        }
    }

    /**
     * 键盘按下事件
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.keyManager.keyPressed(keyCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_E)) {
            if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.contentLength++;
                genContent();
            } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.contentLength--;
                genContent();
            }
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_C)) {
            if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.contentLines++;
                genContent();
            } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.contentLines--;
                genContent();
            }
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_T)) {
            if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.contentTextureIndex++;
            } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.contentTextureIndex--;
            }
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_F)) {
            if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_EQUAL)) {
                this.fontSize++;
            } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_MINUS)) {
                this.fontSize--;
            }
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_LEFT_CONTROL) && keyManager.isKeyPressed(GLFWKey.GLFW_KEY_W)) {
            this.warp = !this.warp;
        }
        this.keyManager.keyReleased(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void genContent() {
        StringBuilder content = new StringBuilder(DateUtils.toString(new Date()) + "\n");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < contentLines; i++) {
            RandomStringUtils.CharSource source;
            switch (random.nextInt(5)) {
                case 0:
                    source = RandomStringUtils.CharSource.DIGITS;
                    break;
                case 1:
                    source = RandomStringUtils.CharSource.ALPHANUMERIC;
                    break;
                case 2:
                    source = RandomStringUtils.CharSource.SPECIAL_CHARACTERS;
                    break;
                case 3:
                    source = RandomStringUtils.CharSource.ASCII_PRINTABLE;
                    break;
                default:
                    source = RandomStringUtils.CharSource.CHINESE;
                    break;
            }
            content.append(RandomStringUtils.generate(random.nextInt((contentLength + 1) / 2) + contentLength / 2, source)).append("\n");
        }
        this.content = content.substring(0, content.length() - 1);
    }
}
