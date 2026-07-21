package today.vanta.client.module.impl.movement;

import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.MoveEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.InventoryUtil;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.system.math.Counter;

public class Fly extends Module {
    private final StringSetting mode = Setting.of("Mode", "Vanilla", "Vanilla", "Miniblox", "Teleport", "Jump", "AirPlace");
    private final NumberSetting speed = Setting.of("Speed", 2f, 0.1f,10f,1).hide(() -> !mode.isValue("Vanilla"));

    private final NumberSetting distance = Setting.of("TP distance", 3, 0, 10, "m").hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting ticks = Setting.of("TP ticks", 10, 1, 20).hide(() -> !mode.isValue("Teleport"));
    private final NumberSetting viewBobbing = Setting.of("View-bob amount", 0.8f, 0.0f, 1f, 1);

    private final Counter jumpCounter = new Counter();

    private double prevposY;

    public Fly() {
        super("Fly", "Allows you to fly like a pelican.", Category.MOVEMENT);
        displayNames = new String[]{"Fly", "Flight", "AirWalk", "AirJump"};
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        if (viewBobbing.getValue().floatValue() != 0) {
            mc.thePlayer.cameraYaw = viewBobbing.getValue().floatValue() / 10;
        }
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        switch (mode.getValue()) {
            case "Vanilla":
                mc.thePlayer.motionY = 0f;
                MovementUtil.strafe(speed.getValue().floatValue());
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.thePlayer.motionY = speed.getValue().floatValue() / 2;
                }

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.thePlayer.motionY = -speed.getValue().floatValue() / 2;
                }
                break;
            case "Jump":
                if (jumpCounter.hasElapsed(550, true)) {
                    mc.thePlayer.jump();
                }
                break;
            case "AirPlace":
                if (jumpCounter.hasElapsed(550, true)) {
                    for (int i = 0; i < 9; i++) {
                        int count = InventoryUtil.getBlockCount(i);
                        if (count > 0) {
                            break; // Stop searching once found
                        } else {
                            InventoryUtil.switchToNextSlot();
                        }
                    }

                    BlockPos below = new BlockPos(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY + mc.thePlayer.motionY,
                            mc.thePlayer.posZ
                    );

                    sendPacket(new C08PacketPlayerBlockPlacement(
                            below,
                            1, // Direction: UP (places block on top of "below", which is under you)
                            mc.thePlayer.getCurrentEquippedItem(),
                            0.5F, 1.0F, 0.5F
                    ));
                }
                break;
        }
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state == EventState.PRE) {
            switch (mode.getValue()) {
                case "Teleport":
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
                    break;

                case "Miniblox":
                    MovementUtil.strafe(0.15f);
                    if (mc.thePlayer.posY <= prevposY) {
                        mc.thePlayer.jump();
                    }
                    break;
            }
        }
    }

    @EventListen
    private void onMove(MoveEvent event) {
        if (mode.isValue("Teleport")) {
            event.setSpeed(0);
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        prevposY = mc.thePlayer.posY;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}