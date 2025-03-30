package today.vanta.client.module.impl.player;

import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.RunTickEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.SprintEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.events.EventPriority;
import today.vanta.util.game.events.EventState;
import today.vanta.util.game.player.DistanceCounter;
import today.vanta.util.game.player.InventoryUtil;
import today.vanta.util.game.player.MovementUtil;
import today.vanta.util.game.player.RotationUtil;
import today.vanta.util.game.player.constructors.Rotation;
import today.vanta.util.game.world.BlockCache;
import today.vanta.util.system.math.Counter;

import java.util.Random;

public class Scaffold extends Module {
    private final StringSetting
    rotationMode = StringSetting.builder()
            .name("Rotation mode")
            .value("Simple")
            .values("Simple", "Godbridge")
            .build(),

    itemSwitchMode = StringSetting.builder()
            .name("Item spoof")
            .value("Switch")
            .values("Switch", "None")
            .build(),

    towerMode = StringSetting.builder()
            .name("Tower mode")
            .value("Jump")
            .values("Jump", "Motion")
            .build(),

    sprintMode = StringSetting.builder()
            .name("Sprint mode")
            .value("Manual")
            .values("Manual", "None", "Always")
            .build();

    private final BooleanSetting sneak = BooleanSetting.builder()
            .name("Sneak")
            .value(false)
            .build()
            .hide(() -> rotationMode.getValue().equals("Godbridge"));

    private final StringSetting sneakMode = StringSetting.builder()
            .name("Sneak mode")
            .value("Eagle")
            .values("Eagle", "Blatant", "Always")
            .build()
            .hide(() -> !sneak.getValue());

    private final NumberSetting
    sneakDelay = NumberSetting.builder()
            .name("Sneak delay (ms)")
            .value(57)
            .min(0)
            .max(300)
            .build()
            .hide(() -> !sneak.getValue() || !(sneakMode.getValue().equals("Eagle") || sneakMode.getValue().equals("Blatant"))),

    unSneakDelay = NumberSetting.builder()
            .name("Unsneak delay (ms)")
            .value(299)
            .min(0)
            .max(300)
            .build()
            .hide(() -> !sneak.getValue() || !(sneakMode.getValue().equals("Eagle") || sneakMode.getValue().equals("Blatant")));

    private final BooleanSetting
    keepY = BooleanSetting.builder()
            .name("Keep Y")
            .value(false)
            .build()
            .hide(() -> rotationMode.getValue().equals("Godbridge")),

    downwards = BooleanSetting.builder()
            .name("Downwards")
            .value(false)
            .build()
            .hide(() -> rotationMode.getValue().equals("Godbridge"));

    private final DistanceCounter distCounter = new DistanceCounter();
    private int targetDistance = 7;

    private final Counter unSneakCounter = new Counter(), sneakCounter = new Counter();

    private Rotation lastRots, rots;
    private double posY;

    public Scaffold() {
        super("Scaffold", "Bridges for you.", Category.PLAYER);
        displayNames = new String[]{"Scaffold", "ScaffoldWalk", "Scaffold Walk", "BlockFly", "Block Fly"};

        rotationMode.addListener((setting, oldValue, newValue) -> {
            if (newValue.equals("Godbridge")) {
                keepY.setValue(false);
                downwards.setValue(false);
                sneak.setValue(false);
            }
        });

        keepY.addListener((setting, oldValue, newValue) -> {
            if (newValue) {
                downwards.setValue(false);
            }
        });

        downwards.addListener((setting, oldValue, newValue) -> {
            if (newValue) {
                keepY.setValue(false);
            }
        });
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onRunTick(RunTickEvent event) {
        if (mc.thePlayer != null && event.state == EventState.PRE) {
            if (sprintMode.getValue().equals("Always")) {
                mc.gameSettings.keyBindSprint.pressed = true;
            }

            if (mc.thePlayer.getHeldItem() == null) {
                switch (itemSwitchMode.getValue()) {
                    case "Switch":
                        InventoryUtil.switchToNextSlot();
                        break;
                }
            }

            if (!MovementUtil.isMoving() && mc.gameSettings.keyBindJump.isKeyDown() && !mc.gameSettings.keyBindJump.isPressed()) {
                switch (towerMode.getValue()) {
                    case "Motion":
                        mc.thePlayer.motionY = 0.42;
                        break;
                }
            }

            if (rotationMode.getValue().equals("Godbridge")) {
                distCounter.tick(mc.thePlayer);
                if (distCounter.getTravelled() >= targetDistance) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    distCounter.reset();
                    targetDistance = 7 + new Random().nextInt(3);
                }
            } else if (sneak.getValue() && sneakMode.getValue().equals("Blatant")) {
                if (unSneakCounter.hasElapsed(unSneakDelay.getValue().longValue(), true)) {
                    mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
                }
                distCounter.tick(mc.thePlayer);
                if (distCounter.getTravelled() >= 1) {
                    if (sneakCounter.hasElapsed(sneakDelay.getValue().longValue(), true)) {
                        mc.gameSettings.keyBindSneak.pressed = true;
                    }
                    distCounter.reset();
                }
            }

            if (downwards.getValue()) {
                posY = mc.thePlayer.posY - 1.8;
            } else if (mc.thePlayer.posY < posY || (!mc.thePlayer.onGround && !MovementUtil.isMoving()) || mc.thePlayer.posY - posY > 6 || !keepY.getValue()) {
                posY = mc.thePlayer.posY - 0.9;
            }

            BlockPos playerBlockPos = new BlockPos(mc.thePlayer.posX, posY, mc.thePlayer.posZ);
            Vanta.instance.processorStorage.getT(TargetProcessor.class).cache = BlockCache.getCache(playerBlockPos);
        }
    }

    @EventListen
    private void onMotion(MotionEvent event) {
        if (event.state.equals(EventState.PRE)) {
            if (sneak.getValue()) {
                switch (sneakMode.getValue()) {
                    case "Eagle":
                        if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround) {
                            if (sneakCounter.hasElapsed(sneakDelay.getValue().longValue(), true)) {
                                mc.gameSettings.keyBindSneak.pressed = true;
                            }
                        } else {
                            if (unSneakCounter.hasElapsed(unSneakDelay.getValue().longValue(), true)) {
                                mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
                            }
                        }
                        break;
                    case "Always":
                        mc.gameSettings.keyBindSneak.pressed = true;
                        break;
                }
            }

            if (mc.thePlayer != null && mc.thePlayer.getHeldItem() != null && Vanta.instance.processorStorage.getT(TargetProcessor.class).cache != null) {
                ItemStack heldItemStack = mc.thePlayer.getHeldItem();

                if (rots != null && heldItemStack != null) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItemStack, Vanta.instance.processorStorage.getT(TargetProcessor.class).cache.pos, Vanta.instance.processorStorage.getT(TargetProcessor.class).cache.facing, new Vec3(Vanta.instance.processorStorage.getT(TargetProcessor.class).cache.pos))) {
                        mc.thePlayer.swingItem();
                    }
                }
            }
        }

        if (Vanta.instance.processorStorage.getT(TargetProcessor.class).cache != null && lastRots != null) {
            switch (rotationMode.getValue()) {
                case "Simple":
                    rots = RotationUtil.getSimpleRotations(Vanta.instance.processorStorage.getT(TargetProcessor.class).cache, lastRots);
                    break;

                case "Godbridge":
                    rots = RotationUtil.getGodbridgeRotations(Vanta.instance.processorStorage.getT(TargetProcessor.class).cache, lastRots);
                    break;
            }

            setRotations(rots, event);
            lastRots = new Rotation(event.yaw, event.pitch);
        } else {
            if (lastRots != null) {
                rots = lastRots;
                setRotations(rots, event);
                lastRots = new Rotation(event.yaw, event.pitch);
            }
        }
    }

    @EventListen
    private void onSprint(SprintEvent event) {
        if (sprintMode.getValue().equals("None") && rots != null) {
            event.cancelled = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));

        distCounter.reset();
        unSneakCounter.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.thePlayer == null) {
            return;
        }

        lastRots = new Rotation(RotationUtil.getAdjustedYaw(), 80.7f);
        posY = mc.thePlayer.posY - 0.9;

        switch (itemSwitchMode.getValue()) {
            case "Switch":
                InventoryUtil.switchToNextSlot();
                break;
        }
    }

    @Override
    public String getSuffix() {
        return rotationMode.getValue();
    }
}
