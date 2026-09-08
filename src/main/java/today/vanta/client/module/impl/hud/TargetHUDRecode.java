package today.vanta.client.module.impl.hud;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.module.impl.combat.KillAura;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;
import java.util.Objects;

public class TargetHUDRecode extends Module {
    private final StringSetting mode = Setting.of("Mode", "Vanta", "Vanta", "Adjust");
    private final BooleanSetting onlyPlayers = Setting.of("Allow only players", true);
    private final BooleanSetting useCustom = Setting.of("Use custom animation values", false);
    private final MultiStringSetting animationProp = Setting.of("Animation", new String[]{"Opacity", "Scale"}, new String[]{"Opacity", "Scale"}).hide(() -> !useCustom.getValue());
    private final NumberSetting durationVal = Setting.of("Animation duration", 250, 100, 450, 0, "ms").hide(() -> !useCustom.getValue());
    private final NumberSetting healthDur = Setting.of("Health bar duration",100,50,400,0,"ms").hide(() -> !useCustom.getValue());
    private final NumberSetting ghostDur = Setting.of("Ghost bar duration",200,100,450,0,"ms").hide(() -> !useCustom.getValue());
    private float x = 450;
    private float y = 500;
    private float width = 120;
    private float height = 38;
    private EntityLivingBase entity;
    private int healthDuration = 100;
    private int state;
    private int desiredState;
    private int ANIMATE_IN = 1;
    private int ANIMATE_OUT = 2;
    private int STANDBY = 3;
    private float entityHealth;
    private float entityMaxHealth;
    private String entityDisplayName;
    private String oldMode;
    private ResourceLocation entityLocationSkin;
    private String entityName;
    private int entityHurtTime = 0;
    private String oldTargetName;
    private float entityDistance;
    private ItemStack entityCurrentItem;
    private ItemStack entityArmorSlot1;
    private ItemStack entityArmorSlot2;
    private ItemStack entityArmorSlot3;
    private ItemStack entityArmorSlot4;
    private boolean entityIsPlayer;
    private boolean refreshed = false;
    private float animatedScale = 0;
    private float targetScale;
    private int duration = 250;
    private int ghostDuration = 350;
    private float barWidth = width - 4;
    private float barHeight = 3f;
    private float targetGhostBar;
    private float animatedGhostBar;
    private float targetBarWidth;
    private float animatedBarWidth;

    public TargetHUDRecode() {
        super("TargetHUDRecode", "TargetHUD module but recoded.", Category.HUD);
    }


    private void checkState() {
        if (!(mc.currentScreen instanceof GuiChat)) {
            if (TargetProcessor.getInstance().target == null) {
                if (mc.objectMouseOver.entityHit != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase && !Vanta.instance.moduleStorage.getT(KillAura.class).isEnabled()) {
                    entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
                } else {
                    entity = null;
                }
            } else {
                entity = TargetProcessor.getInstance().target;
            }
        } else {
            entity = mc.thePlayer;
        }
        boolean valid;
        if (onlyPlayers.getValue()) {
            valid = entity != null && entity instanceof EntityPlayer && !entity.isDead;
        } else {
            valid = entity != null && !entity.isDead;
        }

        if (valid) {
            desiredState = ANIMATE_IN;
            entityHealth = entity.getHealth();
            entityMaxHealth = entity.getMaxHealth();
            entityDisplayName = entity.getDisplayName().getFormattedText();
            entityName = entity.getName();
            entityDistance = mc.thePlayer.getDistanceToEntity(entity);
            entityHurtTime = entity.getHurtTime();
            if (entity instanceof EntityPlayer) {
                entityLocationSkin = ((AbstractClientPlayer) entity).getLocationSkin();
                entityCurrentItem = ((AbstractClientPlayer) entity).inventory.getCurrentItem();
                entityArmorSlot1 = ((AbstractClientPlayer) entity).inventory.armorItemInSlot(0);
                entityArmorSlot2 = ((AbstractClientPlayer) entity).inventory.armorItemInSlot(1);
                entityArmorSlot3 = ((AbstractClientPlayer) entity).inventory.armorItemInSlot(2);
                entityArmorSlot4 = ((AbstractClientPlayer) entity).inventory.armorItemInSlot(3);
                entityIsPlayer = true;
            } else {
                entityIsPlayer = false;
            }
            refreshed = true;
        } else {
            desiredState = ANIMATE_OUT;
        }
        if (desiredState != state) {
            runAnimation(desiredState);
            state = desiredState;
        }

    }

    private Animation inAnimation;
    private Animation outAnimation;

    private void runAnimation(int aState) {
        if (aState == ANIMATE_IN) {
            targetScale = 1;
            inAnimation = Animation.create(
                    animatedScale,
                    targetScale,
                    duration,
                    Easing.EASE_IN_OUT,
                    val -> animatedScale = val
            );
            if (outAnimation != null && outAnimation.started) {
                outAnimation.stop();
            }
            inAnimation.start();
        }

        if (aState == ANIMATE_OUT) {
            targetScale = 0;
            outAnimation = Animation.create(
                    animatedScale,
                    targetScale,
                    duration,
                    Easing.EASE_IN_OUT,
                    val -> animatedScale = val
            );
            if (inAnimation != null && inAnimation.started) {
                inAnimation.stop();
            }
            outAnimation.start();
        }
    }

    private int getAlpha(int alpha) {
        return (int) (alpha * animatedScale);
    }

    private Animation barAnimation;
    @EventListen
    private void onRenderOverlay(RenderOverlayEvent e) {
        checkState();
        ghostDuration = ghostDur.getValue().intValue();
        duration = durationVal.getValue().intValue();
        healthDuration = healthDur.getValue().intValue();
        Color color1 = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float headSize;
        float bar = barWidth * (entityHealth / entityMaxHealth);
        if (bar != targetBarWidth) {
            targetBarWidth = bar;
            if (oldTargetName != entityName || !Objects.equals(oldMode, mode.getValue())) {
                if (barAnimation != null) {
                    barAnimation.stop();
                }
                animatedBarWidth = bar;
//                can = true;
                oldTargetName = entityName;
                oldMode = mode.getValue();
                return;
            }

            barAnimation = Animation.create(
                    animatedBarWidth,
                    targetBarWidth,
                    healthDuration,
                    Easing.LINEAR,
                    val -> animatedBarWidth = val
            );

            barAnimation.start();
        }

        if (bar != targetGhostBar) {
            targetGhostBar = bar;
            if (oldTargetName != entityName || !Objects.equals(oldMode, mode.getValue())) {
                if (barAnimation != null) {
                    barAnimation.stop();
                }
                animatedGhostBar = bar;
//                can = true;
                oldTargetName = entityName;
                oldMode = mode.getValue();
                return;
            }

            barAnimation = Animation.create(
                    animatedGhostBar,
                    targetGhostBar,
                    ghostDuration,
                    Easing.LINEAR,
                    val -> animatedBarWidth = val
            );

            barAnimation.start();
        }
        GlStateManager.pushMatrix();
        if (!animationProp.isHidden() && animationProp.isEnabled("Scale")) {
            GlStateManager.translate(centerX, centerY, 0);
            GlStateManager.scale(animatedScale, animatedScale, 1);
            GlStateManager.translate(-centerX, -centerY, 0);
        }
        try {
            switch (mode.getValue()) {
                case "Vanta":
                    barWidth = width - 4;
                    width = 120;
                    height = 38;
                    Rectangle.create(x, y, width, height).color(new Color(20, 20, 20, getAlpha(190))).push(e);
                    if (entityIsPlayer) {
                        headSize = height - 7;
                        RenderUtil.renderHead(e, entityLocationSkin, x + 2, y + 2, headSize - 2, new Color(255, 255, 255, getAlpha(255)));
                    } else {
                        headSize = 0;
                    }
                    CFonts.SFPT_MEDIUM_18.drawStringWithShadow(entityName, x + 2 + headSize, y + 1, new Color(255, 255, 255, getAlpha(255)));
                    CFonts.SFPT_REGULAR_18.drawStringWithShadow("Health: " + String.format("%.1f", entityHealth), x + 2 + headSize, y + 11, new Color(255, 255, 255, getAlpha(255)));
                    CFonts.SFPT_REGULAR_18.drawStringWithShadow("Distance: " + String.format("%.1f", entityDistance), x + 2 + headSize, y + 21, new Color(255, 255, 255, getAlpha(150)));
                    Rectangle.create(x + 2, y + height - 5, barWidth,barHeight).color(new Color(20,20,20,getAlpha(255))).push(e);
                    GradientRectangle.create(x + 2, y + height - 5, MathHelper.clamp_float(animatedBarWidth,0,barWidth), barHeight).firstColor(new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), getAlpha(color.getAlpha()))).secondColor(new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), getAlpha(color.getAlpha())).darker()).gradientMode(GradientMode.VERTICAL).push(e);
                    break;
                case "Adjust":
                    barWidth = width - 4;
                    width = 100;
                    height = 30;
                    float space = 24.5f;
                    float length = CFonts.getFont("T-Regular", 14).getStringWidth(String.format("%.1f", mc.thePlayer.getHealth() - entityHealth));

                    Rectangle
                            .create(x, y, width, height)
                            .color(new Color(10,10,10,getAlpha(190)))
                            .push(e);

                    if (entityIsPlayer) {
                        RenderUtil.renderHead(
                                e,
                                entityLocationSkin,
                                x + 2,
                                y + 2,
                                20.0F,
                                getDamageHeadTint()
                        );
                    }

                    CFonts.getFont("T-Regular", 16).drawStringWithShadow(entityName, x + 23, y + 1, new Color(255,255,255,getAlpha(255)));

                    Rectangle
                            .create(x + 1.75f, y + space - 0.25f, barWidth + 0.5f, 3.75f)
                            .color(new Color(10,10,10, getAlpha(255)))
                            .push(e);

                    Rectangle
                            .create(x + 2, y + space, animatedGhostBar, 3f)
                            .color(new Color(color.getRed(),color.getBlue(),color.getGreen(),getAlpha(color.getAlpha())).darker())
                            .push(e);

                    Rectangle
                            .create(x + 2, y + space, animatedBarWidth, 3f)
                            .color(new Color(color.getRed(),color.getBlue(),color.getGreen(),getAlpha(color.getAlpha())))
                            .push(e);

                    float itemX = x + 10 + 2;
                    float itemY = y + 10;

//                    if (entityIsPlayer) {
//                        if (entityCurrentItem != null) {
//                            itemX += 10 + 1;
//                            RenderUtil.renderScaledItem(entityCurrentItem, itemX + 1, itemY + 1, 0.65f, new Color(color.getRed(),color.getBlue(),color.getGreen(),getAlpha(color.getAlpha())));
//                        }
//
//                        if (entityArmorSlot4 != null) {
//                            itemX += 10 + 1;
//                            RenderUtil.renderScaledItem(entityArmorSlot4, itemX, itemY, 0.75f);
//                        }
//
//                        if (entityArmorSlot3 != null) {
//                            itemX += 10 + 1;
//                            RenderUtil.renderScaledItem(entityArmorSlot3, itemX, itemY, 0.75f);
//                        }
//
//                        if (entityArmorSlot2 != null) {
//                            itemX += 10 + 1;
//                            RenderUtil.renderScaledItem(entityArmorSlot2, itemX, itemY, 0.75f);
//                        }
//
//                        if (entityArmorSlot1 != null) {
//                            itemX += 10 + 1;
//                            RenderUtil.renderScaledItem(entityArmorSlot1, itemX, itemY, 0.75f, new Color(255,255,255,getAlpha(255)));
//                        }
//                    }

                    CFonts.getFont("T-Regular", 14).drawStringWithShadow(String.format("%.1f", mc.thePlayer.getHealth() - entityHealth), x + width - (length) - 2, y + 15, new Color(255,255,255,getAlpha(255)));
                    break;
            }

        } catch (IllegalArgumentException error) {
            error.printStackTrace();
        }
        GlStateManager.popMatrix();

    }

    private Color getDamageHeadTint() {
        final float hurtProgress = Math.max(
                0.0F,
                Math.min(
                        entityHurtTime == 0
                                ? 0.0F
                                : (entityHurtTime - mc.timer.renderPartialTicks) * 0.3F,
                        1.0F
                )
        );
        final int greenBlue = (int) (255.0F + hurtProgress * (171.0F - 255.0F));
        return new Color(255, greenBlue, greenBlue, getAlpha(255));
    }
}
