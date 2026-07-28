package today.vanta.client.module.impl.hud;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.PlayerUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.font.impl.GlyphFontRenderer;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;

public class TargetHUD extends Module {
    private static final Color BACKGROUND = new Color(20, 20, 20, 200);
    private static final Color ATBACKGROUND = new Color(20, 20, 20, 150);
    private static final Color ATDARKERBACKGROUND = new Color(20, 20, 20, 200);
    private static final Color DARKER_BACKGROUND = new Color(20, 20, 20, 255);
    private static final Color PASSBACKGROUND = new Color(182, 215, 223);

    private static final GlyphFontRenderer RUSTICROADWAY_22 = CFonts.getFont("RusticRoadway", 22);
    private static final GlyphFontRenderer OCRB_18 = CFonts.getFont("OCR-B", 18);
    private static final GlyphFontRenderer OCRB_10 = CFonts.getFont("OCR-B", 10);
    private static final GlyphFontRenderer OCRB_8 = CFonts.getFont("OCR-B", 8);
    private static final GlyphFontRenderer SFPT_MEDIUM_20 = CFonts.getFont("SFPT-Medium", 20);
    private static final GlyphFontRenderer SFPT_REGULAR_16 = CFonts.getFont("SFPT-Regular", 16);

    private EntityLivingBase localTarget;
    private String oldTarget;
    private boolean can;

    private float width = 130;
    private float height = 40;

    private boolean dragging;
    private float dragX, dragY;

    private final StringSetting mode = Setting.of("Mode", "Vanta", "Classic", "Vanta", "Adjust", "ID-Card", "Aged", "Novoline", "Old Atmosphere", "Atmosphere");
    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000);

    public TargetHUD() {
        super("TargetHUD", "Target information.", Category.HUD);
        displayNames = new String[]{"TargetHUD", "TargetHud", "TargetInfo"};
        hideFromArraylist = true;
    }

    @EventListen
    private void onRenderScreen(RenderScreenEvent event) {
        if (mc.thePlayer == null) return;

        if (!(mc.currentScreen instanceof GuiChat) && TargetProcessor.getInstance().target == null) {
            return;
        }

        localTarget = null;

        if (TargetProcessor.getInstance().target == null && mc.currentScreen instanceof GuiChat) {
            localTarget = mc.thePlayer;
        } else if (TargetProcessor.getInstance().target instanceof EntityPlayer) {
            localTarget = TargetProcessor.getInstance().target;
        }

        if (localTarget == null) return;

        if (mc.currentScreen instanceof GuiChat) {
            handleDragging(event.mouseX, event.mouseY);
        } else if (mc.currentScreen != null) {
            return;
        }

        draw(event);
    }

    private void handleDragging(float mouseX, float mouseY) {
        if (Mouse.isButtonDown(0)) {
            if (!dragging && RenderUtil.hovered(mouseX, mouseY, x.getValue().floatValue(), y.getValue().floatValue(), width, height)) {
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

    private float barWidth = width - 26f - 6f;
    private float ghostBarWidth = width - 26f - 6f;
    private float targetWidth = width - 26f - 6f;
    private float targetWidth2 = width - 26f - 6f;
    private float adtargetWidth = width - 4f;
    private float adtargetWidth2 = width - 4f;
    private float adghostBarWidth = width - 4f;
    private float adbarWidth = width - 4f;
    private float abarwidth = width - 37;
    private float atargetwidth = width - 37;
    private float atTargetWidth = width - 36f;
    private float atghostBarWidth = width - 36f;
    private float atTargetWidth2 = width - 36f;
    private float atbarWidth = width - 36f;

    private Animation animation;
    private Animation ghostAnimation;

    private void draw(Renderable renderable) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();

        Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        Color color2 = Vanta.instance.moduleStorage.getT(Theme.class).colors[1];

        switch (mode.getValue()) {
            case "Vanta":
                float headSize = 26f;

                width = 138;
                height = 30;

                Rectangle
                        .create(x, y, width, height)
                        .color(new Color(28, 29, 33))
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 2, y - (headSize / 2) + (height / 2), headSize);
                SFPT_MEDIUM_20.drawStringWithShadow(localTarget.getName(), x + 3 + headSize, y + 1, Color.WHITE);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(String.format("%.1f", localTarget.getHealth()), x + 3 + headSize, y + 11, Color.WHITE);

                float barrwidth = width - 26f - 6f;
                float healthWidth = barrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());
                float ghostWidth = barrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());

                if (healthWidth != targetWidth) {
                    targetWidth = healthWidth;
                    if (oldTarget != localTarget.getName()) {
                        if (animation != null) {
                            animation.stop();
                        }
                        barWidth = healthWidth;
                        can = true;
                        oldTarget = localTarget.getName();
                        return;
                    }

                    animation = Animation.create(
                            barWidth,
                            targetWidth,
                            100,
                            Easing.EASE_IN_OUT,
                            val -> barWidth = val
                    );

                    animation.start();
                }
                if (ghostWidth != targetWidth2) {
                    targetWidth2 = ghostWidth;

                    if (can) {
                        if (ghostAnimation != null) {
                            ghostAnimation.stop();
                        }
                        ghostBarWidth = ghostWidth;
                        oldTarget = localTarget.getName();
                        can = false;
                        return;
                    }

                    ghostAnimation = Animation.create(
                            ghostBarWidth,
                            targetWidth2,
                            450,
                            Easing.EASE_IN_OUT,
                            val -> ghostBarWidth = val
                    );

                    ghostAnimation.start();
                }

                Rectangle
                        .create(x + 4 + headSize, y + 22, barrwidth, 6f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 4 + headSize, y + 22, ghostBarWidth, 6f)
                        .color(color.darker())
                        .push(renderable);

                GradientRectangle
                        .create(x + 4 + headSize, y + 22, barWidth, 6f)
                        .firstColor(color.darker())
                        .secondColor(color)
                        .push(renderable);
                break;

            case "Classic":
                width = 150f;
                height = 55f;

                float healthbarwidth = 100f;
                float whatever = (localTarget.getHealth() / localTarget.getMaxHealth());
                float healthbar = healthbarwidth * whatever;

                String health_str = String.format("%.1f", localTarget.getHealth());
                Color healthbarcol = new Color(48, 246, 6);

                if (whatever < 0.5 && whatever > 0.33) {
                    healthbarcol = new Color(221, 244, 2);
                }

                if (whatever <= 0.33) {
                    healthbarcol = new Color(250, 42, 68);
                }

                Rectangle
                        .create(x, y, width, height)
                        .color(BACKGROUND)
                        .push(renderable);

                RenderUtil.renderEntity((int) x + 15, (int) y + 52, 25, -30, 0, localTarget);

                Rectangle
                        .create(x + 37, y - (10f / 2) + (height / 2) + 10, healthbarwidth, 10f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 37, y - (10f / 2) + (height / 2) + 10, healthbar, 10f)
                        .color(healthbarcol)
                        .push(renderable);

                mc.fontRendererObj.drawStringWithShadow(health_str + " ❤", x + 37, y + 16, Color.WHITE);
                mc.fontRendererObj.drawStringWithShadow(localTarget.getName(), x + 37, y + 2, Color.WHITE);
                break;
            case "Adjust":
                width = 100;
                height = 30;
                float barrrrwidth = width - 4f;

                float adhealthWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());
                float adghostWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());

                if (adhealthWidth != adtargetWidth) {
                    adtargetWidth = adhealthWidth;
                    if (oldTarget != localTarget.getName()) {
                        adbarWidth = adtargetWidth;
                        can = true;
                        oldTarget = localTarget.getName();
                        return;
                    }

                    animation = Animation.create(
                            adbarWidth,
                            adtargetWidth,
                            150,
                            Easing.LINEAR,
                            val -> adbarWidth = val
                    );

                    animation.start();
                }
                if (adghostWidth != adtargetWidth2) {
                    adtargetWidth2 = adghostWidth;

                    if (can) {
                        adghostBarWidth = adtargetWidth2;
                        oldTarget = localTarget.getName();
                        can = false;
                        return;
                    }

                    ghostAnimation = Animation.create(
                            adghostBarWidth,
                            adtargetWidth2,
                            450,
                            Easing.LINEAR,
                            val -> adghostBarWidth = val
                    );

                    ghostAnimation.start();
                }

                float space = 24.5f;
                float length = CFonts.getFont("T-Regular", 14).getStringWidth(String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth()));

                Rectangle
                        .create(x, y, width, height)
                        .color(BACKGROUND)
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 2, y + 2, 20f);
                CFonts.getFont("T-Regular", 16).drawStringWithShadow(localTarget.getName(), x + 24, y + 1, Color.WHITE);

                Rectangle
                        .create(x + 2, y + space, width - 4, 3f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 2, y + space, adghostBarWidth, 3f)
                        .color(color.darker())
                        .push(renderable);

                Rectangle
                        .create(x + 2, y + space, adbarWidth, 3f)
                        .color(color)
                        .push(renderable);

                float itemX = x + 10 + 2;
                float itemY = y + 10;

                ItemStack currentItem = ((EntityPlayer) localTarget).inventory.getCurrentItem();
                if (currentItem != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(currentItem, itemX + 1, itemY + 1, 0.65f);
                }

                ItemStack slot3 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(3);
                if (slot3 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot3, itemX, itemY, 0.75f);
                }

                ItemStack slot2 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(2);
                if (slot2 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot2, itemX, itemY, 0.75f);
                }

                ItemStack slot1 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(1);
                if (slot1 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot1, itemX, itemY, 0.75f);
                }

                ItemStack slot0 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(0);
                if (slot0 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot0, itemX, itemY, 0.75f);
                }

                CFonts.getFont("T-Regular", 14).drawStringWithShadow(String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth()), x + width - (length) - 4, y + 15, Color.WHITE);
                break;

            case "ID-Card":
                width = 211;
                height = 100;
                String entitytype;
                if (PlayerUtil.checkIllegal(localTarget)) {
                    entitytype = "Bot";
                } else {
                    entitytype = "Player";
                }
                String firstChar = String.valueOf(localTarget.getName().charAt(0)).toUpperCase();
                Rectangle
                        .create(x, y, width, height)
                        .color(PASSBACKGROUND)
                        .push(renderable);

                OCRB_10.drawString("NORGE NOREG NORGA", x + 2, y + 2, Color.RED, false);
                OCRB_10.drawString("NORWAY", x + 2, y + 9, Color.RED, false);
                OCRB_10.drawString("ID-KORT ID-DUODASTUS", x + 118, y + 2, Color.RED, false);
                OCRB_10.drawString("IDENTITY CARD", x + 150, y + 9, Color.RED, false);
                OCRB_8.drawString("Etternavn/Etternamn/Sohkanamma/Surname", x + 71, y + 20, Color.RED, false);
                OCRB_18.drawString(entitytype.toUpperCase(), x + 71, y + 25, Color.BLACK, false);
                OCRB_8.drawString("Fornavn/Førenamn/Ovdanamma/Given Name", x + 71, y + 41, Color.RED, false);
                OCRB_18.drawString(localTarget.getName().toUpperCase(), x + 71, y + 47, Color.BLACK, false);
                OCRB_8.drawString("Kjønn/Sokhabeali/Sex", x + 71, y + 63, Color.RED, false);
                OCRB_18.drawString("MINECRAFT", x + 71, y + 69, Color.BLACK, false);
                RUSTICROADWAY_22.drawString(firstChar + ". " + entitytype, x + 71, y + 83, Color.BLACK, false);
                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 3, y + 22, 64);
                break;
            case "Aged":
                width = 150f;
                height = 35f;

                whatever = (localTarget.getHealth() / localTarget.getMaxHealth());
                healthbar = (width - 37) * whatever;

                if (healthbar != atargetwidth) {
                    atargetwidth = healthbar;
                    if (oldTarget != localTarget.getName()) {
                        abarwidth = healthbar;
                        oldTarget = localTarget.getName();
                        return;
                    }

                    animation = Animation.create(
                            abarwidth,
                            atargetwidth,
                            100,
                            Easing.LINEAR,
                            val -> abarwidth = val
                    );

                    animation.start();
                }

                color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
                color2 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0].darker();

                health_str = String.format("%.1f", localTarget.getHealth());
                String distance_str = String.format("%.1f", mc.thePlayer.getDistanceToEntity(localTarget));

                Rectangle
                        .create(x, y, width, height)
                        .color(BACKGROUND)
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 2, y + 2, 31);

                Rectangle
                        .create(x + 35, y - (10f / 2) + (height / 2) + 10, width - 37, 10f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                GradientRectangle
                        .create(x + 35, y - (10f / 2) + (height / 2) + 10, abarwidth, 10f)
                        .firstColor(color)
                        .secondColor(color2)
                        .gradientMode(GradientMode.VERTICAL)
                        .push(renderable);

                mc.exhiFontRendererObj.drawString(health_str, x + 35, y + 12, Color.WHITE);
                mc.exhiFontRendererObj.drawString(localTarget.getName(), x + 35, y + 2, Color.WHITE);
                mc.exhiFontRendererObj.drawString(distance_str, x + width - mc.exhiFontRendererObj.getStringWidth(distance_str) - 2, y + 2, Color.WHITE);
                break;

            case "Novoline":
                float targetNameWidth = mc.fontRendererObj.getStringWidth(localTarget.getName());
                width = Math.max(75, 75 + targetNameWidth);
                height = 42;
                float barW = width - 43;

                float healthRatio = (float) Math.max(0.0, Math.min(localTarget.getHealth(), localTarget.getMaxHealth()) / localTarget.getMaxHealth());
                float calcBarWidth = barW * healthRatio;

                Color backgroundColor1 = new Color(29, 29, 29, 255);
                Color backgroundColor2 = new Color(40, 40, 40, 255);

                Color healthColor = getHealthColor(healthRatio);

                Rectangle.create(x, y, width, height)
                        .color(backgroundColor1)
                        .push(renderable);

                Rectangle.create(x, y, width, height)
                        .color(backgroundColor2)
                        .push(renderable);

                mc.fontRendererObj.drawString(localTarget.getName(), x + 41, y + 4, Color.WHITE);

                Rectangle.create(x + 41, y + 15, barW, 9.0)
                        .color(backgroundColor1)
                        .push(renderable);

                Rectangle.create(x + 41, y + 15, calcBarWidth, 9.0)
                        .color(healthColor)
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 1, y + 1, 37);
                String healthText = String.format("%.1f", localTarget.getHealth());
                mc.fontRendererObj.drawString(healthText, x + 41, y + 29, Color.WHITE);

                int stringW = mc.fontRendererObj.getStringWidth(healthText);
                mc.fontRendererObj.drawString(" ❤", x + 41 + stringW, y + 28, healthColor);
                break;
            case "Old Atmosphere":
                width = 140;
                height = 48;
                barrrrwidth = width - 36f;
                float widthoutline = width - 34f;

                float athealthWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());
                float atghostWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());

                if (athealthWidth != atTargetWidth) {
                    atTargetWidth = athealthWidth;
                    if (oldTarget != localTarget.getName()) {
                        if (animation != null) {
                            animation.stop();
                        }
                        atbarWidth = athealthWidth;
                        can = true;
                        oldTarget = localTarget.getName();
                        return;
                    }

                    animation = Animation.create(
                            atbarWidth,
                            atTargetWidth,
                            150,
                            Easing.LINEAR,
                            val -> atbarWidth = val
                    );

                    animation.start();
                }
                if (atghostWidth != atTargetWidth2) {
                    atTargetWidth2 = atghostWidth;

                    if (can) {
                        if (ghostAnimation != null) {
                            ghostAnimation.stop();
                        }
                        atghostBarWidth = atghostWidth;
                        oldTarget = localTarget.getName();
                        can = false;
                        return;
                    }

                    ghostAnimation = Animation.create(
                            atghostBarWidth,
                            atTargetWidth2,
                            450,
                            Easing.LINEAR,
                            val -> atghostBarWidth = val
                    );

                    ghostAnimation.start();
                }

                space = 28f;
                length = SFPT_REGULAR_16.getStringWidth(String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth()));

                Rectangle
                        .create(x, y, width, height)
                        .color(ATBACKGROUND)
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 2, y + 2, 30f);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(localTarget.getName(), x + 33, y + 1, Color.WHITE);

                Rectangle
                        .create(x + 33, y + space - 1, widthoutline, 5f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 34, y + space, atghostBarWidth, 3f)
                        .color(color.darker())
                        .push(renderable);

                Rectangle
                        .create(x + 34, y + space, atbarWidth, 3f)
                        .color(color)
                        .push(renderable);

                itemX = x + 20 + 2;
                itemY = y + 12.5f;


                slot3 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(3);
                if (slot3 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot3, itemX, itemY, 0.8f);
                }

                slot2 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(2);
                if (slot2 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot2, itemX, itemY, 0.8f);
                }

                slot1 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(1);
                if (slot1 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot1, itemX, itemY, 0.8f);
                }

                slot0 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(0);
                if (slot0 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot0, itemX, itemY, 0.8f);
                }

                currentItem = ((EntityPlayer) localTarget).inventory.getCurrentItem();
                if (currentItem != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(currentItem, itemX + 1, itemY + 1, 0.8f);
                }


                Rectangle
                        .create(x + 1,y + 34,width - 2,12)
                        .color(ATDARKERBACKGROUND)
                        .push(renderable);

                String winratio = "";
                float healthper = (localTarget.getHealth() / localTarget.getMaxHealth()) * 100;
                String healthperStr = String.format("%.1f", healthper);
                float lengthh = SFPT_REGULAR_16.getStringWidth(healthperStr + "%");
                String ratiostr = String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth());
                float ratio = Float.parseFloat(ratiostr);
                if (ratio > 0 && ratio != 0) {
                    winratio = EnumChatFormatting.GRAY+"Winning"+ EnumChatFormatting.DARK_GRAY + " ("+ "+"+ratio+")";
                }
                if (ratio == 0) {
                    winratio = EnumChatFormatting.GRAY+"Tie"+ EnumChatFormatting.DARK_GRAY + " ("+ "+"+ratio+")";
                }
                if (ratio < 0) {
                    winratio = EnumChatFormatting.GRAY+"Losing"+ EnumChatFormatting.DARK_GRAY + " ("+ratio+")";
                }
                float stringheight = SFPT_REGULAR_16.getFontHeight();
                SFPT_REGULAR_16.drawString(winratio, x + 3, y + 34 + 6 - stringheight, Color.WHITE);
                SFPT_REGULAR_16.drawString(healthperStr + "%", x + width - lengthh - 4, y + 34 + 6 - stringheight, Color.WHITE);
                break;
            case "Atmosphere":
                width = 130;
                height = 34;
                barrrrwidth = width - 36f;
                widthoutline = width - 34f;

                athealthWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());
                atghostWidth = barrrrwidth * (localTarget.getHealth() / localTarget.getMaxHealth());

                if (athealthWidth != atTargetWidth) {
                    atTargetWidth = athealthWidth;
                    if (oldTarget != localTarget.getName()) {
                        if (animation != null) {
                            animation.stop();
                        }
                        atbarWidth = athealthWidth;
                        can = true;
                        oldTarget = localTarget.getName();
                        return;
                    }

                    animation = Animation.create(
                            atbarWidth,
                            atTargetWidth,
                            150,
                            Easing.LINEAR,
                            val -> atbarWidth = val
                    );

                    animation.start();
                }
                if (atghostWidth != atTargetWidth2) {
                    atTargetWidth2 = atghostWidth;

                    if (can) {
                        if (ghostAnimation != null) {
                            ghostAnimation.stop();
                        }
                        atghostBarWidth = atghostWidth;
                        oldTarget = localTarget.getName();
                        can = false;
                        return;
                    }

                    ghostAnimation = Animation.create(
                            atghostBarWidth,
                            atTargetWidth2,
                            450,
                            Easing.LINEAR,
                            val -> atghostBarWidth = val
                    );

                    ghostAnimation.start();
                }

                space = 28f;
                length = SFPT_REGULAR_16.getStringWidth(String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth()));

                Rectangle
                        .create(x, y, width, height)
                        .color(ATBACKGROUND)
                        .push(renderable);

                RenderUtil.renderHead(renderable, (EntityPlayer) localTarget, x + 2, y + 2, 30f);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(localTarget.getName(), x + 33, y + 1, Color.WHITE);

                Rectangle
                        .create(x + 33, y + space - 1, widthoutline, 5f)
                        .color(DARKER_BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 34, y + space, atghostBarWidth, 3f)
                        .color(color.darker())
                        .push(renderable);

                Rectangle
                        .create(x + 34, y + space, atbarWidth, 3f)
                        .color(color)
                        .push(renderable);

                itemX = x + 20 + 2;
                itemY = y + 12.5f;


                slot3 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(3);
                if (slot3 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot3, itemX, itemY, 0.8f);
                }

                slot2 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(2);
                if (slot2 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot2, itemX, itemY, 0.8f);
                }

                slot1 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(1);
                if (slot1 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot1, itemX, itemY, 0.8f);
                }

                slot0 = ((EntityPlayer) localTarget).inventory.armorItemInSlot(0);
                if (slot0 != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(slot0, itemX, itemY, 0.8f);
                }

                currentItem = ((EntityPlayer) localTarget).inventory.getCurrentItem();
                if (currentItem != null) {
                    itemX += 10 + 1;
                    RenderUtil.renderScaledItem(currentItem, itemX + 1, itemY + 1, 0.8f);

                }
                String healthdif = String.format("%.1f", mc.thePlayer.getHealth() - localTarget.getHealth());
                if (Float.parseFloat(healthdif) > 0) {
                    healthdif = "+"+healthdif;
                }
                length = SFPT_REGULAR_16.getStringWidth(healthdif);
                SFPT_REGULAR_16.drawStringWithShadow(healthdif, x + width - (length) - 2, y + 17, Color.WHITE);
                break;
        }

        if (dragging && mc.currentScreen instanceof GuiChat) {
            if (mode.isValue("Aged")) {
                GradientRectangle
                        .create(x - 0.5, y - 0.5, width + 1, height + 1)
                        .firstColor(color)
                        .secondColor(color2)
                        .outline(true)
                        .push(renderable);
            } else {
                Rectangle
                        .create(x - 0.5, y - 0.5, width + 1, height + 1)
                        .color(color)
                        .outline(true)
                        .push(renderable);
            }
        }
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }

    private Color getHealthColor(double ratio) {
        float hue = (float) (ratio * 360.0 / 3.0) / 360.0f;
        return Color.getHSBColor(hue, 1.0f, 0.5f);
    }
}