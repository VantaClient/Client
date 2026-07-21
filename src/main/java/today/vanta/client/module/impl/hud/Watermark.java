package today.vanta.client.module.impl.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.client.Strings;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;
import java.util.Calendar;
import java.util.Formatter;

public class Watermark extends Module {
    private static final GlyphFontRenderer HN_MEDIUM_24 = CFonts.getFont("HN-Medium", 24);
    private static final GlyphFontRenderer HN_REGULAR_48 = CFonts.getFont("HN-Regular", 48);
    private static final GlyphFontRenderer SFPT_SEMIBOLD_42 = CFonts.getFont("SFPT-Semibold", 42);
    private static final Color BACKGROUND = new Color(20, 20, 20, 190);

    private final StringSetting
            style = Setting.of("Style", "Vanta", "Vanta", "Jello", "Char", "Exhi", "Adjust" , "Vestige"),
            fontMode = Setting.of("Font mode", "Exhi", "Exhi", "Minecraft", "SFPT").hide(() -> !style.isValue("Exhi"));

    public Watermark() {
        super("Watermark", "Draws a watermark of the client.", Category.HUD);
        hideFromArraylist = true;
        setEnabled(true);
    }

    @EventListen(priority = EventPriority.LOWEST)
    private void onRenderOverlay(RenderOverlayEvent event) {
        String firstChar = String.valueOf(Strings.CLIENT_NAME.charAt(0));
        float firstCharWidth = SFPT_SEMIBOLD_42.getStringWidth(firstChar) - 1;
        String watermarkText = Strings.CLIENT_NAME.substring(1);

        Color[] colors = Vanta.instance.moduleStorage.getT(Theme.class).colors;

        float x = 5;
        float y = 5;

        switch (style.getValue()) {
            case "Vanta":
                SFPT_SEMIBOLD_42.drawStringWithShadow(Strings.CLIENT_NAME, x, y, colors[0]);
                CFonts.SFPT_MEDIUM_18.drawStringWithShadow(Strings.CLIENT_VERSION, x, y + 18 + 3, Color.WHITE);
                break;

            case "Jello":
                HN_REGULAR_48.drawString(Strings.CLIENT_NAME, x, y, new Color(255, 255, 255, 185));
                HN_MEDIUM_24.drawString("Jello", x, y + HN_REGULAR_48.getFontHeight() - 1, new Color(255, 255, 255, 185));
                break;

            case "Char":
                SFPT_SEMIBOLD_42.drawStringWithShadow(firstChar, x, y, colors[0]);
                SFPT_SEMIBOLD_42.drawStringWithShadow(watermarkText, x + firstCharWidth, y, Color.WHITE);
                break;

            case "Exhi":
                Formatter format = new Formatter();
                Calendar gfg_calender = Calendar.getInstance();
                format.format("%tl:%tM %Tp", gfg_calender,
                        gfg_calender, gfg_calender);


                switch (fontMode.getValue()) {
                    case "Exhi":
                        mc.exhiFontRendererObj.drawString(firstChar, 2, 2, colors[0], true);
                        mc.exhiFontRendererObj.drawString(watermarkText + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + format + EnumChatFormatting.GRAY + "] " + "[" + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + " FPS" + EnumChatFormatting.GRAY + "]", 9, 2, Color.WHITE, true);
                        break;
                    case "Minecraft":
                        mc.fontRendererObj.drawString(firstChar, 2, 2, colors[0], true);
                        mc.fontRendererObj.drawString(watermarkText + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + format + EnumChatFormatting.GRAY + "] " + "[" + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + " FPS" + EnumChatFormatting.GRAY + "]", 8, 2, Color.WHITE, true);
                        break;
                    case "SFPT":
                        CFonts.SFPT_REGULAR_24.drawStringWithShadow(firstChar, 2, 2, colors[0]);
                        CFonts.SFPT_REGULAR_24.drawStringWithShadow(watermarkText + EnumChatFormatting.GRAY + " [" + EnumChatFormatting.WHITE + format + EnumChatFormatting.GRAY + "] " + "[" + EnumChatFormatting.WHITE + Minecraft.getDebugFPS() + " FPS" + EnumChatFormatting.GRAY + "]", 9, 2, Color.WHITE);
                        break;
                }
                break;

            case "Adjust":
                CFonts.getFont("T-Regular", 20).drawStringWithShadow("§r" + firstChar + "§f" + watermarkText, 2, 2, colors[0]);
                break;
            case "Vestige":
                float length = CFonts.SFPT_MEDIUM_24.getStringWidth(Strings.CLIENT_NAME + " v"+ Strings.CLIENT_VERSION);
                GradientRectangle
                        .create(2,2,length + 2,2)
                        .firstColor(colors[0])
                        .secondColor(colors[1])
                        .push(event);
                Rectangle
                        .create(2,4,length + 2,14)
                        .color(BACKGROUND)
                        .push(event);

                CFonts.SFPT_MEDIUM_24.drawString(Strings.CLIENT_NAME + " v"+ Strings.CLIENT_VERSION, 2,4,Color.WHITE,false);
                break;
        }
    }
}