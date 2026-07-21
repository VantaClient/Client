package today.vanta.util.game.render.shape.impl;

import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.GL11;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.shape.Shape;

import java.awt.*;

@NotNullByDefault
public class Rectangle extends Shape<Rectangle> {
    private Color color = Color.WHITE;

    private boolean outline = false;
    private float outlineWidth = 2.0f;

    private Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static Rectangle create(double x, double y, double width, double height) {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle color(Color color) {
        this.color = color;
        return this;
    }

    public Rectangle outline(boolean outline) {
        this.outline = outline;
        return this;
    }

    public Rectangle outlineWidth(float outlineWidth) {
        this.outlineWidth = outlineWidth;
        return this;
    }

    @Override
    public void push(Renderable renderable) {
        RenderUtil.start();

        GL11.glPushMatrix();
        GL11.glTranslated(x + width / 2.0, y + height / 2.0, 0);
        GL11.glRotatef(rotation, 0, 0, 1);
        GL11.glTranslated(-(x + width / 2.0), -(y + height / 2.0), 0);

        GL11.glLineWidth(outlineWidth);

        RenderUtil.color(color);
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_QUADS);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x, y + height);
        GL11.glEnd();

        GL11.glPopMatrix();

        RenderUtil.stop();
    }
}