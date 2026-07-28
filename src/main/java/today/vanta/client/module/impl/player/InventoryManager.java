package today.vanta.client.module.impl.player;

import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import today.vanta.client.event.impl.game.render.DisplayGuiScreenEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.processor.impl.TargetProcessor;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.InventoryUtil;
import today.vanta.util.system.math.Counter;
import today.vanta.util.system.math.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryManager extends Module {
    private static final long CLOSE_DELAY = 50L;

    private final NumberSetting minDelay = Setting.of("Min delay", 300, 10, 1000, "ms");
    private final NumberSetting maxDelay = Setting.of("Max delay", 300, 10, 1000, "ms");
    private final BooleanSetting inventoryOnly = Setting.of("Inventory only", true);
    private final BooleanSetting exitOnEnemy = Setting.of("Exit on enemy", true);

    private final NumberSetting startDelay = Setting.of("Initial delay", 100, 10, 1000, "ms");

    private final BooleanSetting keepSword = Setting.of("Keep swords", true);
    private final BooleanSetting keepPickaxe = Setting.of("Keep pickaxes", true);
    private final BooleanSetting keepAxe = Setting.of("Keep axes", true);
    private final BooleanSetting keepShovel = Setting.of("Keep shovels", true);
    private final BooleanSetting keepBow = Setting.of("Keep bows", true);
    private final BooleanSetting keepFood = Setting.of("Keep food", true);

    private final NumberSetting swordSlot = Setting.of("Sword slot", 1, 1, 9)
            .hide(() -> !keepSword.getValue());
    private final NumberSetting pickaxeSlot = Setting.of("Pickaxe slot", 1, 1, 9)
            .hide(() -> !keepPickaxe.getValue());
    private final NumberSetting axeSlot = Setting.of("Axe slot", 1, 1, 9)
            .hide(() -> !keepAxe.getValue());
    private final NumberSetting blockSlot = Setting.of("Block slot", 5, 1, 9);
    private final NumberSetting shovelSlot = Setting.of("Shovel slot", 4, 1, 9)
            .hide(() -> !keepShovel.getValue());
    private final NumberSetting foodSlot = Setting.of("Food slot", 6, 1, 9)
            .hide(() -> !keepFood.getValue());
    private final NumberSetting bowSlot = Setting.of("Bow slot", 9, 1, 9)
            .hide(() -> !keepBow.getValue());

    private final NumberSetting keepBlocks = Setting.of("Keep X stack of blocks", 3, 1, 9);
    private final NumberSetting keepThrowables = Setting.of("Keep throwables", 3, 1, 9);

    private final BooleanSetting humanized = Setting.of("Humanized", true);
    private final NumberSetting fittsWeight = Setting.of("Fitts weight", 15, 0, 100, "%").hide(() -> !humanized.getValue());
    private final NumberSetting hickWeight = Setting.of("Hick weight", 10, 0, 100, "%").hide(() -> !humanized.getValue());

    private final BooleanSetting whileMoving = Setting.of("While moving", false)
            .hide(() -> inventoryOnly.getValue());

    private final Counter actionTimer = new Counter();
    private final Counter startTimer = new Counter();
    private final Counter stateTimer = new Counter();

    private int lastActionSlot = -1;
    private boolean actionPerformed;
    private int actionsSinceOpen;

    private enum SilentState { CLOSED, OPENING, OPEN, CLOSING }
    private SilentState silentState = SilentState.CLOSED;

    public InventoryManager() {
        super("InventoryManager", "Automatically manages inventory.", Category.PLAYER);
        displayNames = new String[] {"InventoryManager", "InvManager", "Manager", "InventorySorter", "InvSorter", "Sorter", "Cleaner", "InvCleaner", "InventoryCleaner"};
    }

    @Override
    public void onEnable() {
        actionTimer.reset();
        lastActionSlot = -1;
        silentState = SilentState.CLOSED;
        actionsSinceOpen = 0;
    }

    @Override
    public void onDisable() {
        actionTimer.reset();
        lastActionSlot = -1;

        if (mc.thePlayer == null) return;

        closeSilentInventory();
    }

    @EventListen
    private void onDisplayGuiScreen(DisplayGuiScreenEvent event) {
        if (event.screen instanceof GuiInventory) {
            resetTimers();
        }
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (TargetProcessor.getInstance().target != null && exitOnEnemy.getValue()) {
            if (mc.currentScreen instanceof GuiInventory) {
                mc.thePlayer.closeScreen();
            }
            closeSilentInventory();
        } else if (mc.currentScreen instanceof GuiInventory) {
            manageInventory();
            resetSilentState();
            return;
        } else if (inventoryOnly.getValue()) {
            closeSilentInventory();
        } else if (!whileMoving.getValue() && isMoving()) {
            closeSilentInventory();
        } else if (isBusy()) {
            closeSilentInventory();
        }

        processSilentState();
    }

    private void processSilentState() {
        switch (silentState) {
            case CLOSED:
                if (mc.currentScreen != null) return;
                if (inventoryOnly.getValue()) return;
                if (!whileMoving.getValue() && isMoving()) return;
                if (isBusy()) return;
                if (!hasWork()) return;

                openSilentInventory();
                silentState = SilentState.OPENING;
                stateTimer.reset();
                actionsSinceOpen = 0;
                break;

            case OPENING:
                if (stateTimer.hasElapsed(startDelay.getValue().longValue())) {
                    silentState = SilentState.OPEN;
                }
                break;

            case OPEN:
                actionPerformed = false;
                manageInventory();

                if (actionPerformed) {
                    actionsSinceOpen++;
                } else if (!hasWork()) {
                    closeSilentInventory();
                    silentState = SilentState.CLOSING;
                    stateTimer.reset();
                }
                break;

            case CLOSING:
                if (stateTimer.hasElapsed(CLOSE_DELAY)) {
                    silentState = SilentState.CLOSED;
                }
                break;
        }
    }

    private void manageInventory() {
        actionPerformed = false;

        List<Integer> throwableItems = new ArrayList<>();
        List<Integer> blockItems = new ArrayList<>();
        int[] bestItems = getBestItems();

        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            Item item = stack.getItem();
            if (item instanceof ItemBlock) {
                handleBlockItem(i, blockItems);
            } else if (item instanceof ItemSnowball || item instanceof ItemEgg) {
                handleThrowableItem(i, throwableItems);
            } else if (item instanceof ItemSword) {
                handleBestItem(i, bestItems[4], keepSword);
            } else if (item instanceof ItemFood) {
                handleBestItem(i, bestItems[9], keepFood);
            } else if (item instanceof ItemPickaxe) {
                handleBestItem(i, bestItems[5], keepPickaxe);
            } else if (item instanceof ItemAxe) {
                handleBestItem(i, bestItems[6], keepAxe);
            } else if (item instanceof ItemSpade) {
                handleBestItem(i, bestItems[7], keepShovel);
            } else if (item instanceof ItemBow) {
                handleBestItem(i, bestItems[10], keepBow);
            } else {
                handleTrash(i);
            }
        }

        dropExtraStacks(blockItems, keepBlocks.getValue().intValue());
        dropExtraStacks(throwableItems, keepThrowables.getValue().intValue());
        handleArmor(bestItems);
        handleHotbarItems(bestItems);
    }

    private boolean hasWork() {
        List<Integer> throwableItems = new ArrayList<>();
        List<Integer> blockItems = new ArrayList<>();
        int[] bestItems = getBestItems();

        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            Item item = stack.getItem();
            if (item instanceof ItemBlock) {
                Block block = ((ItemBlock) item).getBlock();
                if (!InventoryUtil.canPlaceOnBlock(block)) {
                    return true;
                }
                blockItems.add(i);
            } else if (item instanceof ItemSnowball || item instanceof ItemEgg) {
                throwableItems.add(i);
            } else if (item instanceof ItemSword) {
                if (!keepSword.getValue() || (bestItems[4] != -1 && bestItems[4] != i)) return true;
            } else if (item instanceof ItemFood) {
                if (!keepFood.getValue() || (bestItems[9] != -1 && bestItems[9] != i)) return true;
            } else if (item instanceof ItemPickaxe) {
                if (!keepPickaxe.getValue() || (bestItems[5] != -1 && bestItems[5] != i)) return true;
            } else if (item instanceof ItemAxe) {
                if (!keepAxe.getValue() || (bestItems[6] != -1 && bestItems[6] != i)) return true;
            } else if (item instanceof ItemSpade) {
                if (!keepShovel.getValue() || (bestItems[7] != -1 && bestItems[7] != i)) return true;
            } else if (item instanceof ItemBow) {
                if (!keepBow.getValue() || (bestItems[10] != -1 && bestItems[10] != i)) return true;
            } else if (InventoryUtil.isTrash(item)) {
                return true;
            }
        }

        if (blockItems.size() > keepBlocks.getValue().intValue()) return true;
        if (throwableItems.size() > keepThrowables.getValue().intValue()) return true;

        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (i != bestItems[armor.armorType]) return true;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (bestItems[i] != -1 && bestItems[i] != 39 - i) return true;
        }

        if (keepSword.getValue() && bestItems[4] != -1 && bestItems[4] != swordSlot.getValue().intValue() - 1) return true;
        if (keepPickaxe.getValue() && bestItems[5] != -1 && bestItems[5] != pickaxeSlot.getValue().intValue() - 1) return true;
        if (keepAxe.getValue() && bestItems[6] != -1 && bestItems[6] != axeSlot.getValue().intValue() - 1) return true;
        if (keepShovel.getValue() && bestItems[7] != -1 && bestItems[7] != shovelSlot.getValue().intValue() - 1) return true;
        if (keepFood.getValue() && bestItems[9] != -1 && bestItems[9] != foodSlot.getValue().intValue() - 1) return true;
        if (keepBow.getValue() && bestItems[10] != -1 && bestItems[10] != bowSlot.getValue().intValue() - 1) return true;

        return bestItems[8] != -1 && bestItems[8] != blockSlot.getValue().intValue() - 1;
    }

    private int[] getBestItems() {
        return new int[]{
                InventoryUtil.getBestArmor(0),
                InventoryUtil.getBestArmor(1),
                InventoryUtil.getBestArmor(2),
                InventoryUtil.getBestArmor(3),
                InventoryUtil.getBestSword(false),
                InventoryUtil.getBestPickaxe(),
                InventoryUtil.getBestAxe(),
                InventoryUtil.getBestShovel(),
                InventoryUtil.getBestBlock(),
                InventoryUtil.getBestFood(),
                InventoryUtil.getBestBow()
        };
    }

    private void handleBlockItem(int i, List<Integer> blockItems) {
        Block block = ((ItemBlock) mc.thePlayer.inventory.getStackInSlot(i).getItem()).getBlock();
        if (!InventoryUtil.canPlaceOnBlock(block)) {
            dropItem(i);
        } else {
            blockItems.add(i);
        }
    }

    private void handleThrowableItem(int i, List<Integer> throwableItems) {
        throwableItems.add(i);
    }

    private void dropExtraStacks(List<Integer> items, int maxStacks) {
        if (items.size() <= maxStacks) {
            return;
        }

        List<Integer> sortedByStackSize = InventoryUtil.getSortedByStackSize(items);
        for (int i = maxStacks; i < sortedByStackSize.size(); i++) {
            dropItem(sortedByStackSize.get(i));
        }
    }

    private void handleBestItem(int i, int bestItem, BooleanSetting keepSetting) {
        if (!keepSetting.getValue()) {
            dropItem(i);
            return;
        }
        if (bestItem != -1 && bestItem != i) {
            dropItem(i);
        }
    }

    private void handleTrash(int i) {
        if (InventoryUtil.isTrash(mc.thePlayer.inventory.getStackInSlot(i).getItem())) {
            dropItem(i);
        }
    }

    private void handleArmor(int[] bestItems) {
        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                switch (armor.armorType) {
                    case 0:
                        if (i != bestItems[0]) dropItem(i);
                        break;
                    case 1:
                        if (i != bestItems[1]) dropItem(i);
                        break;
                    case 2:
                        if (i != bestItems[2]) dropItem(i);
                        break;
                    case 3:
                        if (i != bestItems[3]) dropItem(i);
                        break;
                }
            }
        }

        equipArmor(bestItems);
    }

    private void equipArmor(int[] bestItems) {
        if (bestItems[0] != -1 && bestItems[0] != 39) shiftClickItem(bestItems[0]);
        if (bestItems[1] != -1 && bestItems[1] != 38) shiftClickItem(bestItems[1]);
        if (bestItems[2] != -1 && bestItems[2] != 37) shiftClickItem(bestItems[2]);
        if (bestItems[3] != -1 && bestItems[3] != 36) shiftClickItem(bestItems[3]);
    }

    private void handleHotbarItems(int[] bestItems) {
        Map<BooleanSetting, Integer> hotbarItems = new HashMap<>();
        hotbarItems.put(keepSword, bestItems[4]);
        hotbarItems.put(keepPickaxe, bestItems[5]);
        hotbarItems.put(keepAxe, bestItems[6]);
        hotbarItems.put(keepShovel, bestItems[7]);
        hotbarItems.put(keepFood, bestItems[9]);
        hotbarItems.put(keepBow, bestItems[10]);

        Map<BooleanSetting, NumberSetting> hotbarSlots = new HashMap<>();
        hotbarSlots.put(keepSword, swordSlot);
        hotbarSlots.put(keepPickaxe, pickaxeSlot);
        hotbarSlots.put(keepAxe, axeSlot);
        hotbarSlots.put(keepShovel, shovelSlot);
        hotbarSlots.put(keepFood, foodSlot);
        hotbarSlots.put(keepBow, bowSlot);

        hotbarItems.forEach((setting, bestItem) -> {
            if (setting.getValue() && bestItem != -1 && bestItem != hotbarSlots.get(setting).getValue().intValue() - 1) {
                swapItem(bestItem, hotbarSlots.get(setting).getValue().intValue() - 1);
            }
        });

        if (bestItems[8] != -1 && bestItems[8] != blockSlot.getValue().intValue() - 1) {
            swapItem(bestItems[8], blockSlot.getValue().intValue() - 1);
        }
    }

    private void dropItem(int slot) {
        if (!canAct(slot)) return;
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, correctSlot(slot), 1, 4, mc.thePlayer);
    }

    private void swapItem(int slot, int targetSlot) {
        if (!canAct(slot)) return;
        mc.playerController.windowClick(0, correctSlot(slot), targetSlot, 2, mc.thePlayer);
    }

    private void shiftClickItem(int item) {
        if (!canAct(item)) return;
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, correctSlot(item), 0, 1, mc.thePlayer);
    }

    private boolean canAct(int targetSlot) {
        if (!startTimer.hasElapsed(startDelay.getValue().longValue())) return false;

        long requiredDelay = calculateHumanizedDelay(distance(lastActionSlot, targetSlot), countNonEmptySlots());

        if (!actionTimer.hasElapsed(requiredDelay)) return false;

        actionTimer.reset();
        lastActionSlot = targetSlot;
        actionPerformed = true;
        return true;
    }

    private int countNonEmptySlots() {
        int count = 0;
        for (int i = 0; i < 40; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) != null) count++;
        }
        return count;
    }

    private long calculateHumanizedDelay(int distance, int choices) {
        double min = minDelay.getValue().doubleValue();
        double max = maxDelay.getValue().doubleValue();

        if (!humanized.getValue()) {
            return (long) MathUtil.range(min, max);
        }

        double base = (min + max) / 2.0;

        double fittsComponent = Math.min(Math.log(distance + 1) / Math.log(2), 4.0);
        double hickComponent = Math.log(choices + 1) / Math.log(2);

        double fittsInfluence = fittsWeight.getValue().doubleValue() / 100.0;
        double hickInfluence = hickWeight.getValue().doubleValue() / 100.0;

        double calculatedDelay = base * (1.0 + fittsInfluence * fittsComponent + hickInfluence * hickComponent);

        double randomFactor = ThreadLocalRandom.current().nextDouble(0.9, 1.1);

        return (long) Math.max(min, Math.min(max, calculatedDelay * randomFactor));
    }

    private int distance(int slot1, int slot2) {
        if (slot2 == -1) return Integer.MAX_VALUE;
        int row1 = slot1 / 9, col1 = slot1 % 9;
        int row2 = slot2 / 9, col2 = slot2 % 9;
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
    }

    private int correctSlot(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        }
        if (slot < 9) {
            return slot + 36;
        }
        return slot;
    }

    private boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0;
    }

    private boolean isBusy() {
        return mc.playerController.getIsHittingBlock()
                || mc.thePlayer.isUsingItem()
                || mc.thePlayer.isSwingInProgress;
    }

    private void openSilentInventory() {
        mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
        resetTimers();
    }

    private void closeSilentInventory() {
        if (silentState == SilentState.CLOSED || silentState == SilentState.CLOSING) return;
        mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId));
        silentState = SilentState.CLOSING;
        stateTimer.reset();
        lastActionSlot = -1;
    }

    private void resetSilentState() {
        silentState = SilentState.CLOSED;
        lastActionSlot = -1;
    }

    private void resetTimers() {
        actionTimer.reset();
        startTimer.reset();
        lastActionSlot = -1;
    }
}
