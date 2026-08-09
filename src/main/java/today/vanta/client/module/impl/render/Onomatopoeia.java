package today.vanta.client.module.impl.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.MultiStringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ProjectionUtil;
import today.vanta.util.game.render.font.CFonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Onomatopoeia extends Module {
    private final MultiStringSetting entities = Setting.of("Entities", new String[]{"Players"}, new String[]{"Players", "Monsters", "Animals", "Local", "Invisibles"});

    private final List<HitMarker> markers = new ArrayList<>();
    private String[] messages = {
            "BOOM!",
            "POW!",
            "BAM!",
            "WHACK!",
            "SMACK!",
            "SOCK!",
            "THWACK!",
            "BONK!",
            "CRACK!",
            "CRUNCH!",
            "CRASH!",
            "SMASH!",
            "THUD!",
            "WHAM!",
            "CLANG!",
            "SLASH!",
            "SWISH!",
            "SHING!",
            "SLICE!",
            "CHOP!",
            "KAPOW!"
    };
    private EntityLivingBase entityLivingBase = null;
    private EntityLivingBase lastTarget = null;

    // guards for onSendPacket
    private Object lastProcessedPacket = null;
    private boolean attackRegistered = false;

    public Onomatopoeia() {
        super("Onomatopoeia", "Displays cartoon like text on target damage.", Category.RENDER);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        float ticks = event.partialTicks;
        ScaledResolution sr = event.scaledResolution;
        Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        if (TargetProcessor.getInstance().target != null) {
            if (validEntity(TargetProcessor.getInstance().target)) {
                entityLivingBase = TargetProcessor.getInstance().target;
            }
        } else {
            if (mc.objectMouseOver.entityHit != null) {
                if (validEntity(mc.objectMouseOver.entityHit)) {
                    entityLivingBase = (EntityLivingBase) mc.objectMouseOver.entityHit;
                }
            }
        }

        // once the hit animation resets, allow a new marker to be registered for the next attack
        if (entityLivingBase != null && entityLivingBase.hurtTime == 0) {
            attackRegistered = false;
        }

        Iterator<HitMarker> iterator = markers.iterator();
        while (iterator.hasNext()) {
            HitMarker marker = iterator.next();

            if (System.currentTimeMillis() > marker.timestamp + 3000L) {
                iterator.remove();
                continue;
            }

            ProjectionUtil.ScreenBounds bounds = ProjectionUtil.projectBoundingBox(marker.entity, ticks, sr);
            if (bounds == null) continue;

            // roll the *relative* position within the bounding box once, then re-project
            // it onto the box every frame so it tracks the entity instead of drifting
            if (marker.xFraction == null || marker.yFraction == null) {
                marker.xFraction = ThreadLocalRandom.current().nextDouble();
                marker.yFraction = ThreadLocalRandom.current().nextDouble();
            }

            double x = bounds.minX + marker.xFraction * (bounds.maxX - bounds.minX);
            double y = bounds.minY + marker.yFraction * (bounds.maxY - bounds.minY);

            CFonts.getFont("BADABB", 48).drawStringWithShadow(messages[marker.messageIndex], (float) x, (float) y, color);
        }
    }

    @EventListen
    private void onSendPacket(SendPacketEvent event) {
        if (entityLivingBase == null) return;
        if (!(event.packet instanceof C02PacketUseEntity)) return;

        // ignore duplicate dispatch of the exact same packet object
        if (event.packet == lastProcessedPacket) return;
        lastProcessedPacket = event.packet;

        C02PacketUseEntity useEntityPacket = (C02PacketUseEntity) event.packet;
        if (useEntityPacket.getAction() != C02PacketUseEntity.Action.ATTACK) return;

        // don't add a new timestamp again for the same target until the swing resets (hurtTime == 0)
        if (lastTarget == entityLivingBase && attackRegistered) return;

        if (entityLivingBase.hurtTime == 0) return;

        markers.add(new HitMarker(entityLivingBase));
        lastTarget = entityLivingBase;
        attackRegistered = true;
    }

    private boolean validEntity(Entity living) {
        if (living == mc.thePlayer && entities.isEnabled("Local")) return true;
        if (living instanceof EntityAnimal && entities.isEnabled("Animals")) return true;
        if (living instanceof IMob && entities.isEnabled("Monsters")) return true;
        if (living.isInvisible() && entities.isEnabled("Invisibles")) return true;
        if (living instanceof EntityPlayer && entities.isEnabled("Player")) return true;
        if (TargetProcessor.getInstance().bots.contains(living.getName())) return false;
        return false;
    }

    private class HitMarker {
        final EntityLivingBase entity;
        final long timestamp;
        final int messageIndex;
        Double xFraction;
        Double yFraction;

        HitMarker(EntityLivingBase entity) {
            this.entity = entity;
            this.timestamp = System.currentTimeMillis();
            this.messageIndex = ThreadLocalRandom.current().nextInt(0, messages.length);
        }
    }
}