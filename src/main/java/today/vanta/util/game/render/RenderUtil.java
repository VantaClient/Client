package today.vanta.util.game.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import today.vanta.Vanta;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author liticane
 */
public class RenderUtil {
    public static boolean hovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    public static void rectangle(double x, double y, double width, double height, boolean filled, Color color) {
        start();

        if (color != null)
            color(color);

        GL11.glLineWidth(2.0f);
        GlStateManager.glBegin(filled ? GL_TRIANGLE_FAN : GL_LINE_LOOP);

        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x, y + height);

        GlStateManager.glEnd();
        stop();
    }

    public static void rectangle(double x, double y, double width, double height, Color color) {
        rectangle(x, y, width, height, true, color);
    }

    public static void rectangle(double x, double y, double width, double height, int color) {
        rectangle(x, y, width, height, true, new Color(color));
    }

    public static void start_scissor() {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    public static void scissor(double x, double y, double width, double height) {
        width = Math.max(width, 0.1);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        double scale = sr.getScaleFactor();

        y = sr.getScaledHeight() - y;

        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;

        GL11.glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }

    public static void end_scissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }

    public static void image(ResourceLocation resourceLocation, float x, float y, float width, float height, Color color) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(width, height, 1);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(1, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(0, 1);
        GL11.glEnd();

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    public static void image(ResourceLocation resourceLocation, float x, float y, float width, float height, int color) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(width, height, 1);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(1, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(0, 1);
        GL11.glEnd();

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    public static void image(int textureId, int x, int y, int width, int height) {
        if (textureId == 0) return;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(textureId);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GlStateManager.disableBlend();
        GlStateManager.bindTexture(0);
    }

    public static void color(Color color) {
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
    }

    public static void color(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    public static void start() {
        GlStateManager.pushMatrix();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();

        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void stop() {
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();

        GlStateManager.disableBlend();
        color(Color.WHITE);
        GlStateManager.popMatrix();
    }

    public static int bindBufferedImage(BufferedImage image) {
        int textureId = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), image, false, false);
        GlStateManager.bindTexture(textureId);

        return textureId;
    }

    public static BufferedImage base64ToBufferedImage(String base64Image) {
        try {
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            Vanta.instance.logger.error("Failed to create an image from base64.");
            return null;
        }
    }
}