package xin.vanilla.banira.internal;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.AdvancementSelectScreen;
import xin.vanilla.banira.client.gui.ItemSelectScreen;
import xin.vanilla.banira.client.gui.StringInputScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.*;
import xin.vanilla.banira.common.data.CircularList;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final CircularList<String> textures = CircularList.asList(
            "textures/gui/sakura_cat.png"
            , "textures/gui/aotake_cat.png"
            , "textures/gui/narcissus_cat.png"
            , "textures/gui/snowflake_cat.png"
            , "textures/gui/sakura_moe.png"
    );

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
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        mouseHelper.tick(mouseX, mouseY);

        ShapeDrawArgs bgRect = ShapeDrawArgs.rect(stack, 10, 10, this.width - 20, this.height - 20, 0x44000000);
        bgRect.rect().radius(8).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        AbstractGuiUtils.drawShape(bgRect);

        // AbstractGuiUtils.drawLine(stack, 20, 20, 150, 150, 1, 0x33FFFFFF);

        // // 白色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.BACKGROUND, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 50) / 2, (super.height - 50) / 2, 50, 50, 0xFFFFFFFF)
        // );
        // // 红色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.OVERLAY, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 10) / 2, (super.height - 10) / 2, 10, 10, 0xFFFF0000)
        // );
        // // 黑色矩形
        // AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.FOREGROUND, (s) ->
        //         AbstractGuiUtils.fill(s, (super.width - 30) / 2, (super.height - 30) / 2, 30, 30, 0xFF000000)
        // );

        ShapeDrawArgs circle = ShapeDrawArgs.circle(stack, super.width / 2f, super.height / 2f, 15, 0x33FFFFFF);
        AbstractGuiUtils.drawShape(circle);

        // ShapeDrawArgs ellipse = ShapeDrawArgs.ellipse(stack, super.width / 2f, super.height / 2f, 7.5f, 15, 0x33FFFFFF);
        // ellipse.ellipse().rotation((System.currentTimeMillis() / 50d) % 360);
        // AbstractGuiUtils.drawShape(ellipse);

        ShapeDrawArgs ellipseRing = ShapeDrawArgs.ellipse(stack, super.width / 2f, super.height / 2f, 7.5f, 15, 0x33FFFFFF);
        ellipseRing.ellipse().rotation((System.currentTimeMillis() / 50d) % 360).border(2);
        AbstractGuiUtils.drawShape(ellipseRing);

        // ShapeDrawArgs ring = ShapeDrawArgs.circle(stack, super.width / 2f, super.height / 2f, 17, 0x33FFFFFF);
        // ring.circle().border(0.5f);
        // AbstractGuiUtils.drawShape(ring);

        // ShapeDrawArgs sector = ShapeDrawArgs.sector(stack, (super.width - 35) / 2f, (super.height - 35) / 2f, 35, 0, 75, 0x33FFFFFF);
        // AbstractGuiUtils.drawShape(sector);

        // ShapeDrawArgs sectoredRing = ShapeDrawArgs.sectorRing(stack, super.width / 2f, super.height / 2f, 35, 30, 180, 255, 0x33FFFFFF);
        // AbstractGuiUtils.drawShape(sectoredRing);

        ShapeDrawArgs rect1 = ShapeDrawArgs.rect(stack, (super.width - 60) / 2f, (super.height - 60) / 2f, 60, 60, 0x33000000);
        rect1.rect().topRight(15).bottomLeft(15).bottomRight(15);
        AbstractGuiUtils.drawShape(rect1);

        ShapeDrawArgs rect11 = ShapeDrawArgs.rect(stack, (super.width - 70) / 2f, (super.height - 70) / 2f, 70, 70, 0x33000000);
        rect11.rect().border(4).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE).topLeft(2).topRight(15).bottomLeft(15).bottomRight(15);
        AbstractGuiUtils.drawShape(rect11);

        int hudY = 1;
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal("contentLines：" + contentLines)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal("contentLength：" + contentLength)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal("fontSize：" + fontSize)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));
        AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal("warp：" + warp)).x(5).y(20 * hudY++).padding(4).margin(0).inScreen(false));

        if (StringUtils.isNullOrEmptyEx(content)) genContent();
        if (mouseHelper.isLeftPressing()) {
            AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal(content)
                            .stack(stack)
                            .font(super.font)
                            .align(EnumAlignment.CENTER))
                    .x(mouseX).y(mouseY).fontSize(fontSize).align(EnumAlignment.CENTER)
                    .wrap(warp).maxWidth(warp ? AbstractGuiUtils.multilineTextWidth(this.content) / 2 : 0)
            );
        } else if (mouseHelper.isRightPressing()) {
            AbstractGuiUtils.drawPopupMessage(FontDrawArgs.ofPopo(Text.literal(content)
                            .stack(stack)
                            .font(super.font)
                            .align(EnumAlignment.CENTER))
                    .x(mouseX).y(mouseY).padding(0).fontSize(fontSize).align(EnumAlignment.CENTER)
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
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_INSERT)) {
            Minecraft.getInstance().setScreen(new ItemSelectScreen(new ItemSelectScreen.Args().parentScreen(this).onDataReceived((itemStack) -> {
                LOGGER.debug("Select itemStack: {}", ItemUtils.serializeItemStack(itemStack));
            })));
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_HOME)) {
            StringInputScreen.Args screenArgs = new StringInputScreen.Args()
                    .setParentScreen(this)
                    .addWidget(new StringInputScreen.Widget()
                            .name("name")
                            .title(Text.literal("enter_name").shadow(true))
                            .validator((input) -> {
                                if (StringUtils.isNullOrEmptyEx(input.value())) {
                                    return Component.translatableClient(EnumI18nType.TIPS, "enter_value_s_error", input.value()).toString();
                                }
                                return null;
                            })
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("author")
                            .title(Text.literal("enter_author_name").shadow(true))
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("version")
                            .title(Text.literal("enter_version").shadow(true))
                    )
                    .addWidget(new StringInputScreen.Widget()
                            .name("description")
                            .title(Text.literal("enter_description").shadow(true))
                            .allowEmpty(true)
                    )
                    .setCallback(input -> LOGGER.debug("Entered name: {}", input.value("name")));
            Minecraft.getInstance().setScreen(new StringInputScreen(screenArgs));
        } else if (keyManager.isKeyPressed(GLFWKey.GLFW_KEY_PAGE_UP)) {
            AdvancementSelectScreen.Args args = new AdvancementSelectScreen.Args();
            args.parentScreen(this)
                    .defaultAdvancement(BaniraCodex.resourceFactory().empty())
                    .onDataReceived(input -> {
                        LOGGER.debug("Selected advancement: {}", input);
                    });
            Minecraft.getInstance().setScreen(new AdvancementSelectScreen(args));
        }
        this.keyManager.keyReleased(keyCode);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void genContent() {
        StringBuilder content = new StringBuilder(Component.translatableClient(EnumI18nType.WORD, "banira_codex").modId(BaniraCodex.MODID).toString()).append("\n");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < contentLines - 1; i++) {
            if (i == 0) {
                Component component = Component.literal("Copyright (c) %s ").appendArg(DateUtils.getYearPart(new Date()))
                        .append(Component.translatableClient(EnumI18nType.WORD, "vanilla_xin").modId(BaniraCodex.MODID));
                content.append(component.toString()).append("\n");
            } else {
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
        }
        this.content = content.substring(0, content.length() - 1);
    }
}
