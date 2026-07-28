package today.vanta.client.module.impl.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Mouse;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.client.RenderScreenEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.module.impl.combat.KillAura;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.player.PlayerUtil;
import today.vanta.util.game.render.RenderUtil;
import today.vanta.util.game.render.font.CFonts;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.game.world.EntityUtil;
import today.vanta.util.system.math.Counter;
import today.vanta.util.system.math.MathUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Information extends Module {
    private final StringSetting mode = Setting.of("Mode", "Text", "Window", "Text");
    private final NumberSetting
            x = Setting.of("X position", 20, 0, 2000),
            y = Setting.of("Y position", 20, 0, 2000);

    private float width = 75;
    private float height = 50;
    private boolean dragging;
    private float dragX, dragY;
    private final Counter playTime = new Counter();
    private String oldServer;
    private float totalHeight;
    private float totalWidth;
    private float outlineWidth;
    private final List<EntityLivingBase> list = new ArrayList<>();
    private int kills = 0;

    public Information() {
        super("Information", "Provides information on the player.", Category.HUD);
        displayNames = new String[]{"Information", "SessionStats", "SessionInfo"};
        hideFromArraylist = true;
    }

    private void handleDragging(float mouseX, float mouseY) {
        if (mode.isValue("Text")) return;
        if (Mouse.isButtonDown(0)) {
            if (!dragging && RenderUtil.hovered(mouseX, mouseY, x.getValue().floatValue(), y.getValue().floatValue(), width, height)) {
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

    @EventListen
    private void onRenderScreen(RenderScreenEvent event) {
        if (mc.currentScreen instanceof GuiChat) {
            handleDragging(event.mouseX, event.mouseY);
        }
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        Color color1 = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];

        if (!mc.isSingleplayer() && mc.getCurrentServerData().serverIP != null) {
            if (!Objects.equals(oldServer, mc.getCurrentServerData().serverIP)) {
                playTime.reset();
                oldServer = mc.getCurrentServerData().serverIP;
            }
        }

        switch (mode.getValue()) {
            case "Text":
                totalHeight = height;
                totalWidth = width;
                outlineWidth = 0;
                float ydraw = event.scaledResolution.getScaledHeight() - 19;
                if (mc.currentScreen instanceof GuiChat) {
                    ydraw = event.scaledResolution.getScaledHeight() - 230;
                }
                mc.exhiFontRendererObj.drawString("Ping: " + PlayerUtil.getPing(mc.thePlayer), 2, ydraw, Color.WHITE,true);
                mc.exhiFontRendererObj.drawString("BPS: " + MovementUtil.getBPS(), 2, ydraw + 10, Color.WHITE,true);
                break;
            case "Window":
                width = 150;
                height = 51;
                totalHeight = RenderUtil.getTotalWindowHeight(height);
                totalWidth = RenderUtil.getTotalWindowWidth(width);
                outlineWidth = RenderUtil.getOutlineWidth();
                RenderUtil.drawWindowRectangle(event,displayName,x.getValue().floatValue(),y.getValue().floatValue(), width, height);


//                Rectangle
//                        .create(x.getValue().floatValue(),y.getValue().floatValue(),WIDTH,12)
//                        .color(WINDOWBG)
//                        .push(event);
//                CFonts.SFPT_REGULAR_18.drawStringWithShadow("Information", x.getValue().floatValue() + 1, y.getValue().floatValue(), Color.white);
//                Rectangle
//                        .create(x.getValue().floatValue(),y.getValue().floatValue() + 12,WIDTH,HEIGHT - 12)
//                        .color(BACKGROUND)
//                        .push(event);

                RenderUtil.renderHead(event,mc.thePlayer,x.getValue().floatValue() + 2,y.getValue().floatValue() + 14,47);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow(mc.thePlayer.getName(), x.getValue().floatValue() + 50, y.getValue().floatValue() + 11.6f, Color.WHITE);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow("FPS: "+ Minecraft.getDebugFPS(), x.getValue().floatValue() + 50, y.getValue().floatValue() + 21.6f, Color.WHITE);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow("BPS: "+ MovementUtil.getBPS(), x.getValue().floatValue() + 50, y.getValue().floatValue() + 31.6f, Color.WHITE);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow("Ping: "+ PlayerUtil.getPing(mc.thePlayer), x.getValue().floatValue() + 50, y.getValue().floatValue() + 41.6f, Color.WHITE);
                CFonts.SFPT_REGULAR_18.drawStringWithShadow("Session: " + MathUtil.formatDuration(playTime.getElapsedTime()), x.getValue().floatValue() + 50, y.getValue().floatValue() + 51.6f, Color.WHITE);

                break;

        }

        if (dragging && mc.currentScreen instanceof GuiChat) {
                Rectangle
                        .create(x.getValue().floatValue() - outlineWidth - 0.5f, y.getValue().floatValue() - outlineWidth -  0.5f, totalWidth + 1, totalHeight + 1)
                        .color(color1)
                        .outline(true)
                        .push(event);
        }
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        KillAura killAura = TargetProcessor.getInstance().killaura;
        mc.theWorld.getLoadedEntityList().stream()
                .filter(e -> e instanceof EntityPlayer)
                .map(e -> (EntityLivingBase) e)
                .filter(e -> EntityUtil.isValidforKill(e, killAura.raytrace.getValue(), killAura.searchRange.getValue().floatValue()))
                .sorted(EntityUtil.getComparatorForSorting(killAura.sortMode.getValue()))
                .forEachOrdered(list::add);

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).isDead) {
                    if (list.get(i).getLastAttacker() == null) return;
                    ChatUtil.send(ChatUtil.Prefix.INFO, list.get(i).getName() + " Killer: " + list.get(i).getLastAttacker().getName());
                    if (list.get(i).getLastAttacker() == mc.thePlayer) {
                        kills++;
                        list.remove(i);
                    }
                }
            }
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            kills = 0;
        }
    }
}
