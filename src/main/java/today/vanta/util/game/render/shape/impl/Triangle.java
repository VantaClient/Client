package today.vanta.util.game.render.shape.impl;

import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.GL11;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.shape.Shape;

import java.awt.*;

@NotNullByDefault
public class Triangle extends Shape<Triangle> {
    private Color color = Color.WHITE;

    private boolean outline = false;
    private float outlineWidth = 2.0f;

    private Triangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static Triangle create(double x, double y, double width, double height) {
        return new Triangle(x, y, width, height);
    }

    public Triangle color(Color color) {
        this.color = color;
        return this;
    }

    public Triangle outline(boolean outline) {
        this.outline = outline;
        return this;
    }

    public Triangle outlineWidth(float outlineWidth) {
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
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_TRIANGLES);
        GL11.glVertex2d(x + width / 2.0, y);
        GL11.glVertex2d(x, y + height);
        GL11.glVertex2d(x + width, y + height);
        GL11.glEnd();

        GL11.glPopMatrix();

        RenderUtil.stop();
    }
}