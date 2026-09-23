package today.vanta.client.screen;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClickGUI;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.client.Strings;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.Icons;
import today.vanta.util.game.render.font.impl.MsdfFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.ColorUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DropDownGUI extends VantaScreen {
    private Color[] colors;
    private Color white;
    private final Color BACKGROUND = new Color(20,20,20,190);


    private float panelWidth = 115;
    private int count = 0;
    private float categoryRectHeight = 13f;
    private float moduleRectHeight = 12f;

    private int modulePadding = 2;
    private int spaceBetween = 5;

    private float x = width / 2;
    private float y = 50;


    private boolean hasLeftClicked = false, hasRightClicked;

    private ArrayList<Module> expandedModules = new ArrayList<>();

    private String mHoveredDescription = null;

    private NumberSetting draggingSlider;
    private static MsdfFontRenderer bigFont = CFonts.getFont("almarai-bold", 20);
    private static final MsdfFontRenderer font = CFonts.getFont("almarai-regular", 18);
    private static final MsdfFontRenderer smallFont = CFonts.getFont("almarai-regular", 14);
    private static final MsdfFontRenderer ICONS_16 = CFonts.getFont("Icons", 16, Icons.CHARS);


    @Override
    protected void initScreen() {
        ChatUtil.send(ChatUtil.Prefix.INFO,"press 'H' to reset ClickGUI mode because this one isnt finished :)");
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        // This is for the future if I plan on adding fading
        white = Color.white;
        colors = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors;
        if (Keyboard.isKeyDown(Keyboard.KEY_H)) {
            Vanta.instance.moduleStorage.getT(ClickGUI.class).design.setValue("Dropdown");
        }

        if (!Mouse.isButtonDown(1)) {
            hasRightClicked = false;
        }
        float drawX = x + spaceBetween + 190;
        count = 0;
        for (Category category : Category.values()) {
            count++;
            boolean cHovered = RenderUtil.hovered(event.mouseX,event.mouseY,drawX,y,panelWidth,categoryRectHeight);
            GradientRectangle.create(drawX,y,panelWidth,categoryRectHeight).firstColor(colors[0]).secondColor(colors[0].darker()).gradientMode(GradientMode.VERTICAL).push(event);
            float fullLength = drawX + (panelWidth / 2);
            float iconWidth = ICONS_16.getStringWidth(category.icon + "") - 5;
            float textWidth = bigFont.getStringWidth(category.name);
            float gap = 7f; // whatever spacing you actually want

            float combinedWidth = iconWidth + gap + textWidth;
            float startX = fullLength - combinedWidth / 2;

            float iconX = startX;
            float textX = startX + iconWidth + gap;

            ICONS_16.drawStringWithShadow(category.icon + "", iconX, y + (categoryRectHeight / 2) - ((float) ICONS_16.getFontHeight() / 2), white);
            bigFont.drawStringWithShadow(category.name, textX, y + (categoryRectHeight / 2) - ((float) bigFont.getFontHeight() / 2), white);
            float settingHeight = 0;
            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                if (module.isExpanded()) {
                    for (Setting setting : module.settings) {
                        settingHeight += getSettingHeight(setting);
                    }
                }
            }
            float mY = y + categoryRectHeight;
            float mX = drawX;
            for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(category)) {
                Rectangle.create(mX,mY + 1,panelWidth,moduleRectHeight + 2).color(BACKGROUND).push(event);
                GradientRectangle.create(mX + modulePadding,mY + modulePadding,panelWidth - (modulePadding * 2),moduleRectHeight)
                        .firstColor(BACKGROUND)
                        .secondColor(module.isEnabled() ? colors[0] : ColorUtil.getDarker(BACKGROUND,2))
                        .gradientMode(GradientMode.HORIZONTAL)
                        .push(event);
                font.drawStringWithShadow(module.name,mX + modulePadding + 1,mY + (moduleRectHeight / 2) - ((float) font.getFontHeight() / 2) + (modulePadding / 2),white);
                boolean mHovered = RenderUtil.hovered(event.mouseX,event.mouseY,mX,mY,panelWidth - (modulePadding * 2),moduleRectHeight);
                if (mHovered) {
                    if (Mouse.isButtonDown(1) && !hasRightClicked) {
                        module.setExpanded(!module.isExpanded());
                        hasRightClicked = true;
                    }
                    mHoveredDescription = module.description;
                    if (Objects.equals(mHoveredDescription, module.description)) {
                        mHoveredDescription = null;
                    } else {
                    }
                }
                float sY = mY + moduleRectHeight + modulePadding;
                if (module.isExpanded()) {
                    Rectangle.create(mX,sY,panelWidth - (modulePadding * 2),settingHeight + 1).color(BACKGROUND).push(event);
                    for (Setting setting : module.settings) {
                        renderSetting(setting,event.mouseX,event.mouseY,mX + modulePadding,sY + 3,panelWidth - (modulePadding * 2),event);
                        sY += getSettingHeight(setting);
                    }
                }
                mY += (moduleRectHeight) + modulePadding + (module.isExpanded() ? settingHeight + 1 : 0);
            }
            drawX += panelWidth + spaceBetween;
            renderDescription(event.mouseX,event.mouseY,event);
        }
    }

    private void renderDescription(float mouseX,float mouseY, Renderable renderable) {
        if (mHoveredDescription == null) return;
        Rectangle.create(mouseX - 10,mouseY + 10,smallFont.getStringWidth(mHoveredDescription) + 2,font.getFontHeight() + 2).color(BACKGROUND).push(renderable);
        smallFont.drawStringWithShadow(mHoveredDescription,mouseX - 10 + modulePadding,mouseY + 10 + (font.getFontHeight() + (float) 2 / 2) - ((float) font.getFontHeight()),white);
    }

    private void renderSetting(Setting setting, float mouseX, float mouseY, float x, float y, float width, Renderable renderable) {
        float textOffset = 2.3f;
        int padding = 1;
        if (setting instanceof BooleanSetting) {
            int booleanSize = 7;
            float outlineMinusThing = 1f;
            float toggleX = x + width - 2 - booleanSize - outlineMinusThing;
            boolean hover = RenderUtil.hovered(mouseX,mouseY,toggleX,y - outlineMinusThing,booleanSize,booleanSize);
            if (hover && !hasLeftClicked) {
                if (Mouse.isButtonDown(0)) {
                    setting.setValue(!((BooleanSetting) setting).getValue().booleanValue());
                    hasLeftClicked = true;
                }
            }
            smallFont.drawStringWithShadow(setting.name,x + padding,y - textOffset,Color.white);
            Rectangle.create(toggleX,y - outlineMinusThing,booleanSize,booleanSize).color(new Color(50,50,50,255)).push(renderable);
            Rectangle
                    .create(toggleX + outlineMinusThing ,y,booleanSize - (outlineMinusThing * 2),booleanSize - (outlineMinusThing * 2)).color(setting.getValue().equals(true) ? colors[0] : Color.black).push(renderable);
        }
    }

    private float getSettingHeight(Setting setting) {
        float val = 0;
        int padding = 2;
        if (setting instanceof BooleanSetting) {
            val += 7 + padding;
        }
        return val;
    }


    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingSlider = null;
        hasLeftClicked = false;
    }
}
