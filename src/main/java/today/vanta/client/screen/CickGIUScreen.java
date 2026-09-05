package today.vanta.client.screen;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
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

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CickGIUScreen extends VantaScreen {
    private Color color1, color2;
    private static final float cButtonHeight = 12;
    private static final float cButtonWidth = 47;

    private static final MsdfFontRenderer ICONS_16 = CFonts.getFont("Icons", 16, Icons.CHARS);
    private static final MsdfFontRenderer ICONS_12 = CFonts.getFont("Icons", 12, Icons.CHARS);

    public float sWidth = 409, sHeight = 275;
    private final float mButtonWidth = sWidth - 4, mButtonHeight = 22;
    public float x = -999, y = -999;

    private boolean hasLeftClicked = false, hasRightClicked;

    private Category currentCategory = Category.COMBAT;
    private ArrayList<Module> expandedModules = new ArrayList<>();

    @Override
    protected void initScreen() {
        if (x == -999 || y == -999) {
            x = width / 2f - sWidth / 2;
            y = height / 2f - sHeight / 2;
        }
        ChatUtil.send(ChatUtil.Prefix.INFO,"press 'H' to reset ClickGUI mode because this one isnt finished :)");
    }

    @EventListen
    private void onRender(RenderScreenEvent event) {
        color1 = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        color2 = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[1];
        if (!Mouse.isButtonDown(0)) {
            hasLeftClicked = false;
        }
        if (!Mouse.isButtonDown(1)) {
            hasRightClicked = false;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_H)) {
            Vanta.instance.moduleStorage.getT(ClickGUI.class).design.setValue("Dropdown");
        }
            RenderUtil.scissor(x, y, sWidth, sHeight, () -> {
                Rectangle.create(x, y, sWidth, sHeight)
                        .color(new Color(30, 30, 30, 225))
                        .push(event);
                CFonts.SFPT_REGULAR_24.drawHorizontalGradientString(Strings.CLIENT_NAME, x + 2, y + 1, Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0], Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[1], 1, 1);
                float CStringLength = CFonts.SFPT_REGULAR_24.getStringWidth(Strings.CLIENT_NAME + " ");
                CFonts.SFPT_REGULAR_24.drawStringWithShadow(EnumChatFormatting.GRAY + "v" + Strings.CLIENT_VERSION, x + 2 + CStringLength, y + 1, Color.white);
                float xDraw = x + 4 + CFonts.SFPT_REGULAR_24.getStringWidth(EnumChatFormatting.GRAY + "v" + Strings.CLIENT_VERSION) + CStringLength + 2;
                for (Category category : Category.values()) {
                    boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, xDraw, y + 2, cButtonWidth, cButtonHeight);
                    float cTextLength = CFonts.SFPT_REGULAR_18.getStringWidth(category.name);
                    float cTextHeight = CFonts.SFPT_REGULAR_18.getFontHeight();
                    Rectangle.create(xDraw, y + 2, cButtonWidth, cButtonHeight).color(hover ? new Color(40, 40, 40, 190) : new Color(20, 20, 20, 190)).push(event);
                    if (category == currentCategory) {
                        GradientRectangle.create(xDraw,y + 2,cButtonWidth,1).firstColor(color1.darker()).secondColor(color1).push(event);
                    }
                    CFonts.SFPT_REGULAR_18.drawStringWithShadow(category.name, xDraw + (cButtonWidth / 2) - (cTextLength / 2), y + 2 + (cButtonHeight / 2) - (cTextHeight / 2), Color.white);
                    if (hover && !hasLeftClicked) {
                        if (Mouse.isButtonDown(0)) {
                            currentCategory = category;
                            ChatUtil.send(ChatUtil.Prefix.INFO, category.name);
                            hasLeftClicked = true;
                        }
                    }
                    xDraw += cButtonWidth + 2;
                }
                GradientRectangle.create(x, y + 16, sWidth, 1).gradientMode(GradientMode.HORIZONTAL).firstColor(color1).secondColor(color2).push(event);
                RenderUtil.scissor(x, y + 17, sWidth, sHeight - 17, () -> {
                    float mY = y + 19;
                    for (Module module : Vanta.instance.moduleStorage.getModulesByCategory(currentCategory)) {
                        float totalSettingHeight = 0;
                        float hoverHeight = mY + mButtonHeight > y + sHeight ? mY + mButtonHeight - (y + sHeight) : mButtonHeight;
                        boolean hover = RenderUtil.hovered(event.mouseX, event.mouseY, x + 2, mY, mButtonWidth, hoverHeight);
                        if (hover && !hasLeftClicked) {
                            if (Mouse.isButtonDown(0)) {
                                module.setEnabled(!module.isEnabled());
                                hasLeftClicked = true;
                            }
                        }
                        if (hover && !hasRightClicked) {
                            if (Mouse.isButtonDown(1)) {
                                hasRightClicked = true;
                                if (expandedModules.contains(module)) {
                                    expandedModules.remove(module);
                                } else {
                                    expandedModules.add(module);
                                }
                            }
                        }
                        Rectangle.create(x + 2, mY, mButtonWidth, mButtonHeight).color(hover ? new Color(40, 40, 40, 190) : new Color(20, 20, 20, 190)).push(event);
                        CFonts.getFont("SFPT-Regular", 18).drawStringWithShadow(module.name, x + 3, mY + 1, module.isEnabled() ? color1 : Color.white);
                        CFonts.getFont("SFPT-Regular", 16).drawStringWithShadow(EnumChatFormatting.GRAY + module.description, x + 3, mY + 11, Color.white);
                        if (expandedModules.contains(module)) {
                            Rectangle
                                    .create(x + 2,mY + 21,mButtonWidth,1).color(color1).push(event);
                            for (Setting setting : module.settings.stream().filter(m -> !m.isHidden()).collect(Collectors.toList())) {
                                totalSettingHeight += getSettingHeight(setting);
                            }
                            Rectangle.create(x + 2,mY + 22,mButtonWidth,totalSettingHeight).color(new Color(20, 20, 20, 190)).push(event);
                            float sY = mY + 25;
                            for (Setting setting : module.settings.stream().filter(m -> !m.isHidden()).collect(Collectors.toList())) {
                                renderSetting(setting,event.mouseX,event.mouseY,x + 4,sY,sWidth,event);
                                sY += getSettingHeight(setting);
                            }
                        }

                        mY += mButtonHeight + 2 + totalSettingHeight;
                    }
                });
            });
    }

    private void renderSetting(Setting setting, float mouseX, float mouseY, float x, float y, float width, Renderable renderable) {
        MsdfFontRenderer font = CFonts.getFont("SFPT-Regular", 16);
        float textOffset = 2.3f;
        if (setting instanceof BooleanSetting) {
            int booleanSize = 7;
            float outlineMinusThing = 0.5f;
            boolean hover = RenderUtil.hovered(mouseX,mouseY,x + width - 8 - booleanSize - outlineMinusThing,y - outlineMinusThing,booleanSize,booleanSize);
            if (hover && !hasLeftClicked) {
                if (Mouse.isButtonDown(0)) {
                    setting.setValue(!((BooleanSetting) setting).getValue().booleanValue());
                    hasLeftClicked = true;
                }
            }
            font.drawStringWithShadow(setting.name,x,y - textOffset,Color.white);
            Rectangle.create(x + width - 8 - booleanSize - outlineMinusThing,y - outlineMinusThing,booleanSize,booleanSize).color(new Color(50,50,50,255)).push(renderable);
            Rectangle
                    .create(x + width - 8 - booleanSize,y,booleanSize - 1,booleanSize - 1).color(setting.getValue().equals(true) ? color1 : Color.black).push(renderable);
        }

        if (setting instanceof NumberSetting) {
            float sliderWidth = width - 6;
            float sliderHeight = 2;
            float sliderProgress = sliderWidth * (((NumberSetting) setting).getValue().floatValue() / ((NumberSetting) setting).max.floatValue());
            float sliderPointerHeight = 4;
            float sliderPointerWidth = 3;
            font.drawStringWithShadow(setting.name,x,y,Color.white);
            font.drawStringWithShadow(String.valueOf(setting.getValue()),x + sliderWidth - font.getStringWidth(String.valueOf(((NumberSetting) setting).getValue().doubleValue())),y,Color.white);
            Rectangle.create(x,y + 11,sliderWidth,sliderHeight).color(new Color(50,50,50,255)).push(renderable);
            GradientRectangle.create(x,y + 11,sliderProgress,sliderHeight).firstColor(color1).secondColor(color1.darker()).gradientMode(GradientMode.VERTICAL).push(renderable);
            Rectangle.create(x + sliderProgress,y + 10f,sliderPointerWidth,sliderPointerHeight).color(Color.white).push(renderable);
        }

        if (setting instanceof StringSetting) {
            int padding = 1;
            // WHY DO I HAVE TO DO IT LIKE THIS FUCKSAKE
            font.drawStringWithShadow(setting.name,x,y - textOffset, Color.white);
            font.drawStringWithShadow( ((StringSetting) setting).expanded ? "- " : "+ " +  ((StringSetting) setting).getValue(),x + width - 8 - font.getStringWidth(((StringSetting) setting).expanded ? "- " : "+ " +  ((StringSetting) setting).getValue()),y - textOffset,Color.white);
            boolean hover = RenderUtil.hovered(mouseX,mouseY,x,y,x + width - 8 - font.getStringWidth(((StringSetting) setting).getValue()),font.getFontHeight());
            if (hover && !hasRightClicked) {
                if (Mouse.isButtonDown(1)) {
                    ((StringSetting) setting).expanded = !((StringSetting) setting).expanded;
                    hasRightClicked = true;
                }
            }
            if (((StringSetting) setting).expanded) {
                float Sy = y += font.getFontHeight();
                for (String s : Arrays.stream(((StringSetting) setting).allValues).collect(Collectors.toList())) {
                    boolean hovered = RenderUtil.hovered(mouseX,mouseY,x + width - 8 - font.getStringWidth(s),Sy - textOffset,font.getStringWidth(s),font.getFontHeight());
                    font.drawStringWithShadow(s,x + width - 8 - font.getStringWidth(s),Sy - textOffset,hovered ? color2 : Color.white);
                    if (hovered && !hasLeftClicked) {
                        if (Mouse.isButtonDown(0)) {
                            ((StringSetting) setting).setValue(s);
                            ((StringSetting) setting).expanded = false;
                            hasLeftClicked = true;
                        }
                    }
                    Sy += font.getFontHeight();
                }
            }
        }
    }

    private float getSettingHeight(Setting setting) {
        MsdfFontRenderer font = CFonts.getFont("SFPT-Regular", 16);
        float val = 0;
        if (setting instanceof NumberSetting) {
            val += 17;
        }
        if (setting instanceof BooleanSetting) {
            val += 11;
        }
        if (setting instanceof StringSetting) {
            if (((StringSetting) setting).expanded) {
                val += font.getFontHeight() * (Arrays.stream(((StringSetting) setting).allValues).count() + 1);
            } else {
                val += 11;
            }
        }
        return val;
    }


    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
       // empty
    }
}