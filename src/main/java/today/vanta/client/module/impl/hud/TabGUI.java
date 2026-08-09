package today.vanta.client.module.impl.hud;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.storage.impl.ModuleStorage;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TabGUI extends Module {
    private static final Comparator<Category> CATEGORY_COMPARATOR =
            Comparator.comparingInt((final Category category) -> CFonts.SFPT_REGULAR_18.getStringWidth(category.name)).reversed();

    private static final GlyphFontRenderer SFPT_REGULAR_20 = CFonts.getFont("SFPT-Regular", 20);
    private static final GlyphFontRenderer SFPT_REGULAR_18 = CFonts.getFont("SFPT-Regular", 18);

    private static final float VANTA_ROW_HEIGHT = 12.0F;
    private static final float VANTA_WIDTH = 70.0F;
    private static final float VANTA_MODULE_WIDTH = 100.0F;

    private static final float ADJUST_ELEMENT_HEIGHT = 14.0F;
    private static final float ADJUST_PANEL_PADDING = 5.0F;
    private static final float ADJUST_CATEGORY_WIDTH = 80.0F;
    private static final float ADJUST_MODULE_WIDTH = 126.0F;
    private static final float ADJUST_SETTINGS_WIDTH = 210.0F;
    private static final float ADJUST_PANEL_GAP = 4.0F;
    private static final float ADJUST_ANIMATION_SPEED = 0.22F;
    private static final float ADJUST_ANIMATION_SNAP = 0.5F;

    private static final Color ADJUST_PANEL_COLOR = new Color(0xFF111111, true);
    private static final Color ADJUST_CATEGORY_COLOR = new Color(0xFFA8A7A7, true);
    private static final Color ADJUST_SELECTED_TEXT_COLOR = new Color(0xFFDBD4D5, true);
    private static final Color ADJUST_ENABLED_COLOR = Color.WHITE;
    private static final Color ADJUST_DISABLED_COLOR = new Color(0xFF808080, true);
    private static final Color ADJUST_LOCKED_COLOR = new Color(0xFF80FF80, true);

    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000),
            opacity = Setting.of("Background opacity", 190, 10, 255);

    private final StringSetting mode = Setting.of("Mode", "Vanta", "Vanta", "Adjust");
    private final NumberSetting adjustScale = Setting.of("Scale", 1.0, 0.5, 2.0, 2)
            .hide(() -> !mode.isValue("Adjust"));
    private final StringSetting selectionMode = Setting.of(
            "Selection mode",
            "Horizontal gradient",
            "Horizontal gradient", "Vertical gradient", "Darker"
    ).hide(() -> mode.isValue("Adjust"));

    private final Category[] vantaCategories = Arrays.stream(Category.values())
            .sorted(CATEGORY_COMPARATOR)
            .toArray(Category[]::new);
    private final Category[] adjustCategories = Category.values();
    private final int[] adjustCursorItems = new int[3];

    private int vantaSelectedCategoryIndex, vantaSelectedModuleIndex;
    private boolean vantaUpPressed, vantaDownPressed, vantaLeftPressed, vantaActivationPressed;
    private boolean vantaExpanded;

    private int adjustCursorDepth;
    private boolean adjustLockedInSlider;
    private boolean adjustUpPressed, adjustDownPressed, adjustLeftPressed, adjustRightPressed, adjustEnterPressed;
    private boolean adjustAnimationInitialized;
    private float animatedCategoryY, animatedModulePanelY, animatedModuleSelectionY;
    private float animatedSettingsPanelY, animatedSettingsSelectionY;
    private float animatedModulePanelWidth, animatedSettingsPanelWidth;
    private float[] categoryAlphaStates = new float[0];
    private float[] moduleAlphaStates = new float[0];
    private float[] settingAlphaStates = new float[0];
    private long adjustLastRenderTime;

    private boolean dragging;
    private float dragX, dragY, height;

    public TabGUI() {
        super("TabGUI", "Tabbin' the categories an modules.", Category.HUD);
        displayNames = new String[] { "TabGUI", "TabGui" };
        mode.addListener((setting, oldValue, newValue) -> {
            releaseInputStates();
            adjustAnimationInitialized = false;
            adjustLastRenderTime = 0L;
        });
    }

    @EventListen
    private void onRenderOverlay(final RenderOverlayEvent event) {
        if (mode.isValue("Adjust"))
            renderAdjustMode(event);
        else
            renderVantaMode(event);
    }

    @EventListen
    private void onRenderScreen(final RenderScreenEvent event) {
        if (mc.currentScreen instanceof GuiChat)
            handleDragging(event.mouseX, event.mouseY);
    }

    @Override
    public void onEnable() {
        vantaSelectedCategoryIndex = 0;
        vantaSelectedModuleIndex = 0;
        vantaExpanded = false;
        Arrays.fill(adjustCursorItems, 0);
        adjustCursorDepth = 0;
        adjustLockedInSlider = false;
        adjustAnimationInitialized = false;
        animatedModulePanelWidth = 0.0F;
        animatedSettingsPanelWidth = 0.0F;
        categoryAlphaStates = new float[0];
        moduleAlphaStates = new float[0];
        settingAlphaStates = new float[0];
        adjustLastRenderTime = 0L;
        releaseInputStates();
    }

    private void renderVantaMode(final Renderable renderable) {
        final ModuleStorage moduleStorage = Vanta.instance.moduleStorage;
        final float xPosition = x.getValue().floatValue();
        final float yPosition = y.getValue().floatValue();
        height = vantaCategories.length * VANTA_ROW_HEIGHT;

        vantaSelectedCategoryIndex = Math.max(
                0,
                Math.min(vantaSelectedCategoryIndex, vantaCategories.length - 1)
        );
        final List<Module> currentModules = moduleStorage.getModulesByCategory(
                vantaCategories[vantaSelectedCategoryIndex]
        );
        if (currentModules.isEmpty())
            vantaSelectedModuleIndex = 0;
        else
            vantaSelectedModuleIndex = Math.max(
                    0,
                    Math.min(vantaSelectedModuleIndex, currentModules.size() - 1)
            );

        final Color background = new Color(20, 20, 20, opacity.getValue().intValue());
        Rectangle
                .create(xPosition, yPosition, VANTA_WIDTH, height)
                .color(background)
                .push(renderable);
        renderVantaSelection(
                renderable,
                xPosition,
                yPosition + vantaSelectedCategoryIndex * VANTA_ROW_HEIGHT,
                VANTA_WIDTH
        );

        float categoryY = yPosition;
        for (final Category category : vantaCategories) {
            SFPT_REGULAR_18.drawStringWithShadow(category.name, xPosition + 2, categoryY + 0.5f, Color.WHITE);
            categoryY += VANTA_ROW_HEIGHT;
        }

        if (vantaExpanded && !currentModules.isEmpty()) {
            final float moduleX = xPosition + VANTA_WIDTH + 2.0F;
            Rectangle
                    .create(moduleX, yPosition, VANTA_MODULE_WIDTH, VANTA_ROW_HEIGHT * currentModules.size())
                    .color(background)
                    .push(renderable);

            renderVantaSelection(
                    renderable,
                    moduleX,
                    yPosition + vantaSelectedModuleIndex * VANTA_ROW_HEIGHT,
                    VANTA_MODULE_WIDTH
            );

            float moduleY = yPosition;
            for (final Module module : currentModules) {
                SFPT_REGULAR_18.drawStringWithShadow(
                        module.name,
                        moduleX + 2,
                        moduleY,
                        module.isEnabled() ? Vanta.instance.moduleStorage.getT(Theme.class).colors[0] : Color.WHITE
                );
                moduleY += VANTA_ROW_HEIGHT;
            }
        }

        handleVantaInput(currentModules);
    }

    private void renderVantaSelection(
            final Renderable renderable,
            final float xPosition,
            final float yPosition,
            final float width
    ) {
        if (selectionMode.isValue("Horizontal gradient")) {
            GradientRectangle
                    .create(xPosition, yPosition, width, VANTA_ROW_HEIGHT)
                    .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                    .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                    .gradientMode(GradientMode.HORIZONTAL)
                    .push(renderable);
        } else if (selectionMode.isValue("Vertical gradient")) {
            GradientRectangle
                    .create(xPosition, yPosition, width, VANTA_ROW_HEIGHT)
                    .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                    .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                    .gradientMode(GradientMode.VERTICAL)
                    .push(renderable);
        } else
            Rectangle.create(xPosition, yPosition, width, VANTA_ROW_HEIGHT).color(new Color(70, 70, 70)).push(renderable);
    }

    private void handleVantaInput(final List<Module> currentModules) {
        final boolean activationDown = Keyboard.isKeyDown(Keyboard.KEY_RIGHT)
                || Keyboard.isKeyDown(Keyboard.KEY_RETURN);
        if (activationDown && !vantaActivationPressed) {
            if (!vantaExpanded && !currentModules.isEmpty()) {
                vantaExpanded = true;
                vantaSelectedModuleIndex = 0;
            } else if (vantaExpanded && !currentModules.isEmpty())
                currentModules.get(vantaSelectedModuleIndex).setEnabled(
                        !currentModules.get(vantaSelectedModuleIndex).isEnabled()
                );
        }
        vantaActivationPressed = activationDown;

        final boolean leftDown = Keyboard.isKeyDown(Keyboard.KEY_LEFT);
        if (leftDown && !vantaLeftPressed && vantaExpanded)
            vantaExpanded = false;
        vantaLeftPressed = leftDown;

        final boolean downDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN);
        if (downDown && !vantaDownPressed) {
            if (vantaExpanded && !currentModules.isEmpty())
                vantaSelectedModuleIndex = (vantaSelectedModuleIndex + 1) % currentModules.size();
            else if (!vantaExpanded) {
                vantaSelectedCategoryIndex = (vantaSelectedCategoryIndex + 1) % vantaCategories.length;
                vantaSelectedModuleIndex = 0;
            }
        }
        vantaDownPressed = downDown;

        final boolean upDown = Keyboard.isKeyDown(Keyboard.KEY_UP);
        if (upDown && !vantaUpPressed) {
            if (vantaExpanded && !currentModules.isEmpty())
                vantaSelectedModuleIndex =
                        (vantaSelectedModuleIndex - 1 + currentModules.size()) % currentModules.size();
            else if (!vantaExpanded) {
                vantaSelectedCategoryIndex =
                        (vantaSelectedCategoryIndex - 1 + vantaCategories.length) % vantaCategories.length;
                vantaSelectedModuleIndex = 0;
            }
        }
        vantaUpPressed = upDown;
    }

    private void renderAdjustMode(final Renderable renderable) {
        clampAdjustCursor();
        handleAdjustInput();
        clampAdjustCursor();

        final float scale = adjustScale.getValue().floatValue();
        final float startX = x.getValue().floatValue();
        final float startY = y.getValue().floatValue();
        final float elementHeight = ADJUST_ELEMENT_HEIGHT * scale;
        final float panelPadding = ADJUST_PANEL_PADDING * scale;
        final float categoryWidth = ADJUST_CATEGORY_WIDTH * scale;
        final float moduleWidth = ADJUST_MODULE_WIDTH * scale;
        final float settingsWidth = ADJUST_SETTINGS_WIDTH * scale;
        final float panelGap = ADJUST_PANEL_GAP * scale;
        final GlyphFontRenderer font = CFonts.getFont("T-Regular", 17.5F * scale);
        final Theme theme = Vanta.instance.moduleStorage.getT(Theme.class);
        final int selectedCategoryIndex = adjustCursorItems[0];

        height = getAdjustRenderHeight(scale);
        drawAdjustPanel(renderable, startX, startY, categoryWidth, adjustCategories.length * elementHeight);

        final float targetCategoryY = startY + selectedCategoryIndex * elementHeight;
        final float targetModulePanelY = targetCategoryY;
        float targetModuleSelectionY = targetModulePanelY;
        float targetSettingsPanelY = targetModulePanelY;
        float targetSettingsSelectionY = targetSettingsPanelY;
        if (adjustCursorDepth > 0) {
            targetModuleSelectionY = targetModulePanelY + adjustCursorItems[1] * elementHeight;
            targetSettingsPanelY = targetModuleSelectionY;
            if (adjustCursorDepth > 1)
                targetSettingsSelectionY = targetSettingsPanelY + adjustCursorItems[2] * elementHeight;
        }

        initializeAdjustAnimations(
                targetCategoryY,
                targetModulePanelY,
                targetModuleSelectionY,
                targetSettingsPanelY,
                targetSettingsSelectionY
        );
        final float frameScale = getAdjustFrameScale();
        animatedCategoryY = animateAdjust(animatedCategoryY, targetCategoryY, frameScale);
        animatedModulePanelY = animateAdjust(animatedModulePanelY, targetModulePanelY, frameScale);
        animatedModuleSelectionY = animateAdjust(animatedModuleSelectionY, targetModuleSelectionY, frameScale);
        animatedSettingsPanelY = animateAdjust(animatedSettingsPanelY, targetSettingsPanelY, frameScale);
        animatedSettingsSelectionY = animateAdjust(
                animatedSettingsSelectionY,
                targetSettingsSelectionY,
                frameScale
        );
        animatedModulePanelWidth = animateAdjust(
                animatedModulePanelWidth,
                adjustCursorDepth > 0 ? moduleWidth : 0.0F,
                frameScale
        );
        animatedSettingsPanelWidth = animateAdjust(
                animatedSettingsPanelWidth,
                adjustCursorDepth > 1 ? settingsWidth : 0.0F,
                frameScale
        );

        drawAdjustSelection(
                renderable,
                startX,
                animatedCategoryY,
                categoryWidth,
                elementHeight,
                theme
        );

        categoryAlphaStates = ensureAlphaStates(categoryAlphaStates, adjustCategories.length, 146.0F);
        for (int index = 0; index < adjustCategories.length; index++) {
            final boolean selected = index == selectedCategoryIndex;

            categoryAlphaStates[index] = animateAdjust(
                    categoryAlphaStates[index],
                    selected ? 255.0F : 146.0F,
                    frameScale
            );

            font.drawStringWithShadow(
                    adjustCategories[index].name,
                    startX + panelPadding - 2,
                    startY + index * elementHeight + 1 * scale,
                    withAlpha(
                            selected ? ADJUST_SELECTED_TEXT_COLOR : ADJUST_CATEGORY_COLOR,
                            categoryAlphaStates[index]
                    )
            );
        }

        if (animatedModulePanelWidth <= 0.5F) return;

        final List<Module> modules = getAdjustModules();
        final int moduleCount = modules.size();
        final float moduleX = startX + categoryWidth + panelGap;
        final float moduleY = animatedModulePanelY;
        final float currentModuleWidth = Math.abs(animatedModulePanelWidth - moduleWidth) <= ADJUST_ANIMATION_SNAP
                ? moduleWidth
                : animatedModulePanelWidth;
        final float moduleHeight = Math.max(elementHeight, moduleCount * elementHeight);
        drawAdjustPanel(renderable, moduleX, moduleY, currentModuleWidth, moduleHeight);

        final int selectedModuleIndex = moduleCount > 0
                ? Math.max(0, Math.min(adjustCursorItems[1], moduleCount - 1))
                : 0;
        moduleAlphaStates = ensureAlphaStates(moduleAlphaStates, moduleCount, 128.0F);
        RenderUtil.scissor(moduleX - 0.5F, moduleY - 0.5F, currentModuleWidth + 1.0F, moduleHeight + 1.0F, () -> {
            for (int index = 0; index < moduleCount; index++) {
                final Module module = modules.get(index);
                final boolean selected = index == selectedModuleIndex;

                moduleAlphaStates[index] = animateAdjust(
                        moduleAlphaStates[index],
                        selected ? 255.0F : 128.0F,
                        frameScale
                );

                if (selected && currentModuleWidth > 1.0F)
                    drawAdjustSelection(
                            renderable,
                            moduleX,
                            animatedModuleSelectionY,
                            currentModuleWidth,
                            elementHeight,
                            theme
                    );

                final Color moduleColor = module.isEnabled() ? ADJUST_ENABLED_COLOR : ADJUST_DISABLED_COLOR;
                font.drawStringWithShadow(
                        module.name,
                        moduleX + panelPadding - 2,
                        moduleY + index * elementHeight + 1 * scale,
                        withAlpha(
                                selected && module.isEnabled() ? ADJUST_SELECTED_TEXT_COLOR : moduleColor,
                                moduleAlphaStates[index]
                        )
                );
            }
        });

        if (animatedSettingsPanelWidth <= 0.5F || moduleCount == 0) return;

        final List<Setting<?>> settings = getVisibleSettings(modules.get(selectedModuleIndex));
        final int settingsCount = settings.size();
        final float settingsX = moduleX + currentModuleWidth + panelGap;
        final float settingsY = animatedSettingsPanelY;
        final float currentSettingsWidth =
                Math.abs(animatedSettingsPanelWidth - settingsWidth) <= ADJUST_ANIMATION_SNAP
                        ? settingsWidth
                        : animatedSettingsPanelWidth;
        final float settingsHeight = Math.max(elementHeight, settingsCount * elementHeight);
        drawAdjustPanel(renderable, settingsX, settingsY, currentSettingsWidth, settingsHeight);

        final int selectedSettingIndex = settingsCount > 0
                ? Math.max(0, Math.min(adjustCursorItems[2], settingsCount - 1))
                : 0;
        settingAlphaStates = ensureAlphaStates(settingAlphaStates, settingsCount, 128.0F);
        RenderUtil.scissor(
                settingsX - 0.5F,
                settingsY - 0.5F,
                currentSettingsWidth + 1.0F,
                settingsHeight + 1.0F,
                () -> {
                    for (int index = 0; index < settingsCount; index++) {
                        final boolean selected = index == selectedSettingIndex;
                        settingAlphaStates[index] = animateAdjust(
                                settingAlphaStates[index],
                                selected ? 255.0F : 128.0F,
                                frameScale
                        );
                        if (selected && currentSettingsWidth > 1.0F)
                            drawAdjustSelection(
                                    renderable,
                                    settingsX,
                                    animatedSettingsSelectionY,
                                    currentSettingsWidth,
                                    elementHeight,
                                    theme
                            );

                        font.drawStringWithShadow(
                                settings.get(index).name + ": " + getSettingDisplayString(settings.get(index)),
                                settingsX + panelPadding,
                                settingsY + index * elementHeight + 2.0F * scale,
                                withAlpha(
                                        selected && adjustLockedInSlider
                                                ? ADJUST_LOCKED_COLOR
                                                : selected ? ADJUST_SELECTED_TEXT_COLOR : ADJUST_DISABLED_COLOR,
                                        settingAlphaStates[index]
                                )
                        );
                    }
                }
        );
    }

    private void drawAdjustPanel(
            final Renderable renderable,
            final float xPosition,
            final float yPosition,
            final float width,
            final float panelHeight
    ) {
        Rectangle.create(xPosition, yPosition, width, panelHeight).color(ADJUST_PANEL_COLOR).push(renderable);
    }

    private void drawAdjustSelection(
            final Renderable renderable,
            final float xPosition,
            final float yPosition,
            final float width,
            final float selectionHeight,
            final Theme theme
    ) {
        Rectangle
                .create(xPosition, yPosition - 0.5F, width, selectionHeight + 1.0F)
                .color(theme.colors[1])
                .push(renderable);
        GradientRectangle
                .create(xPosition, yPosition - 0.5F, width, selectionHeight + 1.0F)
                .firstColor(theme.colors[0])
                .secondColor(theme.colors[1])
                .gradientMode(GradientMode.HORIZONTAL)
                .push(renderable);
    }

    private void handleAdjustInput() {
        final boolean rightDown = Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
        if (rightDown && !adjustRightPressed) {
            adjustCursorDepth++;
            clampAdjustCursor();
            wrapAdjustCursor();
        }
        adjustRightPressed = rightDown;

        final boolean leftDown = Keyboard.isKeyDown(Keyboard.KEY_LEFT);
        if (leftDown && !adjustLeftPressed) {
            if (adjustLockedInSlider)
                adjustLockedInSlider = false;
            adjustCursorDepth--;
            clampAdjustCursor();
        }
        adjustLeftPressed = leftDown;

        final boolean downDown = Keyboard.isKeyDown(Keyboard.KEY_DOWN);
        if (downDown && !adjustDownPressed) {
            if (adjustLockedInSlider)
                adjustLockedNumber(-1);
            else {
                adjustCursorItems[adjustCursorDepth]++;
                wrapAdjustCursor();
            }
        }
        adjustDownPressed = downDown;

        final boolean upDown = Keyboard.isKeyDown(Keyboard.KEY_UP);
        if (upDown && !adjustUpPressed) {
            if (adjustLockedInSlider)
                adjustLockedNumber(1);
            else {
                adjustCursorItems[adjustCursorDepth]--;
                wrapAdjustCursor();
            }
        }
        adjustUpPressed = upDown;

        final boolean enterDown = Keyboard.isKeyDown(Keyboard.KEY_RETURN);
        if (enterDown && !adjustEnterPressed)
            activateAdjustItem();
        adjustEnterPressed = enterDown;
    }

    private void activateAdjustItem() {
        final List<Module> modules = getAdjustModules();
        if (adjustCursorDepth == 1 && !modules.isEmpty()) {
            final Module module = modules.get(adjustCursorItems[1]);
            module.setEnabled(!module.isEnabled());
            return;
        }
        if (adjustCursorDepth != 2 || modules.isEmpty()) return;

        final List<Setting<?>> settings = getVisibleSettings(modules.get(adjustCursorItems[1]));
        if (settings.isEmpty()) return;

        final Setting<?> selectedSetting = settings.get(adjustCursorItems[2]);
        if (selectedSetting instanceof BooleanSetting) {
            final BooleanSetting booleanSetting = (BooleanSetting) selectedSetting;
            booleanSetting.setValue(!booleanSetting.getValue());
        } else if (selectedSetting instanceof StringSetting)
            ((StringSetting) selectedSetting).next();
        else if (selectedSetting instanceof NumberSetting)
            adjustLockedInSlider = !adjustLockedInSlider;
    }

    private void adjustLockedNumber(final int direction) {
        final List<Module> modules = getAdjustModules();
        if (modules.isEmpty()) return;

        final List<Setting<?>> settings = getVisibleSettings(modules.get(adjustCursorItems[1]));
        if (settings.isEmpty() || !(settings.get(adjustCursorItems[2]) instanceof NumberSetting)) {
            adjustLockedInSlider = false;
            return;
        }

        final NumberSetting numberSetting = (NumberSetting) settings.get(adjustCursorItems[2]);
        final Number currentValue = numberSetting.getValue();
        final double increment = Math.pow(10.0D, -numberSetting.places);
        final double factor = Math.pow(10.0D, numberSetting.places);
        final double changedValue = Math.round(
                Math.max(
                        numberSetting.min.doubleValue(),
                        Math.min(
                                numberSetting.max.doubleValue(),
                                currentValue.doubleValue() + increment * direction
                        )
                ) * factor
        ) / factor;

        if (currentValue instanceof Integer)
            numberSetting.setValue((int) changedValue);
        else if (currentValue instanceof Float)
            numberSetting.setValue((float) changedValue);
        else if (currentValue instanceof Long)
            numberSetting.setValue((long) changedValue);
        else if (currentValue instanceof Short)
            numberSetting.setValue((short) changedValue);
        else if (currentValue instanceof Byte)
            numberSetting.setValue((byte) changedValue);
        else
            numberSetting.setValue(changedValue);
    }

    private void wrapAdjustCursor() {
        final int itemCount;
        if (adjustCursorDepth == 0)
            itemCount = adjustCategories.length;
        else if (adjustCursorDepth == 1)
            itemCount = getAdjustModules().size();
        else {
            final List<Module> modules = getAdjustModules();
            itemCount = modules.isEmpty()
                    ? 0
                    : getVisibleSettings(modules.get(adjustCursorItems[1])).size();
        }

        if (itemCount == 0) {
            adjustCursorItems[adjustCursorDepth] = 0;
            return;
        }
        if (adjustCursorItems[adjustCursorDepth] >= itemCount)
            adjustCursorItems[adjustCursorDepth] = 0;
        else if (adjustCursorItems[adjustCursorDepth] < 0)
            adjustCursorItems[adjustCursorDepth] = itemCount - 1;
    }

    private void clampAdjustCursor() {
        adjustCursorDepth = Math.max(0, Math.min(adjustCursorDepth, 2));
        adjustCursorItems[0] = Math.max(0, Math.min(adjustCursorItems[0], adjustCategories.length - 1));

        final List<Module> modules = getAdjustModules();
        if (modules.isEmpty()) {
            adjustCursorItems[1] = 0;
            adjustCursorItems[2] = 0;
            if (adjustCursorDepth == 2)
                adjustCursorDepth = 1;
            adjustLockedInSlider = false;
            return;
        }

        adjustCursorItems[1] = Math.max(0, Math.min(adjustCursorItems[1], modules.size() - 1));
        final List<Setting<?>> settings = getVisibleSettings(modules.get(adjustCursorItems[1]));
        if (settings.isEmpty()) {
            adjustCursorItems[2] = 0;
            if (adjustCursorDepth == 2)
                adjustCursorDepth = 1;
            adjustLockedInSlider = false;
        } else
            adjustCursorItems[2] = Math.max(0, Math.min(adjustCursorItems[2], settings.size() - 1));

        if (adjustLockedInSlider
                && (adjustCursorDepth != 2 || !(settings.get(adjustCursorItems[2]) instanceof NumberSetting)))
            adjustLockedInSlider = false;
    }

    private List<Module> getAdjustModules() {
        return Vanta.instance.moduleStorage.getModulesByCategory(adjustCategories[adjustCursorItems[0]]);
    }

    private List<Setting<?>> getVisibleSettings(final Module module) {
        return module.settings.stream()
                .filter(setting -> !setting.isHidden())
                .collect(Collectors.toList());
    }

    private String getSettingDisplayString(final Setting<?> setting) {
        if (setting instanceof NumberSetting) {
            final NumberSetting numberSetting = (NumberSetting) setting;
            return String.format(
                    Locale.ROOT,
                    "%." + numberSetting.places + "f%s",
                    numberSetting.getValue().doubleValue(),
                    numberSetting.suffix
            );
        }
        if (setting instanceof MultiStringSetting)
            return String.join(", ", ((MultiStringSetting) setting).getValue());
        return String.valueOf(setting.getValue());
    }

    private void initializeAdjustAnimations(
            final float categoryY,
            final float modulePanelY,
            final float moduleSelectionY,
            final float settingsPanelY,
            final float settingsSelectionY
    ) {
        if (adjustAnimationInitialized) return;

        animatedCategoryY = categoryY;
        animatedModulePanelY = modulePanelY;
        animatedModuleSelectionY = moduleSelectionY;
        animatedSettingsPanelY = settingsPanelY;
        animatedSettingsSelectionY = settingsSelectionY;
        adjustAnimationInitialized = true;
    }

    private float getAdjustFrameScale() {
        final long currentTime = System.currentTimeMillis();
        final float frameScale = adjustLastRenderTime == 0L
                ? 1.0F
                : Math.max(0.25F, Math.min(3.0F, (currentTime - adjustLastRenderTime) / 1000.0F * 60.0F));
        adjustLastRenderTime = currentTime;
        return frameScale;
    }

    private float animateAdjust(final float current, final float target, final float frameScale) {
        final float difference = target - current;
        if (Math.abs(difference) <= ADJUST_ANIMATION_SNAP)
            return target;

        return current + difference * (
                1.0F - (float) Math.pow(1.0F - ADJUST_ANIMATION_SPEED, frameScale)
        );
    }

    private float[] ensureAlphaStates(final float[] states, final int size, final float defaultAlpha) {
        if (states.length == size) return states;

        final float[] nextStates = new float[size];
        for (int index = 0; index < size; index++)
            nextStates[index] = index < states.length ? states[index] : defaultAlpha;
        return nextStates;
    }

    private Color withAlpha(final Color color, final float alpha) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.max(0, Math.min(Math.round(alpha), 255))
        );
    }

    private float getAdjustRenderWidth(final float scale) {
        float width = ADJUST_CATEGORY_WIDTH * scale;
        if (adjustCursorDepth > 0)
            width += (ADJUST_PANEL_GAP + ADJUST_MODULE_WIDTH) * scale;
        if (adjustCursorDepth > 1)
            width += (ADJUST_PANEL_GAP + ADJUST_SETTINGS_WIDTH) * scale;
        return width;
    }

    private float getAdjustRenderHeight(final float scale) {
        final float elementHeight = ADJUST_ELEMENT_HEIGHT * scale;
        float renderHeight = adjustCategories.length * elementHeight;
        if (adjustCursorDepth == 0) return renderHeight;

        final List<Module> modules = getAdjustModules();
        renderHeight = Math.max(
                renderHeight,
                adjustCursorItems[0] * elementHeight + Math.max(elementHeight, modules.size() * elementHeight)
        );
        if (adjustCursorDepth > 1 && !modules.isEmpty())
            renderHeight = Math.max(
                    renderHeight,
                    adjustCursorItems[0] * elementHeight
                            + adjustCursorItems[1] * elementHeight
                            + Math.max(
                                    elementHeight,
                                    getVisibleSettings(modules.get(adjustCursorItems[1])).size() * elementHeight
                            )
            );
        return renderHeight;
    }

    private void handleDragging(final float mouseX, final float mouseY) {
        if (!Mouse.isButtonDown(0)) {
            dragging = false;
            return;
        }

        final float width = mode.isValue("Adjust")
                ? getAdjustRenderWidth(adjustScale.getValue().floatValue())
                : VANTA_WIDTH;
        if (!dragging && RenderUtil.hovered(
                mouseX,
                mouseY,
                x.getValue().floatValue(),
                y.getValue().floatValue(),
                width,
                height
        )) {
            dragging = true;
            dragX = mouseX - x.getValue().floatValue();
            dragY = mouseY - y.getValue().floatValue();
        }

        if (dragging) {
            x.setValue(mouseX - dragX);
            y.setValue(mouseY - dragY);
        }
    }

    private void releaseInputStates() {
        vantaUpPressed = false;
        vantaDownPressed = false;
        vantaLeftPressed = false;
        vantaActivationPressed = false;
        adjustUpPressed = false;
        adjustDownPressed = false;
        adjustLeftPressed = false;
        adjustRightPressed = false;
        adjustEnterPressed = false;
    }
}
