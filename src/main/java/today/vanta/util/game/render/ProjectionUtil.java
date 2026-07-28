package today.vanta.util.game.render;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import today.vanta.util.game.Commons;

public class ProjectionUtil implements Commons {
    public static ScreenBounds projectBoundingBox(Entity entity, float partialTicks, ScaledResolution sr) {
        double entityX = interpolate(entity.prevPosX, entity.posX, partialTicks);
        double entityY = interpolate(entity.prevPosY, entity.posY, partialTicks);
        double entityZ = interpolate(entity.prevPosZ, entity.posZ, partialTicks);

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        AxisAlignedBB box = entity.getEntityBoundingBox();

        AxisAlignedBB cameraSpaceBox = new AxisAlignedBB(
                box.minX - entity.posX + entityX - viewerX,
                box.minY - entity.posY + entityY - viewerY,
                box.minZ - entity.posZ + entityZ - viewerZ,
                box.maxX - entity.posX + entityX - viewerX,
                box.maxY - entity.posY + entityY - viewerY,
                box.maxZ - entity.posZ + entityZ - viewerZ
        );

        ScreenBounds bounds = new ScreenBounds();
        boolean projected = false;

        for (int i = 0; i < 8; i++) {
            double cornerX = (i & 1) == 0 ? cameraSpaceBox.minX : cameraSpaceBox.maxX;
            double cornerY = (i & 2) == 0 ? cameraSpaceBox.minY : cameraSpaceBox.maxY;
            double cornerZ = (i & 4) == 0 ? cameraSpaceBox.minZ : cameraSpaceBox.maxZ;

            Vec3 screen = project(cornerX, cornerY, cornerZ, sr);

            if (screen != null) {
                bounds.include(screen.xCoord, screen.yCoord);
                projected = true;
            }
        }

        if (!projected) {
            return null;
        }

        bounds.clamp(sr.getScaledWidth_double(), sr.getScaledHeight_double());
        return bounds.isVisible() ? bounds : null;
    }

    // claude made this
    public static ScreenBounds projectSelectionBoundingBox(AxisAlignedBB box, ScaledResolution sr) {
        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        AxisAlignedBB cameraSpaceBox = new AxisAlignedBB(
                box.minX - viewerX,
                box.minY - viewerY,
                box.minZ - viewerZ,
                box.maxX - viewerX,
                box.maxY - viewerY,
                box.maxZ - viewerZ
        );

        ScreenBounds bounds = new ScreenBounds();
        boolean projected = false;

        for (int i = 0; i < 8; i++) {
            double cornerX = (i & 1) == 0 ? cameraSpaceBox.minX : cameraSpaceBox.maxX;
            double cornerY = (i & 2) == 0 ? cameraSpaceBox.minY : cameraSpaceBox.maxY;
            double cornerZ = (i & 4) == 0 ? cameraSpaceBox.minZ : cameraSpaceBox.maxZ;

            Vec3 screen = project(cornerX, cornerY, cornerZ, sr);
            if (screen != null) {
                bounds.include(screen.xCoord, screen.yCoord);
                projected = true;
            }
        }

        if (!projected) return null;

        bounds.clamp(sr.getScaledWidth_double(), sr.getScaledHeight_double());
        return bounds.isVisible() ? bounds : null;
    }


    public static Vec3 project(double x, double y, double z, ScaledResolution sr) {
        Vec3 projected = ActiveRenderInfo.projectWorldToScreen(x, y, z);

        if (projected == null || projected.zCoord < 0.0D || projected.zCoord > 1.0D) {
            return null;
        }

        double scale = sr.getScaleFactor();
        double screenX = projected.xCoord / scale;
        double screenY = sr.getScaledHeight_double() - projected.yCoord / scale;

        return new Vec3(screenX, screenY, projected.zCoord);
    }

    public static double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }

    public static class ScreenBounds {
        public double minX = Double.MAX_VALUE;
        public double minY = Double.MAX_VALUE;
        public double maxX = -Double.MAX_VALUE;
        public double maxY = -Double.MAX_VALUE;

        private void include(double x, double y) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
        }

        private void clamp(double screenWidth, double screenHeight) {
            this.minX = MathHelper.clamp_double(this.minX, 0.0D, screenWidth);
            this.minY = MathHelper.clamp_double(this.minY, 0.0D, screenHeight);
            this.maxX = MathHelper.clamp_double(this.maxX, 0.0D, screenWidth);
            this.maxY = MathHelper.clamp_double(this.maxY, 0.0D, screenHeight);
        }

        private boolean isVisible() {
            return this.maxX > this.minX && this.maxY > this.minY;
        }
    }
}