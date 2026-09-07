package today.vanta.client.module.impl.hud;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
    private final StringSetting mode = Setting.of("Mode", "Vanta", "Vanta", "Cryptix");
    private final BooleanSetting onlyPlayers = Setting.of("Allow only players", true);
    private final NumberSetting durationVal = Setting.of("Animation duration", 250, 100, 450, 0, "ms");
    private float x = 450;
    private float y = 500;
    private float width = 120;
    private float height = 38;
    private EntityLivingBase entity;
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
    private String oldTargetName;
    private float entityDistance;
    private boolean entityIsPlayer;
    private boolean refreshed = false;
    private float animatedScale = 0;
    private float targetScale;
    private int duration = 250;
    private float barWidth = width - 4;
    private float barHeight = 3f;
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
            if (entity instanceof EntityPlayer) {
                entityLocationSkin = ((AbstractClientPlayer) entity).getLocationSkin();
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
        duration = durationVal.getValue().intValue();
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
                return;
            }

            barAnimation = Animation.create(
                    animatedBarWidth,
                    targetBarWidth,
                    100,
                    Easing.LINEAR,
                    val -> animatedBarWidth = val
            );

            barAnimation.start();
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(animatedScale, animatedScale, 1);
        GlStateManager.translate(-centerX, -centerY, 0);
        try {
            Rectangle.create(x, y, width, height).color(new Color(20, 20, 20, getAlpha(190))).push(e);
            if (entityIsPlayer) {
                headSize = height - 7;
                RenderUtil.renderHead(e, entityLocationSkin, x + 2, y + 2, headSize - 2, new Color(255, 255, 255, getAlpha(255)));
            } else {
                headSize = 0;
            }
            CFonts.SFPT_MEDIUM_18.drawStringWithShadow(entityName, x + 2 + headSize, y + 1, new Color(255, 255, 255, getAlpha(255)));
            CFonts.SFPT_REGULAR_18.drawStringWithShadow("Health: " + String.format("%.1f", entityHealth), x + 2 + headSize, y + 11, new Color(255, 255, 255, getAlpha(255)));
            CFonts.SFPT_REGULAR_18.drawStringWithShadow("Distance: " + String.format("%.1f", entityDistance), x + 2 + headSize, y + 21, new Color(255, 255, 255, getAlpha(255)));
            GradientRectangle.create(x + 2, y + height - 5, animatedBarWidth, barHeight).firstColor(new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), getAlpha(color.getAlpha()))).secondColor(new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), getAlpha(color.getAlpha())).darker()).gradientMode(GradientMode.VERTICAL).push(e);

        } catch (IllegalArgumentException error) {
            error.printStackTrace();
        }
        GlStateManager.popMatrix();

    }
}
