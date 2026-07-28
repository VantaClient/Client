package today.vanta.client.module.impl.player;

import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.player.PlayerUtil;

public class AntiVoid extends Module {
    private final StringSetting mode = Setting.of("Mode", "Flag", "Blink", "Flag", "Setback");
    private final StringSetting setbackmode = Setting.of("Setback mode", "Ground", "Previous", "Ground");
    private final NumberSetting waitTime = Setting.of("Ticks until activation", 5, 0, 40, 0);
    private final BooleanSetting stopMotion = Setting.of("Stop motion", false);

    private double prevPosZ, prevPosX, prevPosY;
    private int tick;

    public AntiVoid() {
        super("AntiVoid", "teleport yes.", Category.PLAYER);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer.capabilities.isFlying || MovementUtil.movementModuleEnabled()) return;

        switch (setbackmode.getValue()) {
            case "Previous":
                if (!PlayerUtil.isOverVoid()) {
                    prevPosY = mc.thePlayer.posY;
                    prevPosX = mc.thePlayer.posX;
                    prevPosZ = mc.thePlayer.posZ;
                }
                break;
            case "Ground":
                if (mc.thePlayer.onGround && !PlayerUtil.isOverVoid()) {
                    prevPosY = mc.thePlayer.posY;
                    prevPosX = mc.thePlayer.posX;
                    prevPosZ = mc.thePlayer.posZ;
                }
                break;
        }

        if (MovementUtil.movementModuleEnabled()) {
            return;
        }

        switch (mode.getValue()) {
            case "Flag":
                if (PlayerUtil.isOverVoid() && !mc.thePlayer.onGround) {
                    tick++;
                    if (tick > waitTime.getValue().intValue()) {
                        mc.thePlayer.motionY -= 0.4f;
                        if (stopMotion.getValue()) {
                            MovementUtil.stop();
                        }
                    }
                } else {
                    tick = 0;
                }
                break;

            case "Blink":

            case "Setback":
                if (PlayerUtil.isOverVoid() && !mc.thePlayer.onGround) {
                    tick++;
                    if (tick > waitTime.getValue().intValue()) {
                        mc.thePlayer.setPosition(prevPosX, prevPosY, prevPosZ);
                        if (stopMotion.getValue()) {
                            MovementUtil.stop();
                        }
                    }
                } else {
                    tick = 0;
                }
                break;
        }
    }

    @EventListen
    private void onSendPacket(SendPacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mode.isValue("Blink") && PlayerUtil.isOverVoid() && !mc.thePlayer.onGround && !mc.thePlayer.isSpectator() && !mc.thePlayer.capabilities.isFlying) {
            event.cancelled = true;
        }
    }
}