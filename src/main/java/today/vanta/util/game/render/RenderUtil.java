package today.vanta.util.game.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.optifine.reflect.Reflector;
import org.lwjgl.opengl.GL11;
import today.vanta.Vanta;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.module.impl.client.WindowSettings;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.GradientMode;
import today.vanta.util.game.render.shape.impl.GradientRectangle;
import today.vanta.util.game.render.shape.impl.ImageRectangle;
import today.vanta.util.game.render.shape.impl.Rectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean hovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static void start_scissor() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    private static void end_scissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void scissor(double x, double y, double width, double height, Runnable runnable) {
        ScaledResolution sr = new ScaledResolution(mc);
        double scaleFactor = sr.getScaleFactor();

        int sx = (int) (x * scaleFactor);
        int sw = (int) (width * scaleFactor);
        int sh = (int) (height * scaleFactor);

        int sy = (int) ((sr.getScaledHeight() - y - height) * scaleFactor);

        start_scissor();
        GL11.glScissor(sx, sy, sw, sh);

        try {
            runnable.run();
        } finally {
            end_scissor();
        }
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
        GL11.glLineWidth(1.0f);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();

        GlStateManager.disableBlend();
        color(Color.WHITE);
        GlStateManager.popMatrix();
    }

    public static void renderEntity(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase entity) {
        GuiInventory.drawEntityOnScreen(posX, posY, scale, mouseX, mouseY, entity);
    }

    public static void drawWindowRectangle(Renderable renderable, String title, float x, float y, float width, float height) {
        Color WINDOWBG = new Color(20, 20, 20, 220);
        Color BACKGROUND = new Color(20, 20, 20, 190);
        float windowHeight = 12;
        float textHeight = CFonts.SFPT_REGULAR_18.getFontHeight();
        Color outlineColor = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        Color outlineColor2 = Vanta.instance.moduleStorage.getT(Theme.class).colors[1];
        float outlineWidth = 0.5f;

        Rectangle
                .create(x, y, width, windowHeight)
                .color(WINDOWBG)
                .push(renderable);
        switch (Vanta.instance.moduleStorage.getT(WindowSettings.class).textAlignment.getValue()) {
            case "Left":
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(title, x + 0.5f, y - (textHeight / 2) - 2.5f + (windowHeight / 2), Color.white);
                break;
            case "Center":
                float textWidth = CFonts.SFPT_REGULAR_18.getStringWidth(title);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(title, x + (width / 2) - (textWidth / 2), y - (textHeight / 2) - 2.5f + (windowHeight / 2), Color.white);
                break;
        }

        Rectangle
                .create(x, y + windowHeight, width, height)
                .color(BACKGROUND)
                .push(renderable);
        String outlineMode = Vanta.instance.moduleStorage.getT(WindowSettings.class).outline.getValue();
        if (outlineMode != null && !outlineMode.equals("None")) {
            switch (outlineMode) {
                case "Primary":
                    Rectangle
                            .create(x - (outlineWidth / 2), y - (outlineWidth / 2), width + outlineWidth, height + windowHeight + outlineWidth)
                            .color(outlineColor)
                            .outline(true)
                            .outlineWidth(outlineWidth)
                            .push(renderable);
                    break;
                case "Secondary":
                    Rectangle
                            .create(x - (outlineWidth / 2), y - (outlineWidth / 2), width + outlineWidth, height + windowHeight + outlineWidth)
                            .color(outlineColor2)
                            .outline(true)
                            .outlineWidth(outlineWidth)
                            .push(renderable);
                    break;
                case "Horizontal gradient":
                    GradientRectangle
                            .create(x - (outlineWidth / 2), y - (outlineWidth / 2), width + outlineWidth, height + windowHeight + outlineWidth)
                            .firstColor(outlineColor)
                            .secondColor(outlineColor2)
                            .outline(true)
                            .outlineWidth(outlineWidth)
                            .gradientMode(GradientMode.HORIZONTAL)
                            .push(renderable);
                    break;
                case "Vertical gradient":
                    GradientRectangle
                            .create(x - (outlineWidth / 2), y - (outlineWidth / 2), width + outlineWidth, height + windowHeight + outlineWidth)
                            .firstColor(outlineColor)
                            .secondColor(outlineColor2)
                            .outline(true)
                            .outlineWidth(outlineWidth)
                            .gradientMode(GradientMode.VERTICAL)
                            .push(renderable);
                    break;
            }
        }
    }

    public static float getTotalWindowHeight(float height) {
        return height + 12;
    }

    public static float getTotalWindowWidth(float width) {
        float total = width;
        if (!Vanta.instance.moduleStorage.getT(WindowSettings.class).outline.isValue("None")) {
            total += 0.5f;
        }
        return total;
    }

    public static float getOutlineWidth() {
        if (!Vanta.instance.moduleStorage.getT(WindowSettings.class).outline.isValue("None")) {
            return 0.5f;
        } else {
            return 0;
        }
    }

    public static void renderHead(Renderable renderable, EntityPlayer target, float x, float y, float headSize) throws NullPointerException {
        ImageRectangle
                .create(x, y, headSize, headSize, -1)
                .uv(8, 8)
                .uvSize(8, 8)
                .tileSize(64, 64)
                .resource(((AbstractClientPlayer) target).getLocationSkin())
                .push(renderable);

        ImageRectangle
                .create(x, y, headSize, headSize, -1)
                .uv(40, 8)
                .uvSize(8, 8)
                .tileSize(64, 64)
                .resource(((AbstractClientPlayer) target).getLocationSkin())
                .push(renderable);
    }

    public static BufferedImage base64ToBufferedImage(String base64Image) {
        base64Image = base64Image.replace("\\u003d", "=");
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

    public static void renderScaledItem(ItemStack stack, float x, float y, float scale) {
        if (stack == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, scale);

        renderItemIntoGUIFullBright(stack, 0, 0);

        GlStateManager.popMatrix();
    }

    public static void renderItemIntoGUIFullBright(ItemStack stack, float x, float y) {
        mc.renderItem.renderItemGui = true;
        IBakedModel ibakedmodel = mc.renderItem.getItemModelMesher().getItemModel(stack);
        GlStateManager.pushMatrix();
        mc.renderItem.textureManager.bindTexture(TextureMap.locationBlocksTexture);
        mc.renderItem.textureManager.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        setupGuiTransform(x, y, ibakedmodel.isGui3d());

        if (Reflector.ForgeHooksClient_handleCameraTransforms.exists()) {
            ibakedmodel = (IBakedModel) Reflector.call(Reflector.ForgeHooksClient_handleCameraTransforms, new Object[]{ibakedmodel, ItemCameraTransforms.TransformType.GUI});
        } else {
            ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GUI);
        }

        GlStateManager.disableLighting();
        mc.renderItem.renderItem(stack, ibakedmodel);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        mc.renderItem.textureManager.bindTexture(TextureMap.locationBlocksTexture);
        mc.renderItem.textureManager.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
        mc.renderItem.renderItemGui = false;
    }

    private static void setupGuiTransform(float xPosition, float yPosition, boolean isGui3d) {
        GlStateManager.translate(xPosition, yPosition, 100.0F + mc.renderItem.zLevel);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, -1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);

        if (isGui3d) {
            GlStateManager.scale(40.0F, 40.0F, 40.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0F, 64.0F, 64.0F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableLighting();
        }
    }
}