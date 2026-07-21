package today.vanta.util.game.render.shape.impl;

import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.GL11;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.Shape;

import java.awt.*;

@NotNullByDefault
public class GradientRectangle extends Shape<GradientRectangle> {
    private Color firstColor = Color.WHITE;
    private Color secondColor = Color.BLACK;

    private GradientMode gradientMode = GradientMode.HORIZONTAL;

    private boolean outline = false;
    private float outlineWidth = 2.0f;

    private GradientRectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static GradientRectangle create(double x, double y, double width, double height) {
        return new GradientRectangle(x, y, width, height);
    }

    public GradientRectangle firstColor(Color firstColor) {
        this.firstColor = firstColor;
        return this;
    }

    public GradientRectangle secondColor(Color secondColor) {
        this.secondColor = secondColor;
        return this;
    }

    public GradientRectangle gradientMode(GradientMode gradientMode) {
        this.gradientMode = gradientMode;
        return this;
    }

    public GradientRectangle outline(boolean outline) {
        this.outline = outline;
        return this;
    }

    public GradientRectangle outlineWidth(float outlineWidth) {
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

        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_QUADS);
        switch (gradientMode) {
            case VERTICAL:
                // Top left
                RenderUtil.color(firstColor);
                GL11.glVertex2d(x, y);

                // Top right
                RenderUtil.color(firstColor);
                GL11.glVertex2d(x + width, y);

                // Bottom right
                RenderUtil.color(secondColor);
                GL11.glVertex2d(x + width, y + height);

                // Bottom left
                RenderUtil.color(secondColor);
                GL11.glVertex2d(x, y + height);
                break;
            case HORIZONTAL:
                // Top left
                RenderUtil.color(firstColor);
                GL11.glVertex2d(x, y);

                // Top right
                RenderUtil.color(secondColor);
                GL11.glVertex2d(x + width, y);

                // Bottom right
                RenderUtil.color(secondColor);
                GL11.glVertex2d(x + width, y + height);

                // Bottom left
                RenderUtil.color(firstColor);
                GL11.glVertex2d(x, y + height);

                break;
        }
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);

        GL11.glPopMatrix();

        RenderUtil.stop();
    }
}