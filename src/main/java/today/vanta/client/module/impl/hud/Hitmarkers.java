package today.vanta.client.module.impl.hud;

import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.util.MathHelper;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.render.shape.impl.Rectangle;
import today.vanta.util.system.math.MathUtil;

import java.awt.*;

public class Hitmarkers extends Module {
    private long duration = 450;
    private long lastAttack;
    private float elapsed;
    private int width = 3;
    private int height = 6;
    public Hitmarkers() {
        super("Hitmarkers", "Shows indicators on the crosshair when you hit somebody.", Category.HUD);
    }

    @EventListen
    private void onPacket(SendPacketEvent event) {
        if (event.packet instanceof C02PacketUseEntity)
            if (((C02PacketUseEntity) event.packet).getAction() == C02PacketUseEntity.Action.ATTACK) {
                lastAttack = System.currentTimeMillis();
                ChatUtil.send(ChatUtil.Prefix.INFO, event.packet.toString());
            }
    }

    @EventListen
    private void onRender2D(RenderOverlayEvent event) {
        if (lastAttack == 0) return;
        elapsed = MathHelper.clamp_float(System.currentTimeMillis() - lastAttack, 0f, duration);
        if (elapsed < duration) {
            Rectangle
                    .create((double) event.scaledResolution.getScaledWidth() / 2 - width - 5, (double) event.scaledResolution.getScaledHeight() - height - 5, width, height)
                    .rotate(10)
                    .color(Color.white)
                    .push(event);
        }

    }

    @Override
    public void onEnable() {
        lastAttack = 0;
        elapsed = 0;
    }
}
