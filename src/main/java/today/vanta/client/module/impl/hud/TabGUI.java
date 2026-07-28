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
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.storage.impl.ModuleStorage;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// half of this module is made by Claude (Claude made most of the functions, and I made all of the UI/Design and rendering)
public class TabGUI extends Module {
    private static final Comparator<Category> COMP =
        Comparator.comparingInt((Category c) -> CFonts.SFPT_REGULAR_18.getStringWidth(c.name)).reversed();

    private static final GlyphFontRenderer SFPT_REGULAR_22 = CFonts.getFont("SFPT-Regular", 22);
    private static final GlyphFontRenderer SFPT_REGULAR_20 = CFonts.getFont("SFPT-Regular", 20);

    private static final float ROW_HEIGHT = 12f;
    private static final float WIDTH = 70f;
    private static final float catWIDTH = 100f;

    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000),
            opacity = Setting.of("Background opacity", 190,10,255);

    private final StringSetting gradMode = Setting.of(
            "Selection mode",
            "Horizontal gradient",
            "Horizontal gradient", "Vertical gradient", "Darker"
    );

    private final Category[] categories = Arrays.stream(Category.values())
            .sorted(COMP)
            .toArray(Category[]::new);

    private int selectedIndex = 0;
    private int selectedModuleIndex = 0;
    private boolean hasUp;
    private boolean hasDown;
    private boolean hasExpanded;
    private boolean reset;
    private boolean isExpanded;
    private boolean dragging;
    private float dragX, dragY;
    private float height = categories.length * ROW_HEIGHT;

    public TabGUI() {
        super("TabGUI", "Tabbin' the categories an modules.", Category.HUD);
        displayNames = new String[] {"TabGUI", "TabGui"};
    }
    private void handleDragging(float mouseX, float mouseY) {
        if (Mouse.isButtonDown(0)) {
            if (!dragging && RenderUtil.hovered(mouseX, mouseY, x.getValue().floatValue(), y.getValue().floatValue(), WIDTH, height)) {
                dragging = true;
                dragX = mouseX - x.getValue().floatValue();
                dragY = mouseY - y.getValue().floatValue();
            }

            if (dragging) {
                x.setValue(mouseX - dragX);
                y.setValue(mouseY - dragY);
            }
        } else {
            dragging = false;
        }
    }


    public Category getSelectedCategory() {
        return categories[selectedIndex];
    }

    public float getRowY(Category category) {
        int idx = indexOf(category);
        return y.getValue().floatValue() + idx * ROW_HEIGHT;
    }

    private int indexOf(Category category) {
        for (int i = 0; i < categories.length; i++) {
            if (categories[i] == category) return i;
        }
        return 0;
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        ModuleStorage moduleStorage = Vanta.instance.moduleStorage;
        float xPos = x.getValue().floatValue();
        float yPos = y.getValue().floatValue();
        height = categories.length * ROW_HEIGHT;

        Color BG = new Color(20,20,20,opacity.getValue().intValue());
        Color darkerBG = new Color(70,70,70,255);

        int maxIndex = categories.length - 1;
        if (selectedIndex > maxIndex) selectedIndex = maxIndex;
        if (selectedIndex < 0) selectedIndex = 0;


        // Category panel
        Rectangle
                .create(xPos, yPos, WIDTH, height)
                .color(BG)
                .push(event);

        switch (gradMode.getValue()) {
            case "Horizontal gradient":
                GradientRectangle
                        .create(xPos, getRowY(getSelectedCategory()), WIDTH, ROW_HEIGHT)
                        .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                        .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                        .gradientMode(GradientMode.HORIZONTAL)
                        .push(event);
                break;
            case "Vertical gradient":
                GradientRectangle
                        .create(xPos, getRowY(getSelectedCategory()), WIDTH, ROW_HEIGHT)
                        .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                        .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                        .gradientMode(GradientMode.VERTICAL)
                        .push(event);
                break;
            case "Darker":
                Rectangle
                        .create(xPos, getRowY(getSelectedCategory()), WIDTH, ROW_HEIGHT)
                        .color(darkerBG)
                        .push(event);
                break;

        }

        float yDraw = yPos;
        for (Category category : categories) {
            SFPT_REGULAR_22.drawStringWithShadow(
                    category.name,
                    xPos + 0.5f,
                    yDraw - 1,
                    Color.WHITE
            );
            yDraw += ROW_HEIGHT;
        }

        if (isExpanded) {
            List<Module> currentModules = moduleStorage.getModulesByCategory(getSelectedCategory());

            if (!currentModules.isEmpty()) {
                int moduleMaxIndex = currentModules.size() - 1;
                if (selectedModuleIndex > moduleMaxIndex) selectedModuleIndex = moduleMaxIndex;
                if (selectedModuleIndex < 0) selectedModuleIndex = 0;

                float drawX = xPos + WIDTH + 2;
                float drawY = yPos;
                float categoryHeight = ROW_HEIGHT * currentModules.size() + 1;

                Rectangle
                        .create(drawX, drawY, catWIDTH, categoryHeight)
                        .color(BG)
                        .push(event);

                switch (gradMode.getValue()) {
                    case "Horizontal gradient":
                        GradientRectangle
                                .create(drawX, drawY + selectedModuleIndex * ROW_HEIGHT, catWIDTH, ROW_HEIGHT)
                                .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                                .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                                .gradientMode(GradientMode.HORIZONTAL)
                                .push(event);
                        break;
                    case "Vertical gradient":
                        GradientRectangle
                                .create(drawX, drawY + selectedModuleIndex * ROW_HEIGHT, catWIDTH, ROW_HEIGHT)
                                .firstColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[1])
                                .secondColor(Vanta.instance.moduleStorage.getT(Theme.class).colors[0])
                                .gradientMode(GradientMode.VERTICAL)
                                .push(event);
                        break;
                    case "Darker":
                        Rectangle
                                .create(drawX, drawY + selectedModuleIndex * ROW_HEIGHT, catWIDTH, ROW_HEIGHT)
                                .color(darkerBG)
                                .push(event);
                        break;

                }

                for (int i = 0; i < currentModules.size(); i++) {
                    Module module = currentModules.get(i);
                    SFPT_REGULAR_20.drawStringWithShadow(
                            module.name,
                            drawX + 0.5f,
                            drawY - 1,
                                    (module.isEnabled() ? Vanta.instance.moduleStorage.getT(Theme.class).colors[0] : Color.WHITE)
                    );
                    drawY += ROW_HEIGHT;
                }
            }

        }

        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            if (!reset) {
                if (!isExpanded) {
                    isExpanded = true;
                    hasExpanded = true;
                }
                if (isExpanded && !hasExpanded) {
                    Module module = moduleStorage.getModulesByCategory(getSelectedCategory()).get(selectedModuleIndex);
                    if (module.isEnabled()) {
                        module.setEnabled(false);
                    } else {
                        module.setEnabled(true);
                    }
                }
                reset = true;
            }
        } else {
            reset = false;
            hasExpanded = false;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) && isExpanded) {
            isExpanded = false;
        }


        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && !hasDown) {
            if (isExpanded) {
                int size = moduleStorage.getModulesByCategory(getSelectedCategory()).size();
                if (size > 0) selectedModuleIndex = (selectedModuleIndex + 1) % size;
            } else {
                selectedIndex = (selectedIndex + 1) % categories.length;
                selectedModuleIndex = 0; // reset so a fresh category always starts at the top
            }
            hasDown = true;
        }
        if (!Keyboard.isKeyDown(Keyboard.KEY_DOWN)) hasDown = false;

        if (Keyboard.isKeyDown(Keyboard.KEY_UP) && !hasUp) {
            if (isExpanded) {
                int size = moduleStorage.getModulesByCategory(getSelectedCategory()).size();
                if (size > 0) selectedModuleIndex = (selectedModuleIndex - 1 + size) % size;
            } else {
                selectedIndex = (selectedIndex - 1 + categories.length) % categories.length;
                selectedModuleIndex = 0;
            }
            hasUp = true;
        }
        if (!Keyboard.isKeyDown(Keyboard.KEY_UP)) hasUp = false;

    }

    @EventListen
    private void onRenderScreen(RenderScreenEvent event) {
        if (mc.currentScreen instanceof GuiChat) {
            handleDragging(event.mouseX,event.mouseY);
        }
    }

    @Override
    public void onEnable() {
        selectedIndex = 0;
        selectedModuleIndex = 0;
    }
}