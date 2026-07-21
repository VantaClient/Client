package today.vanta.util.game.render.shape.impl;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.GL11;
import today.vanta.util.game.render.ImageUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.shape.Shape;

import java.awt.*;

@NotNullByDefault
public class ImageRectangle extends Shape<ImageRectangle> {
    private int textureId = -1;
    private Color color = Color.WHITE;

    private double u = 0;
    private double v = 0;
    private double uWidth = 1;
    private double uHeight = 1;

    private double tileWidth = 1;
    private double tileHeight = 1;

    private ImageRectangle(double x, double y, double width, double height, int textureId) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textureId = textureId;
    }

    public static ImageRectangle create(double x, double y, double width, double height, int textureId) {
        return new ImageRectangle(x, y, width, height, textureId);
    }

    public ImageRectangle uv(double u, double v) {
        this.u = u;
        this.v = v;
        return this;
    }

    public ImageRectangle uvSize(double uWidth, double uHeight) {
        this.uWidth = uWidth;
        this.uHeight = uHeight;
        return this;
    }

    public ImageRectangle tileSize(double tileWidth, double tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        return this;
    }

    public ImageRectangle color(Color color) {
        this.color = color;
        return this;
    }

    public ImageRectangle resource(ResourceLocation location) {
        this.textureId = ImageUtil.bindAndGetId(location);
        return this;
    }

    @Override
    public void push(Renderable renderable) {
        if (textureId == -1) {
            return;
        }

        RenderUtil.start();

        GL11.glPushMatrix();
        GL11.glTranslated(x + width / 2.0, y + height / 2.0, 0);
        GL11.glRotatef(rotation, 0, 0, 1);
        GL11.glTranslated(-(x + width / 2.0), -(y + height / 2.0), 0);

        double minU = u / tileWidth;
        double minV = v / tileHeight;

        double maxU = (u + uWidth) / tileWidth;
        double maxV = (v + uHeight) / tileHeight;

        GlStateManager.enableTexture2D();
        RenderUtil.color(color);
        GlStateManager.bindTexture(textureId);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(minU, minV);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2d(maxU, minV);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2d(maxU, maxV);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2d(minU, maxV);
        GL11.glVertex2d(x, y + height);
        GL11.glEnd();

        GL11.glPopMatrix();

        RenderUtil.stop();
    }
}