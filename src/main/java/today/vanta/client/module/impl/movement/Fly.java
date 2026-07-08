package today.vanta.client.module.impl.movement;

import io.netty.buffer.Unpooled;
import lombok.val;
import lombok.var;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.MoveEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.RotationProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.system.math.Counter;

public class Fly extends Module {
    private final StringSetting mode = Setting.of("Mode", "Vanilla", "Vanilla", "MinibloxMeme", "Teleport", "Jump");

    private final NumberSetting distance = Setting.of("TP distance", 3, 0, 10, "m").hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting ticks = Setting.of("TP ticks", 10, 1, 20).hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting viewBobbing = Setting.of("View-bob amount", 60.0f,0.0f,100f);
    private final NumberSetting timer = Setting.of("Timer", 10, 0.1, 100)
            .hide(() -> mode.isValue("Teleport"));

    private final Counter jumpCounter = new Counter();

    private double prevposY;

    public Fly() {
        super("Fly", "Allows you to fly like a pelican.", Category.MOVEMENT);
        displayNames = new String[]{"Fly", "Flight", "AirWalk", "AirJump"};
    }

    @EventListen
    private void onUpdate(UpdateEvent __) {
        if (MovementUtil.isMoving()) {
            mc.thePlayer.cameraYaw = viewBobbing.getValue().floatValue() / 1000.0F;
        }
        mc.timer.timerSpeed = timer.getValue().floatValue();
        switch (mode.getValue()) {
            case "MinibloxMeme":
                if (stage == MinibloxStage.MOVE) {
                    mc.thePlayer.motionY = 0;
                    MovementUtil.strafe(0.29998f);
                }
                break;
            case "Vanilla":
                mc.thePlayer.motionY = 0f;
                MovementUtil.strafe(1f);
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.thePlayer.motionY = 1f;
                }

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.thePlayer.motionY = -1f;
                }
                break;
            case "Jump":
                if (jumpCounter.hasElapsed(540, true)) {
                    mc.thePlayer.jump();
                }
                break;
        }
    }

    enum MinibloxStage {
        MOVE,
        FORCE_SETBACK,
        WAIT
    }

    private MinibloxStage stage = MinibloxStage.MOVE;

    private void sendForceSetback(MoveEvent event) {
        val packetbuffer = new PacketBuffer(Unpooled.buffer());
        packetbuffer.writeDouble(mc.thePlayer.posX);
        packetbuffer.writeDouble(prevposY);
        packetbuffer.writeDouble(mc.thePlayer.posZ);
        packetbuffer.writeFloat(mc.thePlayer.rotationYaw);
        packetbuffer.writeFloat(mc.thePlayer.rotationPitch);
        packetbuffer.writeFloat(mc.thePlayer.movementInput.moveForward);
        packetbuffer.writeFloat(mc.thePlayer.movementInput.moveStrafe);
        packetbuffer.writeBoolean(true);
        packetbuffer.writeBoolean(false);
        packetbuffer.writeBoolean(true);
        mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload("miniblox:movepacket", packetbuffer));
        event.y = 0;
        event.setSpeed(0);
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state == EventState.PRE) {
            if (mode.isValue("Teleport")) {
                mc.thePlayer.motionY = 0;

                if ((mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0)
                        && mc.thePlayer.ticksExisted % ticks.getValue().intValue() == 0) {

                    double distance = this.distance.getValue().intValue();

                    mc.thePlayer.setPosition(
                            mc.thePlayer.posX - Math.sin(Math.toRadians(mc.thePlayer.rotationYaw)) * distance,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)) * distance
                    );
                }
            }
        }
    }

    @EventListen
    private void onMove(MoveEvent event) {
        if (mode.isValue("Teleport")) event.setSpeed(0);
        if (mode.isValue("MinibloxMeme")) {
            switch (this.stage) {
                case MOVE:
                    stage = MinibloxStage.FORCE_SETBACK;
                    break;
                case WAIT:
                    sendForceSetback(event);
                    break;
                case FORCE_SETBACK:
                    sendForceSetback(event);
                    stage = MinibloxStage.WAIT;
                    break;
            }
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        prevposY = mc.thePlayer.posY;

    }

    @EventListen
    private void onPacket(ReceivePacketEvent e) {
        if (e.packet instanceof S08PacketPlayerPosLook && mode.isValue("MinibloxMeme") && stage == MinibloxStage.WAIT) {
            stage = MinibloxStage.MOVE;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;
        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}