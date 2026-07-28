package today.vanta.client.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.movement.Speed;
import today.vanta.client.processor.impl.RotationProcessor;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.ChatUtil;

import java.util.concurrent.ThreadLocalRandom;

public class Criticals extends Module {
    private static final double HYPIXEL_CRIT_OFFSET_1 = 0.0145D;
    private static final double HYPIXEL_CRIT_OFFSET_2 = 0.0105D;
    private static final double STANDARD_CRIT_OFFSET = 0.0722435151D;
    private static final double PACKET_CRIT_OFFSET = 0.01125D;
    private static final double RANDOM_OFFSET_MIN = 0.001D;
    private static final double RANDOM_OFFSET_MAX = 0.0011D;
    private int tick;

    private final StringSetting mode = Setting.of("Mode", "Edit", "Edit", "Packet", "Old Watchdog", "Mospixel");

    public Criticals() {
        super("Criticals", "Tries to make all landed hits critical.", Category.COMBAT);
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state != EventState.PRE) return;
        if (mode.isValue("Packet")) return;

//        if (!shouldCrit() && !mode.isValue("Mospixel Post")) return;

        Entity target = getTarget();
        if (target == null) return;

        int hurtResistantTime = target.hurtResistantTime;

        if (mode.isValue("Old Watchdog")) {
            applyHypixelCrit(event, hurtResistantTime);
        }
        if (mode.isValue("Mospixel")) {
            applyMospixelCrit(event,hurtResistantTime);
        }
//            applyStandardCrit(event, hurtResistantTime);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        tick++;
    }

    @EventListen
    private void onSendPacket(SendPacketEvent event) {
        if (mc.thePlayer == null) return;

        if (!mode.isValue("Packet")) return;

        if (!(event.packet instanceof C0APacketAnimation)) return;

        if (!shouldCrit()) return;

        Entity target = getTarget();
        if (target == null) return;

        if (shouldSendPacketCrit(target)) {
            sendPacketCrit();
        }
    }

    private void applyHypixelCrit(MotionEvent event, int hurtResistantTime) {
        switch (hurtResistantTime) {
            case 19:
                applyCrit(event, HYPIXEL_CRIT_OFFSET_1);
                break;
            case 18:
                applyCrit(event, HYPIXEL_CRIT_OFFSET_2);
                break;
            case 17:
                applyRandomCrit(event);
                break;
        }
    }

    private void applyMospixelCrit(MotionEvent event, int hurtResistantTime) {
        if (hurtResistantTime == 19 && tick > 10 && mc.thePlayer.onGround && !Vanta.instance.moduleStorage.getT(Speed.class).isEnabled()) {
            mc.getNetHandler().addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY + 0.0630,
                            mc.thePlayer.posZ,
                            false
                    )
            );
            mc.getNetHandler().addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ,
                            false
                    )
            );
            ChatUtil.send(ChatUtil.Prefix.INFO, String.valueOf(tick));
            tick = 0;
        }
    }

    private void applyStandardCrit(MotionEvent event, int hurtResistantTime) {
        switch (hurtResistantTime) {
            case 17:
                applyRandomCrit(event);
                break;
            case 18:
                applyCrit(event, STANDARD_CRIT_OFFSET);
                break;
            case 19:
                applyRandomCrit(event);
                break;
            case 20:
                applyCrit(event, STANDARD_CRIT_OFFSET);
                break;
        }
    }

    private void applyCrit(MotionEvent event, double offset) {
        event.onGround = false;
        event.y = event.y + offset;
    }

    private void applyRandomCrit(MotionEvent event) {
        double randomOffset = ThreadLocalRandom.current().nextDouble(RANDOM_OFFSET_MIN, RANDOM_OFFSET_MAX);
        event.onGround = false;
        event.y = event.y + randomOffset;
    }

    private boolean shouldSendPacketCrit(Entity target) {
        return target.hurtResistantTime >= 18;
    }

    private void sendPacketCrit() {
        double posX = mc.thePlayer.posX;
        double posY = mc.thePlayer.posY;
        double posZ = mc.thePlayer.posZ;

        double randomOffset = ThreadLocalRandom.current().nextDouble(RANDOM_OFFSET_MIN, RANDOM_OFFSET_MAX);

        sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY + PACKET_CRIT_OFFSET + randomOffset, posZ, false));
        sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(posX, posY + randomOffset, posZ, false));
    }

    private boolean shouldCrit() {
        return isEnabled()
                && RotationProcessor.getInstance().getCurrentRotation() != null
                && !mc.thePlayer.isSpectator()
                && mc.thePlayer.onGround
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava()
                && !mc.thePlayer.isEntityInsideOpaqueBlock()
                && !mc.thePlayer.movementInput.jump
                && !Vanta.instance.moduleStorage.getT(Speed.class).isEnabled();
    }

    private Entity getTarget() {
        if (TargetProcessor.getInstance().killaura.isEnabled() && TargetProcessor.getInstance().target != null) {
            return TargetProcessor.getInstance().target;
        }

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null && !TargetProcessor.getInstance().killaura.isEnabled()) {
            return mc.objectMouseOver.entityHit;
        }

        return null;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}