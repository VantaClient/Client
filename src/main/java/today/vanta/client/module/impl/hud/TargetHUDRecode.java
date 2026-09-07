package today.vanta.client.module.impl.hud;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;

public class TargetHUDRecode extends Module {
    private final BooleanSetting onlyPlayers = Setting.of("Allow only players", true);
    private float x = 450;
    private float y = 500;
    private float width = 120;
    private float height = 40;
    private EntityLivingBase entity;
    private int state;
    private int desiredState;
    private int ANIMATE_IN = 1;
    private int ANIMATE_OUT = 2;
    private int STANDBY = 3;
    private float entityHealth;
    private float entityMaxHealth;
    private String entityDisplayName;
    private ResourceLocation entityLocationSkin;
    private String entityName;
    private float entityDistance;
    private boolean entityIsPlayer;
    private boolean refreshed = false;
    private float animatedScale = 0;
    private float targetScale;
    private int duration = 250;
    public TargetHUDRecode() {
        super("TargetHUDRecode", "TargetHUD module but recoded.", Category.HUD);
    }

    private void checkState() {
        if (!(mc.currentScreen instanceof GuiChat)) {
            entity = TargetProcessor.getInstance().target;
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
                entityLocationSkin =((AbstractClientPlayer) entity).getLocationSkin();
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

    private int getAlpha (int alpha) {
        return (int) (alpha * animatedScale);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent e) {
        checkState();
        float centerX = x + width / 2;
        float centerY = y + height / 2;
        float headSize;
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX,centerY,0);
        GlStateManager.scale(animatedScale,animatedScale,1);
        GlStateManager.translate(-centerX, -centerY, 0);
        try {
            Rectangle.create(x,y,width,height).color(new Color(20,20,20,getAlpha(190))).push(e);
            if (entityIsPlayer) {
                headSize = height - 9;
                RenderUtil.renderHead(e,entityLocationSkin,x + 2,y + 2,headSize - 2,new Color(255,255,255,getAlpha(255)));
            } else {
                headSize = 0;
            }
            CFonts.SFPT_MEDIUM_18.drawStringWithShadow(entityDisplayName,x + 2 + headSize,y + 2,new Color(255,255,255,getAlpha(255)));

        } catch (IllegalArgumentException error) {
            error.printStackTrace();
        }
        GlStateManager.popMatrix();
        
    }
}
