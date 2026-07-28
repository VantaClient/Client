package today.vanta.util.game.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

public class BlockCache {
    // claude did some stuff here
    public final BlockPos pos;
    public final EnumFacing facing;

    // how many blocks ahead of the player's position to search from
    private static final double FORWARD_OFFSET = 2.0;

    public BlockCache(BlockPos pos, EnumFacing facing) {
        this.pos = pos;
        this.facing = facing;
    }

    /**
     * Original entry point - searches starting at the given pos with no forward offset.
     */
    public static BlockCache getCache(BlockPos pos) {
        return getCache(pos, Float.NaN);
    }

    /**
     * Expanded entry point - if yaw is a valid float, the search origin is shifted
     * FORWARD_OFFSET blocks in the direction the player is looking (computed from yaw
     * rather than snapped to a cardinal EnumFacing), so scaffolding targets 2 blocks
     * ahead of the player at any rotation, not just when facing exactly N/S/E/W.
     */
    public static BlockCache getCache(BlockPos pos, float yaw) {
        BlockPos origin = pos;

        if (!Float.isNaN(yaw)) {
            double rad = Math.toRadians(yaw);
            // Minecraft yaw: 0 = south (+Z), 90 = west (-X), forward vector below matches that convention
            double dx = -MathHelper.sin((float) rad);
            double dz = MathHelper.cos((float) rad);

            int offsetX = (int) Math.round(dx * FORWARD_OFFSET);
            int offsetZ = (int) Math.round(dz * FORWARD_OFFSET);

            origin = pos.add(offsetX, 0, offsetZ);
        }

        if (!(Minecraft.getMinecraft().theWorld.getBlockState(origin).getBlock() instanceof BlockAir)) {
            return null; // Ensures we're only searching when in air
        }

        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                for (int i = 1; i > -3; i -= 2) {
                    BlockPos checkPos = origin.add(x * i, 0, z * i);
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