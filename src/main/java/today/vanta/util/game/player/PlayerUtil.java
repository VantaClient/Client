package today.vanta.util.game.player;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.optifine.BlockPosM;
import today.vanta.util.game.Commons;

public class PlayerUtil implements Commons {

    public static boolean isOverVoid() {
        BlockPosM position = new BlockPosM(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        for (int i = 0; i < 256; i++) {
            IBlockState state = mc.theWorld.getBlockState(new BlockPos(position.getX(), i, position.getZ()));
            if (state.getBlock() != Blocks.air)
                return false;
        }
        return true;
    }

    public static boolean isSlabUnderneath() {
        BlockPosM position = new BlockPosM(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        for (int i = 0; i < 2; i++) {
            IBlockState state = mc.theWorld.getBlockState(new BlockPos(position.getX(), position.getY() + i, position.getZ()));
            if (state.getBlock() != Blocks.wooden_slab || state.getBlock() != Blocks.stone_slab || state.getBlock() != Blocks.stone_slab)
                return false;
        }
        return true;
    }

    public static boolean isGlassUnderneath() {
        BlockPos position = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
        IBlockState state = mc.theWorld.getBlockState(position);
        return state.getBlock() == Blocks.glass;
    }

    // from Seline :sob:
    public static boolean checkIllegal(EntityLivingBase entity) {
        float length = entity.getName().length();
        if (length >= 17) {
            return true;
        }

        NetworkPlayerInfo info = mc.getNetHandler()
                .getPlayerInfo(entity.getUniqueID());
        if (info == null) return true;
        if (info.getResponseTime() == 0 && entity.getAge() == 600) return true;
        if (entity.getName().startsWith("§")) return true;

        return false;
    }

    public static int getPing(EntityLivingBase entity) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return 0;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(entity.getUniqueID());
        if (info == null) return 0;

        return info.getResponseTime();
    }
}
