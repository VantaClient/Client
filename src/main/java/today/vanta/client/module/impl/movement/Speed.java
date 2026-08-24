package today.vanta.client.module.impl.movement;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.MovementUtil;

public class Speed extends Module {
    private final StringSetting
            mode = Setting.of("Mode", "NCP", "OldNCP", "Mospixel", "NCP", "Vulcan", "Custom"),
            oncpmode = Setting.of("OldNCP mode", "Y-Port", "Y-Port", "Strafe").hide(() -> !mode.isValue("OldNCP")),
            vulcanMode = Setting.of("Vulcan mode", "Non-Prediction", "Prediction").hide(() -> !mode.getValue().equals("Vulcan"));

    private final BooleanSetting shouldjump = Setting.of("Should jump", true).hide(() -> !mode.isValue("Custom"));
    private final BooleanSetting regularJump = Setting.of("Regular jump", true).hide(() -> !shouldjump.getValue() || !mode.isValue("Custom"));
    private final NumberSetting jumpamount = Setting.of("Jump motion", 0.42f, 0.01, 2, 3).hide(() -> !shouldjump.getValue() || !mode.isValue("Custom") || regularJump.getValue());
    private final BooleanSetting strafe = Setting.of("Should strafe", true).hide(() -> !mode.isValue("Custom"));
    private final NumberSetting strafeamount = Setting.of("Strafe amount", 0, 0, 2, 2).hide(() -> !strafe.getValue() || !mode.isValue("Custom"));
    private final BooleanSetting groundstrafe = Setting.of("Should ground strafe", true).hide(() -> !mode.isValue("Custom"));
    private final NumberSetting groundstrafeamount = Setting.of("Ground strafe amount", 0.2, 0.01, 2, 2).hide(() -> !groundstrafe.getValue() || !mode.isValue("Custom"));
    private final BooleanSetting groundonstrafe = Setting.of("Ground strafe only strafe", true).hide(() -> !mode.isValue("Custom") || !groundstrafe.getValue());
    private final BooleanSetting shouldtickstrafe = Setting.of("Should tick strafe", false).hide(() -> !mode.isValue("Custom"));
    private final NumberSetting tickstrafeamount = Setting.of("Tick strafe amount", 0.2, 0.01, 2, 2).hide(() -> !shouldtickstrafe.getValue() || !mode.isValue("Custom"));
    private final BooleanSetting shouldlowhop = Setting.of("Should low-hop", false).hide(() -> !mode.isValue("Custom"));
    private final NumberSetting lowhopstrength = Setting.of("Low-hop strength", 0.2, 0.01, 10, 2).hide(() -> !shouldlowhop.getValue() || !mode.isValue("Custom")),
            lowhoptick = Setting.of("Low-hop tick", 2, 1, 50, 0).hide(() -> !shouldlowhop.getValue() || !mode.isValue("Custom"));

    public Speed() {
        super("Speed", "Makes you go faster.", Category.MOVEMENT);
        displayNames = new String[]{"Speed", "FastMove", "Zoot", "SpeedyGonzales"};
        super.setEnabled(false);
    }

    private int offGroundTicks;
    private int tick;
    private boolean flag;

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        if (flag) {
            mc.fontRendererObj.drawString("Detected flag! Ticks left: " + (60 - tick), (float) event.scaledResolution.getScaledWidth() / 2 - ((float) mc.fontRendererObj.getStringWidth("Detected flag! Ticks left: " + (60 - tick)) / 2), 400, 0xFFFFFF, true);
        }
    }

    @EventListen
    private void onReceivePacket(ReceivePacketEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (event.packet instanceof S08PacketPlayerPosLook) {
            flag = true;
        }
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (!mc.thePlayer.onGround) {
            offGroundTicks++;
        } else {
            offGroundTicks = 0;
        }

        if (flag) {
            tick++;
        }

        if (tick > 60) {
            tick = 0;
            flag = false;
        }

        if (MovementUtil.isMoving() && !mc.thePlayer.isInWater()) {
            mc.gameSettings.keyBindSprint.pressed = true;

            switch (mode.getValue()) {
                case "OldNCP":
                    switch (oncpmode.getValue()) {
                        case "Y-Port":
                            if (mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                            } else {
                                mc.thePlayer.motionY -= 0.16;
                            }
                            break;
                        case "Strafe":
                            if (mc.thePlayer.onGround) {
                                mc.thePlayer.jump();
                                MovementUtil.strafe();
                            }
                            break;
                    }
                    break;

                case "NCP":
                    if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.jump();
                    }

                    MovementUtil.strafe();

                    if (!MovementUtil.isMoving()) {
                        MovementUtil.stop();
                    }
                    break;

                case "Mospixel-Basic":
                    if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.jump();
                    }

                    if (mc.thePlayer.motionY < 0.17f && offGroundTicks > 3) {
                        mc.thePlayer.motionY -= 0.05f;
                    }
                    break;

                case "Mospixel":
                    if (flag && tick >= 60) {
                        tick = 0;
                        flag = false;
                    }
                    if (flag) {
                        return;
                    }

                    if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.jump();
                    }

                    if (offGroundTicks == 2 && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                        MovementUtil.strafe(0.48f);
                    }
                    if (offGroundTicks == 2 && !mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                        MovementUtil.strafe(0.33f);
                    } else {
                        MovementUtil.strafe();
                    }
//                    if (offGroundTicks > 7 && mc.thePlayer.motionY < 0.421f) {
//                        mc.thePlayer.motionY -= 0.05f;
//                    }

                    if (!MovementUtil.isMoving()) {
                        MovementUtil.stop();
                    }
                    break;

                case "Miniblox":
//                   switch (offGroundTicks) {
//                       case 1:
//                           mc.thePlayer.motionY -= 0.25;
//                           break;
////                       case 2:
////                           mc.thePlayer.motionY -= 0.45;
////                           break;
//                   }
//
//                    if (mc.thePlayer.isInWater() || !MovementUtil.isMoving() || mc.thePlayer.isCollidedHorizontally) return;
//                    if (onGroundTicks >= 1) {
//                        MovementUtil.strafe(0.3f);
//                        mc.thePlayer.jump();
//                    }

                    break;

                case "Vulcan":
                    switch (vulcanMode.getValue()) {
                        case "Non-Prediction":
                            if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                                mc.thePlayer.jump();
                            }

                            if (offGroundTicks == 2 && mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.thePlayer.isCollided) {
                                MovementUtil.strafe(0.48f);
                            }

                            if (offGroundTicks == 2 && !mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.thePlayer.isCollided) {
                                MovementUtil.strafe(0.3f);
                            }

                            if (offGroundTicks > 5 && mc.thePlayer.motionY < 0.421f) {
                                mc.thePlayer.motionY = -0.5f;
                            }
                            break;
                        case "Prediction":
                            if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                                mc.thePlayer.jump();
                            }

                            if (offGroundTicks == 2 && mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.thePlayer.isCollided) {
                                MovementUtil.strafe(0.45f);
                            }


                            if (offGroundTicks == 2 && !mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.thePlayer.isCollided) {
//                                MovementUtil.strafe(MovementUtil.getBaseMoveSpeed());
                            }

                            if (offGroundTicks == 5 && mc.thePlayer.motionY < 0.421f) {
                                mc.thePlayer.motionY -= 0.006f;
                            }
                            break;
                    }
                    break;

                case "Custom":
                    if (shouldjump.getValue()) {
                        if (mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown()) {
                            if (!regularJump.getValue()) {
                                mc.thePlayer.motionY += jumpamount.getValue().floatValue();
                            } else {
                                mc.thePlayer.jump();
                            }
                        }
                    }

                    if (strafe.getValue()) {
                        if (strafeamount.getValue().floatValue() == 0) {
                            MovementUtil.strafe();
                        } else {
                            MovementUtil.strafe(strafeamount.getValue().floatValue());
                        }
                    }

                    if (groundstrafe.getValue() && mc.thePlayer.onGround) {
                        if (groundonstrafe.getValue()) {
                            if (mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown()) {
                                MovementUtil.strafe(groundstrafeamount.getValue().floatValue());
                            }
                        } else {
                            MovementUtil.strafe(groundstrafeamount.getValue().floatValue());
                        }
                    }

                    if (shouldtickstrafe.getValue()) {
                        if (offGroundTicks == 2) {
                            MovementUtil.strafe(tickstrafeamount.getValue().floatValue());
                        }
                    }

                    if (shouldlowhop.getValue()) {
                        if (offGroundTicks == lowhoptick.getValue().intValue()) {
                            mc.thePlayer.motionY -= lowhopstrength.getValue().floatValue();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onDisable() {
        offGroundTicks = 0;
        tick = 0;
        flag = false;

        if (mc.theWorld == null || mc.thePlayer == null) return;

        mc.gameSettings.keyBindSprint.pressed = false;
        mc.gameSettings.keyBindJump.pressed = false;

        mc.timer.timerSpeed = 1.0f;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}