package today.vanta.util.game.player;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import today.vanta.util.game.IMinecraft;

public class InventoryUtil implements IMinecraft {
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

        if (itemStack != null && itemStack.getItem() instanceof ItemBlock) {
            return itemStack.stackSize;
        }

        return 0;
    }
}
