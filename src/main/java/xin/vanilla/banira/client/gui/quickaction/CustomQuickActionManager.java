package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.client.BaniraQuickActionScreenFactory;
import xin.vanilla.banira.api.client.event.BaniraKeyboardEvent;
import xin.vanilla.banira.api.quickaction.*;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.client.util.GLFWKeyUtils;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.common.network.packet.QuickActionCommandsToServer;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** 加载、注册并执行玩家自定义快捷入口。 */
public final class CustomQuickActionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final CustomQuickActionManager INSTANCE = new CustomQuickActionManager();
    private static final String FILE_NAME = "quick_actions.json";
    private static final String ENTRY_PREFIX = BaniraCodex.MODID + ":custom/";
    private static final Type DEFINITION_LIST = new TypeToken<List<CustomQuickActionDefinition>>() { }.getType();

    private final List<CustomQuickActionDefinition> definitions = new ArrayList<>();
    private final Set<String> registeredIds = new HashSet<>();
    private final Map<String, BaniraQuickActionScreenFactory> screenFactories = new HashMap<>();
    private boolean hotReloadRegistered;

    private CustomQuickActionManager() {
    }

    public static CustomQuickActionManager get() {
        return INSTANCE;
    }

    public synchronized List<CustomQuickActionDefinition> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions));
    }

    public synchronized void replaceDefinitions(List<CustomQuickActionDefinition> values) {
        definitions.clear();
        if (values != null) {
            for (CustomQuickActionDefinition value : values) {
                CustomQuickActionDefinition normalized = normalize(value);
                if (normalized != null) definitions.add(normalized);
            }
        }
        save();
        applyToRegistry();
    }

    public synchronized void reload() {
        Path file = configFile();
        if (!hotReloadRegistered) {
            ManagedConfigFiles.register(file, ManagedConfigFiles.Scope.CLIENT, this::reload);
            hotReloadRegistered = true;
        }
        List<CustomQuickActionDefinition> loadedDefinitions = new ArrayList<>();
        if (Files.isRegularFile(file)) {
            try {
                String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                List<CustomQuickActionDefinition> loaded = JsonUtils.GSON.fromJson(json, DEFINITION_LIST);
                if (loaded != null) {
                    for (CustomQuickActionDefinition value : loaded) {
                        CustomQuickActionDefinition normalized = normalize(value);
                        if (normalized != null) loadedDefinitions.add(normalized);
                    }
                }
            } catch (Exception exception) {
                LOGGER.warn("Failed to load custom quick actions from {}", file, exception);
                return;
            }
        }
        definitions.clear();
        definitions.addAll(loadedDefinitions);
        applyToRegistry();
    }

    public synchronized void save() {
        Path file = configFile();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.write(temporary, JsonUtils.PRETTY_GSON.toJson(definitions, DEFINITION_LIST)
                    .getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            ManagedConfigFiles.markWritten(file);
        } catch (Exception exception) {
            LOGGER.warn("Failed to save custom quick actions to {}", file, exception);
        }
    }

    public synchronized void registerScreen(@Nonnull String id, @Nonnull BaniraQuickActionScreenFactory factory) {
        screenFactories.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(factory, "factory"));
    }

    public synchronized List<String> screenIds() {
        List<String> ids = new ArrayList<>(screenFactories.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public void onKeyPressed(BaniraKeyboardEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.repeatedPress()) return;
        if (minecraft.screen != null) {
            return;
        }
        for (CustomQuickActionDefinition definition : definitions()) {
            Set<Integer> pressed = new LinkedHashSet<>(InputStateManager.instance().pressedKeyCodes());
            pressed.add(event.keyCode());
            if (definition.isEnabled() && GLFWKeyUtils.matchKey(definition.getKeyChord(),
                    pressed.stream().mapToInt(Integer::intValue).toArray())) {
                activate(definition, minecraft.screen);
                event.cancel();
                return;
            }
        }
    }

    private synchronized void applyToRegistry() {
        QuickActionRegistry registry = QuickActionRegistry.get();
        for (String id : registeredIds) registry.unregister(id);
        registeredIds.clear();
        for (CustomQuickActionDefinition definition : definitions) {
            if (!definition.isEnabled()) continue;
            String registryId = ENTRY_PREFIX + safeId(definition.getId());
            QuickIcon icon = resolveIcon(definition);
            List<QuickActionContextMenuItem> menuItems = new ArrayList<>();
            menuItems.add(new QuickActionContextMenuItem("edit",
                    BaniraComponent.get().transClientAuto("custom_quick_action_edit"),
                    context -> CustomQuickActionConfigScreen.openEditor(
                            context.currentScreen(), definition.getId())));
            for (CustomQuickActionMenuItem item : definition.getContextMenuItems()) {
                menuItems.add(new QuickActionContextMenuItem("custom/" + safeId(item.getId()),
                        BaniraComponent.get().literal(item.getLabel()),
                        context -> activate(item, context.currentScreen())));
            }
            boolean menuOnly = definition.getSteps().isEmpty()
                    && !definition.getContextMenuItems().isEmpty();
            registry.register(registryId, icon,
                    BaniraComponent.get().literal(definition.getLabel()),
                    EnumQuickActionDisplay.valueOf(definition.getDisplay().name()),
                    menuOnly ? null : context -> activate(definition, context.currentScreen()),
                    menuItems);
            QuickActionEntry registered = registry.getEntry(registryId);
            if (registered != null && menuOnly) {
                // 第一项是编辑入口；纯菜单快捷项左键只展示玩家配置的菜单内容。
                registered.primaryMenuItemOffset(1);
            }
            registeredIds.add(registryId);
        }
        ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }

    private void activate(CustomQuickActionDefinition definition, Screen parent) {
        activate(definition.getExecutionMode(), definition.getSteps(),
                definition.isCloseBeforeExecution(), parent);
    }

    private void activate(CustomQuickActionMenuItem item, Screen parent) {
        activate(item.getExecutionMode(), item.getSteps(), item.isCloseBeforeExecution(), parent);
    }

    private void activate(QuickActionExecutionMode mode, List<CustomQuickActionStep> steps,
                          boolean closeBeforeExecution, Screen parent) {
        if (closeBeforeExecution) {
            Minecraft.getInstance().setScreen(null);
        }
        List<CustomQuickActionStep> commandSteps = new ArrayList<>();
        CustomQuickActionStep screenStep = null;
        for (CustomQuickActionStep step : steps) {
            if (step == null) continue;
            if (step.getType() == QuickActionStepType.COMMAND) commandSteps.add(step);
            if (screenStep == null && step.getType() == QuickActionStepType.SCREEN
                    && step.getCondition() == QuickActionStepCondition.ALWAYS) screenStep = step;
        }
        executeCommands(mode, commandSteps);
        if (screenStep != null) openScreen(screenStep.getValue(), parent);
    }

    private void executeCommands(QuickActionExecutionMode mode, List<CustomQuickActionStep> steps) {
        if (steps.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && PlayerUtils.isRemoteServerModInstalled(
                minecraft.player, BaniraCodex.MODID)) {
            PacketUtils.sendPacketToServer(new QuickActionCommandsToServer(mode, steps));
            return;
        }
        // 服务端未安装 Banira 时仍支持独立指令；依赖执行结果的链式条件不会被臆测。
        if (minecraft.player != null) {
            for (CustomQuickActionStep step : steps) {
                if (mode == QuickActionExecutionMode.PARALLEL
                        || step.getCondition() == QuickActionStepCondition.ALWAYS) {
                    String command = step.getValue().trim();
                    minecraft.player.chat(command.startsWith("/") ? command : "/" + command);
                }
            }
        }
    }

    private void openScreen(String idOrClass, Screen parent) {
        if (idOrClass == null || idOrClass.trim().isEmpty()) return;
        try {
            BaniraQuickActionScreenFactory factory = screenFactories.get(idOrClass);
            Object created = factory != null ? factory.create(parent) : reflectScreen(idOrClass, parent);
            Screen target = created instanceof Screen ? (Screen) created : null;
            if (target != null) Minecraft.getInstance().setScreen(target);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to open quick-action screen {}", idOrClass, throwable);
        }
    }

    private static Screen reflectScreen(String className, Screen parent) throws Exception {
        Class<?> type = Class.forName(className, true, CustomQuickActionManager.class.getClassLoader());
        if (!Screen.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(className + " is not a Screen");
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(Screen.class);
            constructor.setAccessible(true);
            return (Screen) constructor.newInstance(parent);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (Screen) constructor.newInstance();
        }
    }

    static QuickIcon resolveIcon(CustomQuickActionDefinition definition) {
        String value = definition.getIcon() == null ? "" : definition.getIcon();
        ResourceLocation id = ResourceLocation.tryParse(value);
        try {
            switch (definition.getIconType()) {
                case EFFECT:
                    MobEffect effect = id == null ? null : Registry.MOB_EFFECT.getOptional(id).orElse(null);
                    return effect == null ? QuickIcon.item(Items.PAPER) : QuickIcon.effect(effect);
                case RESOURCE:
                    return id == null ? QuickIcon.item(Items.PAPER) : QuickIcon.resource(id);
                case EXTERNAL_FILE:
                    ResourceLocation texture = TextureUtils.loadCustomTexture(Identifier.id(), value);
                    return QuickIcon.resource(texture);
                case ITEM:
                default:
                    Item item = id == null ? Items.PAPER : Registry.ITEM.getOptional(id).orElse(Items.PAPER);
                    return QuickIcon.item(new ItemStack(item));
            }
        } catch (Exception exception) {
            return QuickIcon.item(Items.PAPER);
        }
    }

    static CustomQuickActionDefinition normalize(CustomQuickActionDefinition definition) {
        if (definition == null || definition.getId() == null || definition.getId().trim().isEmpty()) {
            return null;
        }
        CustomQuickActionDefinition result = new CustomQuickActionDefinition()
                .setId(definition.getId().trim())
                .setLabel(definition.getLabel() == null ? definition.getId().trim() : definition.getLabel())
                .setEnabled(definition.isEnabled())
                .setDisplay(definition.getDisplay() == null ? QuickActionDisplayMode.ICON : definition.getDisplay())
                .setIconType(definition.getIconType() == null ? QuickActionIconType.ITEM : definition.getIconType())
                .setIcon(definition.getIcon() == null ? "minecraft:paper" : definition.getIcon())
                .setKeyChord(definition.getKeyChord() == null ? "" : definition.getKeyChord())
                .setCloseBeforeExecution(definition.isCloseBeforeExecution())
                .setExecutionMode(definition.getExecutionMode() == null
                        ? QuickActionExecutionMode.PARALLEL : definition.getExecutionMode());
        List<CustomQuickActionStep> steps = new ArrayList<>();
        if (definition.getSteps() != null) {
            for (CustomQuickActionStep step : definition.getSteps()) {
                if (step == null || step.getType() == null || step.getValue() == null
                        || step.getValue().trim().isEmpty()) continue;
                steps.add(new CustomQuickActionStep().setType(step.getType())
                        .setCondition(step.getCondition() == null
                                ? QuickActionStepCondition.ALWAYS : step.getCondition())
                        .setValue(step.getValue().trim()));
            }
        }
        List<CustomQuickActionMenuItem> menuItems = new ArrayList<>();
        if (definition.getContextMenuItems() != null) {
            for (CustomQuickActionMenuItem item : definition.getContextMenuItems()) {
                CustomQuickActionMenuItem normalized = normalize(item);
                if (normalized != null) menuItems.add(normalized);
            }
        }
        return result.setSteps(steps).setContextMenuItems(menuItems);
    }

    private static CustomQuickActionMenuItem normalize(CustomQuickActionMenuItem item) {
        if (item == null || item.getLabel() == null || item.getLabel().trim().isEmpty()) return null;
        List<CustomQuickActionStep> steps = new ArrayList<>();
        if (item.getSteps() != null) {
            for (CustomQuickActionStep step : item.getSteps()) {
                if (step == null || step.getType() == null || step.getValue() == null
                        || step.getValue().trim().isEmpty()) continue;
                steps.add(new CustomQuickActionStep().setType(step.getType())
                        .setCondition(step.getCondition() == null
                                ? QuickActionStepCondition.ALWAYS : step.getCondition())
                        .setValue(step.getValue().trim()));
            }
        }
        return new CustomQuickActionMenuItem()
                .setLabel(item.getLabel().trim())
                .setCloseBeforeExecution(item.isCloseBeforeExecution())
                .setExecutionMode(item.getExecutionMode() == null
                        ? QuickActionExecutionMode.PARALLEL : item.getExecutionMode())
                .setSteps(steps);
    }

    private static String safeId(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    static String nextDefinitionId() {
        return UUID.randomUUID().toString();
    }

    private static Path configFile() {
        return CustomConfig.getConfigDirectory().resolve(FILE_NAME);
    }
}
