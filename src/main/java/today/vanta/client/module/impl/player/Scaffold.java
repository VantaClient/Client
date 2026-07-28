package today.vanta.client.module.impl.player;

import net.minecraft.block.BlockAir;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.RunTickEvent;
import today.vanta.client.event.impl.game.network.SendPacketEvent;
import today.vanta.client.event.impl.game.player.MotionEvent;
import today.vanta.client.event.impl.game.player.SprintEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.movement.Speed;
import today.vanta.client.processor.impl.RotationProcessor;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
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
            rotationMode = Setting.of("Rotation mode", "Simple", "Simple", "Godbridge", "Static", "Forward", "Sideways", "None"),
            itemSwitchMode = Setting.of("Item spoof", "Switch", "Switch", "Spoof", "None"),
            towerMode = Setting.of("Tower mode", "Jump", "Jump", "Motion", "Low"),
            sprintMode = Setting.of("Sprint mode", "Manual", "None", "Always");

    private final BooleanSetting sneak = Setting.of("Sneak", false).hide(() -> rotationMode.isValue("Godbridge"));
    private final StringSetting sneakMode = Setting.of("Sneak mode", "Eagle", "Eagle", "Blatant", "Always").hide(() -> !sneak.getValue());

    private final NumberSetting
            sneakDelay = Setting.of("Sneak delay", 57, 0, 300, "ms")
            .hide(() -> !sneak.getValue() || !(sneakMode.isValue("Eagle") || sneakMode.isValue("Blatant"))),
            unSneakDelay = Setting.of("Unsneak delay", 299, 0, 300, "ms"
            ).hide(() -> !sneak.getValue() || !(sneakMode.isValue("Eagle") || sneakMode.isValue("Blatant")));

    private final BooleanSetting
            keepY = Setting.of("Keep Y", false).hide(() -> rotationMode.isValue("Godbridge")),
            speedKeepY = Setting.of("Keep Y on speed", false).hide(() -> rotationMode.isValue("Godbridge") || keepY.getValue()),
            downwards = Setting.of("Downwards", false).hide(() -> rotationMode.isValue("Godbridge")),
            smoothRotations = Setting.of("Smooth rotations", false).hide(() -> rotationMode.isValue("Godbridge")),
            miniblox = Setting.of("Miniblox expand", false);

    private final DistanceCounter distCounter = new DistanceCounter();
    private int targetDistance = 7;
    private int tick;

    private final Counter unSneakCounter = new Counter(), sneakCounter = new Counter();

    private Rotation lastRots, rots;
    private double posY;
    private int lastSlot = -1;

    public Scaffold() {
        super("Scaffold", "Bridges for you.", Category.PLAYER);
        displayNames = new String[]{"Scaffold", "ScaffoldWalk", "BlockFly"};

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
                speedKeepY.setValue(false);
            }
        });

        downwards.addListener((setting, oldValue, newValue) -> {
            if (newValue) {
                keepY.setValue(false);
            }
        });
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (!mc.thePlayer.onGround) {
            tick++;
        } else {
            tick = 0;
        }
    }

    @EventListen(priority = EventPriority.HIGHEST)
    private void onRunTick(RunTickEvent event) {
        if (mc.thePlayer != null && event.state == EventState.PRE) {
            if (InventoryUtil.getHotbarBlockCount() == 0) {
                this.setEnabled(false);
                return;
            }

            if (sprintMode.isValue("Always")) {
                mc.gameSettings.keyBindSprint.pressed = true;
            }

            if (itemSwitchMode.isValue("Spoof")) {
                int blockSlot = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                    if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 0 && ((ItemBlock) stack.getItem()).getBlock().isBlockNormalCube()) {
                        blockSlot = i;
                        break;
                    }
                }

                if (blockSlot != -1 && lastSlot != blockSlot) {
                    lastSlot = blockSlot;
                    if (mc.thePlayer.inventory.currentItem != blockSlot) {
                        sendPacket(new C09PacketHeldItemChange(blockSlot));
                    }
                }
            } else if (mc.thePlayer.getHeldItem() == null) {
                switch (itemSwitchMode.getValue()) {
                    case "Switch":
                        InventoryUtil.switchToNextSlot();
                        break;
                }
            }

            if (!MovementUtil.isMoving() && mc.gameSettings.keyBindJump.isKeyDown() && !mc.gameSettings.keyBindJump.isPressed()) {
                switch (towerMode.getValue()) {
                    case "Motion":
                        mc.thePlayer.motionY = 0.5;
                        break;
                    case "Low":
                        if (tick < 2) {
                            mc.thePlayer.motionY = 0.4198499917984999;
                        }
                        if (tick > 3) {
                            mc.thePlayer.motionY -= 0.06f;
                            tick = 0;
                        }
                        break;
                }
            }

            if (rotationMode.isValue("Godbridge")) {
                distCounter.tick(mc.thePlayer);
                if (distCounter.getTravelled() >= targetDistance) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    distCounter.reset();
                    targetDistance = 7 + new Random().nextInt(3);
                }
            } else if (sneak.getValue() && sneakMode.isValue("Blatant")) {
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

            if (downwards.getValue() && mc.gameSettings.keyBindSneak.isKeyDown()) {
                posY = mc.thePlayer.posY - 1.8;
            } else if (mc.thePlayer.posY < posY || (!mc.thePlayer.onGround && !MovementUtil.isMoving()) || mc.thePlayer.posY - posY > 6 || !shouldKeepY()) {
                posY = mc.thePlayer.posY - 0.9;
            }

            BlockPos playerBlockPos = new BlockPos(mc.thePlayer.posX, posY, mc.thePlayer.posZ);
            if (miniblox.getValue()) {
                TargetProcessor.getInstance().cache = BlockCache.getCache(playerBlockPos, mc.thePlayer.rotationYaw);
            } else {
                TargetProcessor.getInstance().cache = BlockCache.getCache(playerBlockPos);
            }

            if (TargetProcessor.getInstance().cache != null && lastRots != null) {
                switch (rotationMode.getValue()) {
                    case "None":
                        rots = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        break;
                    case "Simple":
                        rots = smoothRotations.getValue() ? RotationUtil.getSimpleRotations(TargetProcessor.getInstance().cache, lastRots) : RotationUtil.getSimpleRotations(TargetProcessor.getInstance().cache);
                        break;

                    case "Godbridge":
                        rots = RotationUtil.getGodbridgeRotations(TargetProcessor.getInstance().cache, lastRots);
                        break;

                    case "Static":
                        rots = RotationUtil.getStaticRotations(TargetProcessor.getInstance().cache, lastRots);
                        break;

                    case "Forward":
                        rots = RotationUtil.getForwardRotations(TargetProcessor.getInstance().cache, lastRots);
                        break;

                    case "Sideways":
                        rots = RotationUtil.getSidewaysRotations();
                        break;
                }

                RotationProcessor.getInstance().setTargetRotation(rots);
                lastRots = rots;
            } else if (lastRots != null) {
                rots = lastRots;
                RotationProcessor.getInstance().setTargetRotation(rots);
            }
        }
    }

    @EventListen(priority = EventPriority.HIGHEST)
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

            if (mc.thePlayer != null && TargetProcessor.getInstance().cache != null) {
                ItemStack heldItemStack = mc.thePlayer.getHeldItem();

                if (itemSwitchMode.isValue("Spoof") && lastSlot != -1) {
                    heldItemStack = mc.thePlayer.inventory.getStackInSlot(lastSlot);
                }

                if (rots != null && heldItemStack != null && heldItemStack.getItem() instanceof ItemBlock) {
                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItemStack, TargetProcessor.getInstance().cache.pos, TargetProcessor.getInstance().cache.facing, new Vec3(TargetProcessor.getInstance().cache.pos))) {
                        mc.thePlayer.swingItem();
                    }
                }
            }
        }
    }

    @EventListen
    private void onSendPacket(SendPacketEvent event) {
        if (mc.thePlayer == null) return;
        if (itemSwitchMode.isValue("Spoof") && event.packet instanceof C09PacketHeldItemChange) {
            C09PacketHeldItemChange packet = (C09PacketHeldItemChange) event.packet;
            if (packet.getSlotId() != lastSlot) {
                event.cancelled = true;
            }
        }
    }

    @EventListen
    private void onSprint(SprintEvent event) {
        if (sprintMode.isValue("None") && rots != null) {
            event.cancelled = true;
        }
    }

    private boolean shouldKeepY() {
        boolean speedEnabled = Vanta.instance.moduleStorage.getT(Speed.class).isEnabled();

        if (speedKeepY.getValue()) {
            return speedEnabled;
        }

        return keepY.getValue();
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));

        distCounter.reset();
        unSneakCounter.reset();

        rots = null;
        lastRots = null;
        lastSlot = -1;

        if (mc.thePlayer == null) return;
        if (itemSwitchMode.isValue("Spoof") && lastSlot != -1 && lastSlot != mc.thePlayer.inventory.currentItem) {
            sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        }
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        if (InventoryUtil.getHotbarBlockCount() == 0) {
            this.setEnabled(false);
            return;
        }

        lastRots = new Rotation(RotationUtil.getAdjustedYaw(), 80.7f);
        posY = mc.thePlayer.posY - 0.9;
        lastSlot = -1;

        if (itemSwitchMode.isValue("Spoof")) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 0) {
                    lastSlot = i;
                    if (mc.thePlayer.inventory.currentItem != i) {
                        sendPacket(new C09PacketHeldItemChange(i));
                    }
                    break;
                }
            }
        } else if (itemSwitchMode.isValue("Switch")) {
            InventoryUtil.switchToNextSlot();
        }
    }

    @Override
    public String getSuffix() {
        return rotationMode.getValue();
    }
}