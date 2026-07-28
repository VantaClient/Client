package today.vanta.util.game.player;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import today.vanta.util.game.Commons;
import today.vanta.util.game.player.constructors.Rotation;
import today.vanta.util.game.world.BlockCache;

public class RotationUtil implements Commons {
    public static Rotation gcd(final Rotation rotation) {
        final Rotation previousRotation = new Rotation(mc.thePlayer.prevRotationYaw, mc.thePlayer.prevRotationPitch);
        return gcd(rotation, previousRotation);
    }

    public static Rotation gcd(final Rotation rotation, final Rotation previousRotation) {
        final float sensitivity = mc.gameSettings.mouseSensitivity;
        final float multiplier = sensitivity * 0.6f + 0.2f;
        final float deltaScale = 8.0f * 0.15f;
        final float step = multiplier * deltaScale;
        final float deltaYaw = MathHelper.wrapAngleTo180_float(rotation.yaw - previousRotation.yaw);
        final float deltaPitch = rotation.pitch - previousRotation.pitch;
        final float yaw = previousRotation.yaw + (float) Math.round(deltaYaw / step) * step;
        final float pitch = previousRotation.pitch + (float) Math.round(deltaPitch / step) * step;
        return new Rotation(yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    public static Rotation getSimpleRotations(EntityLivingBase target) {
        double diffX = target.posX - mc.thePlayer.posX;
        double diffY = target.posY + target.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90, 90));
    }


    public static Rotation getSimpleRotations(BlockPos blockPos) {
        double diffX = blockPos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockPos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockPos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90, 90));
    }

    public static Rotation getSimpleRotations(BlockCache blockCache) {
        double diffX = blockCache.pos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockCache.pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockCache.pos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (blockCache.facing == EnumFacing.UP) diffY += 0.5;
        if (blockCache.facing == EnumFacing.DOWN) diffY -= 0.5;

        float yaw = getAdjustedYaw();
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    public static Rotation getSimpleRotations(BlockCache blockCache, Rotation lastRotations) {
        Rotation rots = getSimpleRotations(blockCache);

        float yaw = rots.yaw;
        float pitch = rots.pitch;

        if (lastRotations != null) {
            yaw = smooth(lastRotations.yaw, yaw, 30);
            pitch = smooth(lastRotations.pitch, pitch, 20);
        }

        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90.0f, 90.0f));
    }

    public static Rotation getSidewaysRotations() {
        float originYaw = mc.thePlayer.rotationYaw;

        float yaw = mc.thePlayer.rotationYaw + 105;

        return new Rotation(yaw,84);
    }

    public static Rotation getGodbridgeRotations(BlockCache blockCache, Rotation lastRotations) {
        double diffX = blockCache.pos.getX() + 0.5 - mc.thePlayer.posX;
        double diffY = blockCache.pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = blockCache.pos.getZ() + 0.5 - mc.thePlayer.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        if (blockCache.facing == EnumFacing.UP) diffY += 0.5;
        if (blockCache.facing == EnumFacing.DOWN) diffY -= 0.5;

        float yaw;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
        switch (mc.thePlayer.getHorizontalFacing()) {
            case SOUTH:
                yaw = 150.0f;
                break;
            case NORTH:
                yaw = -34.5f;
                break;
            case EAST:
                yaw = 54.0f;
                break;
            case WEST:
                yaw = -125.5f;
                break;
            default:
                yaw = 45.0f;
        }

        if (lastRotations == null) {
            return new Rotation(yaw, pitch);
        }

        yaw = smooth(lastRotations.yaw, yaw, 30);
        pitch = smooth(lastRotations.pitch, pitch, 20);

        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), MathHelper.clamp_float(pitch, -90, 90));
    }

    public static Rotation getStaticRotations(BlockCache blockCache, Rotation lastRotations) {
        float yaw = 180;
        switch (mc.thePlayer.getHorizontalFacing()) {
            case NORTH:
                yaw = 0;
                break;
            case WEST:
                yaw = -90;
                break;
            case EAST:
                yaw = 90;
                break;
        }
        return new Rotation(MathHelper.wrapAngleTo180_float(yaw), 75.5f);
    }

    public static Rotation getForwardRotations(BlockCache blockCache, Rotation lastRotations) {
        return new Rotation(mc.thePlayer.rotationYaw, 57.5f);
    }

    private static float smooth(float current, float target, float max) {
        float diff = MathHelper.wrapAngleTo180_float(target - current);
        if (diff > max) diff = max;
        if (diff < -max) diff = -max;
        return current + diff;
    }

    /**
     * From the minecraft code {@link net.minecraft.entity.Entity#getVectorForRotation(float, float)}
     *
     * @param pitch The pitch.
     * @param yaw   The yaw.
     * @return Returns a vector of rotations.
     */
    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos((float) (-yaw * 0.017453292F - Math.PI));
        float f1 = MathHelper.sin((float) (-yaw * 0.017453292F - Math.PI));
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static float getAdjustedYaw() {
        switch (mc.thePlayer.getHorizontalFacing()) {
            case SOUTH:
                return -180;
            case NORTH:
                return 0;
            case EAST:
                return 90;
            case WEST:
                return -90;
            default:
                return mc.thePlayer.rotationYaw;
        }
    }
}