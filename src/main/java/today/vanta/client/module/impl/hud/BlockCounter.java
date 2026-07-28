package today.vanta.client.module.impl.hud;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.player.InventoryUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.Renderable;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.animation.Animation;
import today.vanta.util.system.math.animation.Easing;

import java.awt.*;

public class BlockCounter extends Module {
    private static final Color BACKGROUND = new Color(20, 20, 20, 200);

    private final StringSetting mode = Setting.of("Mode", "Vanta", "Vanta", "Box", "Adjust");

    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 70, 0, 2000);

    private static float WIDTH = 90;
    private static float HEIGHT = 40;

    private boolean dragging;
    private float dragX, dragY;
    private int maxBlocks = 0;

    private Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
    private int blocks;
    private Animation animation;
    private float targetwidth = WIDTH - 4;
    ;
    private float animBarWidth = WIDTH - 4;
    ;

    public BlockCounter() {
        super("BlockCounter", "Block information.", Category.HUD);
        hideFromArraylist = true;
    }

    private boolean canBeDrawn() {
        return (mc.currentScreen instanceof GuiChat) || TargetProcessor.getInstance().scaffold.isEnabled();
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        float centerValueX = ((float) event.scaledResolution.getScaledWidth() / 2) - (WIDTH / 2);
        if (x.getValue() == null) {
            return;
        }
        if (x.getValue().floatValue() == centerValueX && dragging) {
            Rectangle
                    .create((float) event.scaledResolution.getScaledWidth() / 2 - 1, 0, 2, event.scaledResolution.getScaledHeight())
                    .color(new Color(200, 200, 200, 180))
                    .push(event);
        }
    }

    @EventListen
    private void onRenderScreen(RenderScreenEvent event) {
        if (mc.thePlayer == null) {
            maxBlocks = 0;
            return;
        }
        blocks = InventoryUtil.getHotbarBlockCount();

        if (maxBlocks < blocks) {
            maxBlocks = blocks;
        }

        if (canBeDrawn()) {
            draw(event);
            if (mc.currentScreen instanceof GuiChat) {
                handleDragging(event.mouseX, event.mouseY);
            }
        }
    }

    private void handleDragging(float mouseX, float mouseY) {
        if (Mouse.isButtonDown(0)) {
            if (!dragging && RenderUtil.hovered(mouseX, mouseY, x.getValue().floatValue(), y.getValue().floatValue(), WIDTH, HEIGHT)) {
                dragging = true;
                dragX = mouseX - x.getValue().floatValue();
                dragY = mouseY - y.getValue().floatValue();
            }

            if (dragging) {
                x.setValue(mouseX - dragX);
                y.setValue(mouseY - dragY);
            }
        } else {
            dragging = false;
        }
    }

    private void draw(Renderable renderable) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        switch (mode.getValue()) {
            case "Vanta":
                WIDTH = 90;
                HEIGHT = 40;

                Rectangle
                        .create(x, y, WIDTH, HEIGHT)
                        .color(BACKGROUND)
                        .push(renderable);

                RenderUtil.renderScaledItem(InventoryUtil.getBestBlockStack(), x, y + 0.5f, 2.4f);

                CFonts.SFPT_SEMIBOLD_20.drawStringWithShadow("Blocks", x + 38, y + 4, color);
                CFonts.SFPT_SEMIBOLD_20.drawStringWithShadow(String.valueOf(blocks), x + 38, y + 15, Color.WHITE);
                break;

            case "Box":
                WIDTH = 90;
                HEIGHT = 20;

                color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
                blocks = InventoryUtil.getHotbarBlockCount();
                float barWidth = WIDTH - 4;
                float bar = barWidth * ((float) blocks / maxBlocks);

                if (bar != targetwidth) {
                    targetwidth = bar;

                    animation = Animation.create(
                            animBarWidth,
                            targetwidth,
                            100,
                            Easing.EASE_IN_OUT,
                            val -> animBarWidth = val
                    );

                    animation.start();
                }

                if (animBarWidth != targetwidth) {
                    targetwidth = bar;
                    if (animation.finished) {
                        animation = Animation.create(
                                animBarWidth,
                                targetwidth,
                                100,
                                Easing.EASE_IN_OUT,
                                val -> animBarWidth = val
                        );

                        animation.start();
                    }
                }


                Rectangle
                        .create(x, y, WIDTH, HEIGHT)
                        .color(BACKGROUND)
                        .push(renderable);

                Rectangle
                        .create(x + 2, y + 14, barWidth, 3)
                        .color(BACKGROUND.darker())
                        .push(renderable);

                Rectangle
                        .create(x + 2, y + 14, animBarWidth, 3)
                        .color(color)
                        .push(renderable);

                String block_str = String.valueOf(blocks);
                float length = CFonts.SFPT_SEMIBOLD_20.getStringWidth(block_str);

                CFonts.SFPT_SEMIBOLD_20.drawStringWithShadow("Blocks", x + 1, y + 1, -1);
                CFonts.SFPT_SEMIBOLD_20.drawStringWithShadow(block_str, x + WIDTH - length - 2 - 1, y + 1, -1);
                break;
            case "Adjust":
                ScaledResolution res = new ScaledResolution(mc);
                String numberStr = String.valueOf(blocks);
                String suffixStr = " blocks";

                float suffixLength = CFonts.getFont("T-Regular", 18).getStringWidth(suffixStr);
                float totalLength = CFonts.getFont("T-Regular", 18).getStringWidth(numberStr) + suffixLength;
                float numberLength = CFonts.getFont("T-Regular", 18).getStringWidth(numberStr);
                float spaceLength = CFonts.getFont("T-Regular", 18).getStringWidth(" ");

                float adjustX = res.getScaledWidth() / 2f - totalLength / 2f;
                float adjustY = res.getScaledHeight() / 2f + 15;

                CFonts.getFont("T-Regular", 18).drawStringWithShadow(numberStr, adjustX, adjustY, color);
                CFonts.getFont("T-Regular", 18).drawStringWithShadow("blocks", adjustX + spaceLength + numberLength, adjustY, Color.white);
                break;
        }

        if (dragging && mc.currentScreen instanceof GuiChat) {
            Rectangle
                    .create(x - 0.5, y - 0.5, WIDTH + 1, HEIGHT + 1)
                    .outline(true)
                    .color(color)
                    .push(renderable);
        }
    }
}