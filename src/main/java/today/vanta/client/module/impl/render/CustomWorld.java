package today.vanta.client.module.impl.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.render.WorldTintEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.ClientSettings;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;

import java.awt.*;

public class CustomWorld extends Module {
    private final NumberSetting alpha = Setting.of("Alpha", 100, 0, 255);

    public CustomWorld() {
        super("CustomWorld", "Applies a fullscreen color tint over the world.", Category.RENDER);
    }

    @EventListen
    private void onWorldTint(WorldTintEvent event) {
        float a = alpha.getValue().intValue() / 255.0F;
        if (a <= 0.0F) return;

        Color color = Vanta.instance.moduleStorage.getT(ClientSettings.class).colors[0];
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        int width = mc.displayWidth;
        int height = mc.displayHeight;

        int originalMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean texture2D = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(0, height, 0).color(r, g, b, a).endVertex();
        worldrenderer.pos(width, height, 0).color(r, g, b, a).endVertex();
        worldrenderer.pos(width, 0, 0).color(r, g, b, a).endVertex();
        worldrenderer.pos(0, 0, 0).color(r, g, b, a).endVertex();
        tessellator.draw();

        if (depthTest) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        if (texture2D) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (alphaTest) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        if (cullFace) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GlStateManager.alphaFunc(alphaFunc, alphaRef);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(originalMatrixMode);
    }
}
