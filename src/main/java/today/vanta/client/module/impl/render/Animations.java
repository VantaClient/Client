package today.vanta.client.module.impl.render;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.render.*;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;

public class Animations extends Module {
    private final StringSetting mode = Setting.of("Mode", "1.7", "1.7", "1.8", "Interia", "Exhibition", "Exhibition Tilt", "Sigma", "Stella", "Smooth", "Up", "Claude");
    private final NumberSetting swingSpeed = Setting.of("Swing speed", 1, 0.1, 3.0, 1);

    private final BooleanSetting
            noBob = Setting.of("German No-bob", false),
            noSway = Setting.of("No hand sway", false),
            smoothSwing = Setting.of("Smooth swing", false),
            noresetanim = Setting.of("No Swing Reset", false);

    public Animations() {
        super("Animations", "Modifies Minecraft block animations.", Category.RENDER);
    }

    @EventListen
    private void onBobArm(BobArmEvent event) {
        event.cancelled = noSway.getValue();
    }

    @EventListen
    private void onRenderItemSwing(RenderItemSwingEvent event) {
        if (smoothSwing.getValue()) {
            event.cancelled = true;
            event.renderer.doItemUsedTransformations(0);
            event.renderer.transformFirstPersonItem(event.equippedProgress, event.swingProgress);
        }
    }

    @EventListen
    private void onSwingAnimation(SwingAnimationEvent event) {
        event.cancelled = true;
        event.swingSpeed = mc.thePlayer.isPotionActive(Potion.digSpeed)
                ? 6 - (1 + mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier())
                : (int) ((mc.thePlayer.isPotionActive(Potion.digSlowdown)
                          ? 6 + (1 + mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2
                          : 6) * (1.0 / swingSpeed.getValue().doubleValue()));
    }

    @EventListen
    private void onSwingAnimReset(SwingAnimResetEvent event) {
        event.cancelled = noresetanim.getValue();
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state == EventState.PRE && noBob.getValue()) {
            mc.thePlayer.distanceWalkedModified = 0.0F;
            mc.gameSettings.viewBobbing = true;
        }
    }

    @EventListen
    private void onPerformBlock(PerformBlockEvent event) {
        event.cancelled = true;
        ItemRenderer renderer = event.renderer;

        float f = event.equippedProgress;
        float f1 = mc.thePlayer.getSwingProgress(event.partialTicks);

        float var9 = MathHelper.sin(MathHelper.sqrt_float(f1) * MathHelper.PI);

        switch (mode.getValue()) {
            case "1.8": {
                renderer.transformFirstPersonItem(0, 0.0F);
                renderer.doBlockTransformations();
                break;
            }
            case "1.7": {
                renderer.transformFirstPersonItem(0.0f, f1);
                GlStateManager.translate(0,0.167,0);
                renderer.doBlockTransformations();
                break;
            }

            case "Sigma":
                renderer.transformFirstPersonItem(f * 0.5f, 0);
                GlStateManager.rotate(-var9 * 55 / 2.0F, -8.0F, -0.0F, 9.0F);
                GlStateManager.rotate(-var9 * 45, 1.0F, var9 / 2, -0.0F);
                renderer.doBlockTransformations();
                GL11.glTranslated(1.2, 0.3, 0.5);
                GL11.glTranslatef(-1, mc.thePlayer.isSneaking() ? -0.1F : -0.2F, 0.2F);
                break;
            case "Stella":
                renderer.transformFirstPersonItem(-0.1f, f1);
                GlStateManager.translate(-0.5F, 0.4F, -0.2F);
                GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(40.0F, 0.0F, 1.0F, 0.0F);
                break;
            case "Exhibition":
                GL11.glTranslated(-0.04D, 0.13D, 0.0D);
                renderer.transformFirstPersonItem(f / 2.5F, 0.0f);
                GlStateManager.rotate(-var9 * 40.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F * 0.2f);
                GlStateManager.rotate(-var9 * 30.0F, 1.0F, var9 / 3.0F, -0.0F);
                renderer.doBlockTransformations();
                break;
            case "Exhibition Tilt":
                GL11.glTranslated(-0.04D, 0.13D, 0.0D);
                renderer.transformFirstPersonItem(f / 2.5F, 0.0f);
                GlStateManager.rotate(-var9 * 12.0F / 1.0F, var9 / 2.0F, 1.0F, 4.0F * 0.2f);
                GlStateManager.rotate(-var9 * 12.0F, 1.0F, var9 / 3.0F, -0.0F);
                renderer.doBlockTransformations();
                break;
            case "Interia":
                renderer.transformFirstPersonItem(0.05f, f1);
                GlStateManager.translate(-0.5F, 0.5F, 0.0F);
                GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                break;
            case "Smooth":
                renderer.transformFirstPersonItem(f / 1.5F, 0.0f);
                renderer.doBlockTransformations();
                GlStateManager.translate(-0.05f, 0.3f, 0.3f);
                GlStateManager.rotate(-var9 * 140.0f, 8.0f, 0.0f, 8.0f);
                GlStateManager.rotate(var9 * 90.0f, 8.0f, 0.0f, 8.0f);
                break;
            case "Up":
                GL11.glTranslated(-0.04D, 0.13D, 0.0D);
                renderer.transformFirstPersonItem(f * 2.5F, 0.0f);
                GlStateManager.rotate(var9 * 10.0F, 1.5f, -var9 * 0.1f, -0.0F);
                renderer.doBlockTransformations();
                break;
            case "Claude":
                // I told Claude to make random values with the one above so this is what it gave me
                GL11.glTranslated(-0.04D + var9 * 0.05D, 0.13D + f1 * 0.1D, 0.0D);
                renderer.transformFirstPersonItem(f * 2.5F, var9 * 15.0f);
                GlStateManager.rotate(var9 * 25.0F, 1.5f, f1 * 2.0f, -f * 5.0F);
                GlStateManager.scale(1.0F, 1.0F, 1.0F + var9 * 0.2F);
                renderer.doBlockTransformations();
                break;
        }
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}