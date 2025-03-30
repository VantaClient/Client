package today.vanta.util.game.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BlockCache {
    public final BlockPos pos;
    public final EnumFacing facing;

    public BlockCache(BlockPos pos, EnumFacing facing) {
        this.pos = pos;
        this.facing = facing;
    }

    public static BlockCache getCache(BlockPos pos) {
        if (!(Minecraft.getMinecraft().theWorld.getBlockState(pos).getBlock() instanceof BlockAir)) {
            return null; // Ensures we're only searching when in air
        }

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                for (int i = 1; i > -3; i -= 2) {
                    BlockPos checkPos = pos.add(x * i, 0, z * i);
                    if (Minecraft.getMinecraft().theWorld.getBlockState(checkPos).getBlock() instanceof BlockAir) {
                        for (EnumFacing direction : EnumFacing.values()) {
                            BlockPos block = checkPos.offset(direction);
                            Material material = Minecraft.getMinecraft().theWorld.getBlockState(block).getBlock().getMaterial();

                            if (material.isSolid() && !material.isLiquid()) {
                                return new BlockCache(block, direction.getOpposite());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static EnumFacing getFace(BlockPos pos) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos adjacent = pos.offset(facing);
            IBlockState state = Minecraft.getMinecraft().theWorld.getBlockState(adjacent);
            Block block = state.getBlock();

            if (!block.isReplaceable(Minecraft.getMinecraft().theWorld, adjacent)
                    && block.isFullBlock()
                    && block.getMaterial() != Material.air
                    && !block.isTranslucent()
                    && block.isCollidable()) {
                return facing;
            }
        }
        return null;
    }
}
