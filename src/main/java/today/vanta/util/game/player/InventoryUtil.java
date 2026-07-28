package today.vanta.util.game.player;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.util.DamageSource;
import today.vanta.util.game.Commons;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InventoryUtil implements Commons {
    private static final List<Object> INVALID_ITEMS = Collections.unmodifiableList(Arrays.asList(
            Blocks.sand, Blocks.gravel, Blocks.dispenser, Blocks.command_block, Blocks.noteblock, Blocks.furnace, Blocks.crafting_table, Blocks.tnt,
            Blocks.dropper, Blocks.beacon, Blocks.vine, Blocks.soul_sand, Blocks.snow, Blocks.ice, Blocks.pumpkin,
            Blocks.air, Blocks.water, Blocks.lava, Blocks.flowing_water, Blocks.flowing_lava, Blocks.command_block, Blocks.chest, Blocks.crafting_table,
            Blocks.enchanting_table, Blocks.furnace, Blocks.noteblock, Blocks.torch, Blocks.redstone_torch, Blocks.web, Blocks.carpet, Blocks.nether_brick_fence,
            Blocks.oak_fence, Blocks.acacia_fence, Blocks.birch_fence, Blocks.jungle_fence, Blocks.dark_oak_fence, Blocks.spruce_fence, Blocks.oak_fence_gate,
            Blocks.acacia_fence_gate, Blocks.birch_fence_gate, Blocks.jungle_fence_gate, Blocks.dark_oak_fence_gate, Blocks.spruce_fence_gate, Blocks.torch,
            Blocks.redstone_torch, Blocks.stone_slab, Blocks.stone_slab2, Blocks.wooden_slab, Blocks.snow_layer, Blocks.ladder, Blocks.sapling, Blocks.vine,
            Blocks.tallgrass, Blocks.waterlily, Blocks.deadbush, Blocks.redstone_wire, Blocks.chest, Blocks.ender_chest, Blocks.trapped_chest, Blocks.double_plant,
            Blocks.flower_pot, Blocks.red_flower, Blocks.yellow_flower, Blocks.skull, Blocks.farmland, Blocks.standing_sign, Blocks.wall_sign,

            Items.stick, Items.flint, Items.feather, Items.string, Items.bone, Items.rotten_flesh, Items.spider_eye, Items.poisonous_potato, Items.pumpkin_seeds,
            Items.melon_seeds, Items.wheat_seeds, Items.sugar, Items.paper, Items.leather, Items.clay_ball, Items.ghast_tear, Items.glass_bottle,
            Items.carrot, Items.potato, Items.golden_horse_armor, Items.iron_horse_armor, Items.diamond_horse_armor, Items.saddle, Items.wooden_hoe, Items.stone_hoe,
            Items.milk_bucket, Items.snowball, Items.egg
    ));

    public static void stealSlot(int slot) {
        performClick(mc.thePlayer.openContainer, slot, 1, 1, mc.thePlayer);
    }

    public static boolean isBlockValid(Block block) {
        return !INVALID_ITEMS.contains(block);
    }

    public static int armorProt(ItemArmor armor, ItemStack item) {
        return armor.damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(new ItemStack[]{item}, DamageSource.generic);
    }

    public static void performClick(Container container, int slot, int button, int action, EntityPlayerSP player) {
        mc.playerController.windowClick(container.windowId, slot, button, action, player);
    }

    public static Slot getSlot(int index) {
        return mc.thePlayer.inventoryContainer.getSlot(index);
    }

    public static int getBestAxe() {
        return getBestToolAgainstBlock(Blocks.planks, false);
    }

    public static int getBestPickaxe() {
        return getBestToolAgainstBlock(Blocks.stone, false);
    }

    public static int getBestShovel() {
        return getBestToolAgainstBlock(Blocks.dirt, false);
    }

    public static boolean isTrash(Item item) {
        return INVALID_ITEMS.contains(item);
    }

    public static void windowClick(Minecraft mc, int windowId, int slotId, int mouseButtonClicked, ClickType type) {
        mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, type.ordinal(), mc.thePlayer);
    }

    public enum ClickType {
        PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL
    }

    public static boolean canPlaceOnBlock(final Block block) {
        return !INVALID_ITEMS.contains(block);
    }

    public static ItemStack getHeldItem() {
        InventoryPlayer inventory = mc.thePlayer.inventory;
        int currentSlot = inventory.currentItem;
        return currentSlot >= 0 && currentSlot < 9 ? inventory.mainInventory[currentSlot] : null;
    }

    public static boolean isHolding(Class<? extends Item> itemType) {
        ItemStack heldItem = getHeldItem();
        return heldItem != null && itemType.isInstance(heldItem.getItem());
    }

    public static boolean isValidChest(GuiChest chestGui) {
        Container container = chestGui.inventorySlots;
        if (container instanceof net.minecraft.inventory.ContainerChest) {
            ContainerChest containerChest = (ContainerChest) container;
            IInventory chestInventory = containerChest.getLowerChestInventory();
            String chestTitle = chestInventory.getName();
            return chestTitle != null && Stream.of("Chest", "Large Chest", "Ender Chest").anyMatch(chestTitle::contains);
        }
        return false;
    }

    public static boolean isInventoryFull() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = getSlot(i).getStack();
            if (stack == null) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> getNonEmptySlots(IInventory inventory) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (inventory.getStackInSlot(i) != null) {
                slots.add(i);
            }
        }
        return slots;
    }

    public static void switchToValidBlock() {
        ItemStack currentStack = getHeldItem();
        if (currentStack != null && currentStack.getItem() instanceof ItemBlock) {
            ItemBlock itemBlock = (ItemBlock) currentStack.getItem();
            if (isBlockValid(itemBlock.getBlock())) {
                return;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0 && stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                if (isBlockValid(itemBlock.getBlock())) {
                    mc.thePlayer.inventory.currentItem = i;
                    return;
                }
            }
        }
    }

    public static int getLeatherArmorColor(EntityPlayer player) {
        int armorColor = -1;
        for (ItemStack itemStack : player.inventory.armorInventory) {
            if (itemStack != null && itemStack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) itemStack.getItem();
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
                    if (itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey("display", 10)) {
                        armorColor = itemStack.getTagCompound().getCompoundTag("display").getInteger("color");
                        break;
                    }
                }
            }
        }
        return armorColor;
    }

    public static int getBestSword(boolean hotbar) {
        float bestDamage = -1;
        float bestQuality = -1;
        int bestSlot = -1;

        for (int i = 0; i < (hotbar ? 9 : 36); i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack != null && stack.getItem() instanceof ItemSword) {
                ItemSword sword = (ItemSword) stack.getItem();
                int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
                float damage = sword.getDamageVsEntity() + level * 1.25f;

                if (bestDamage < damage) {
                    bestDamage = damage;
                    bestQuality = sword.getDamageVsEntity();
                    bestSlot = i;
                }

                if (damage == bestDamage && sword.getDamageVsEntity() < bestQuality) {
                    bestQuality = sword.getDamageVsEntity();
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static int getBestToolAgainstBlock(Block blockPos, boolean hotbar) {
        float bestSpeed = 1F;
        int bestSlot = -1;

        for (int i = 0; i < (hotbar ? 9 : 36); i++) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);

            if (item != null) {
                float speed = item.getStrVsBlock(blockPos);

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static List<Integer> getSortedByStackSize(List<Integer> list) {
        return list.stream().sorted(Comparator.comparingInt(i -> {
            Optional<ItemStack> optionalStack = Optional.ofNullable(mc.thePlayer.inventory.getStackInSlot(i));
            return optionalStack.map(stack -> -stack.stackSize).orElse(0);
        })).collect(Collectors.toList());
    }

    public static int getBestBlock() {
        int bestBlockCount = -1;
        int bestSlot = -1;
        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                ItemBlock block = (ItemBlock) stack.getItem();
                if (canPlaceOnBlock(block.getBlock())) {
                    if (stack.stackSize > bestBlockCount) {
                        bestBlockCount = stack.stackSize;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }

    public static int getBestArmor(int armorType) {
        float bestProt = -1;
        int bestSlot = -1;
        for (int i = 0; i < 40; i++) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
            if (item == null) continue;
            if (item.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item.getItem();
                if (armor.armorType == armorType) {
                    float armorProt = armorProt(armor, mc.thePlayer.inventory.getStackInSlot(i));
                    if (armorProt > bestProt) {
                        bestProt = armorProt;
                        bestSlot = i;
                    }
                }
            }
        }
        return bestSlot;
    }

    public static int getBestFood() {
        float bestFood = -1;
        int bestSlot = -1;
        for (int i = 0; i < 40; i++) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
            if (item != null && item.getItem() instanceof ItemFood) {
                ItemFood food = (ItemFood) item.getItem();
                float foodval = food.getSaturationModifier(item);
                if (bestFood < foodval) {
                    bestFood = foodval;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static int getBestBow() {
        float bestDmg = -1;
        int bestSlot = -1;
        for (int i = 0; i < 40; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);
            if (itemStack != null && itemStack.getItem() instanceof ItemBow) {
                int powLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack);
                int punchLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack);
                int flameLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack);

                float dmg = 1 + powLevel * 0.5f + punchLevel * 5 + flameLevel * 2;
                if (dmg > bestDmg) {
                    bestSlot = i;
                    bestDmg = dmg;
                }
            }
        }
        return bestSlot;
    }

    public static void switchToNextSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int nextSlot = -1;
        int currentBlockCount = getBlockCount(currentSlot);

        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;

            int slotBlockCount = getBlockCount(i);
            if ((slotBlockCount > currentSlot) || (currentBlockCount == 0 && slotBlockCount > 0)) {
                nextSlot = i;
                break;
            }
        }

        if (nextSlot != -1) {
            mc.thePlayer.inventory.currentItem = nextSlot;
        }
    }

    public static int getBlockCount(int slot) {
        ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(slot);

        if (itemStack != null && itemStack.getItem() instanceof ItemBlock && !INVALID_ITEMS.contains(((ItemBlock) itemStack.getItem()).getBlock())) {
            return itemStack.stackSize;
        }

        return 0;
    }

    public static int getHotbarBlockCount() {
        int total = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack != null && stack.getItem() instanceof ItemBlock && !INVALID_ITEMS.contains(((ItemBlock) stack.getItem()).getBlock())) {
                total += stack.stackSize;
            }
        }
        return total;
    }

    public static ItemStack getBestBlockStack() {
        ItemStack currentStack = mc.thePlayer.inventory.getCurrentItem();

        if (currentStack != null && currentStack.getItem() instanceof ItemBlock) {
            return currentStack;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return stack;
            }
        }

        return null;
    }
}