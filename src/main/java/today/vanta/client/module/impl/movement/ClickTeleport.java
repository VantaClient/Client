package today.vanta.client.module.impl.movement;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.*;
import org.lwjgl.opengl.GL11;
import today.vanta.Vanta;
import today.vanta.client.event.impl.game.MiddleClickEvent;
import today.vanta.client.event.impl.game.render.Render3DEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.client.setting.impl.StringSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.world.PathfindingUtil;
import today.vanta.util.system.math.Counter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickTeleport extends Module {
    private final StringSetting mode = Setting.of("Mode", "Direct", "Direct", "Pathed chunks", "Chunked", "Hybrid");
    private final NumberSetting reach = Setting.of("Reach", 50, 3, 200, "blocks");
    private final NumberSetting delay = Setting.of("Delay", 50, 0, 1000, "ms")
            .hide(() -> mode.isValue("Direct"));
    private final NumberSetting chunkLength = Setting.of("Chunk length", 5, 1, 20, "blocks")
            .hide(() -> mode.isValue("Direct"));
    private final NumberSetting directPackets = Setting.of("Direct packets", 3, 1, 20)
            .hide(() -> !mode.isValue("Direct"));

    private PathEntity chunkedPath;
    private int chunkedIndex;
    private double chunkedExactX;
    private double chunkedExactY;
    private double chunkedExactZ;
    private boolean chunkedReachedTarget;

    private List<Vec3> straightChunks;
    private int straightIndex;
    private double straightTargetX;
    private double straightTargetY;
    private double straightTargetZ;

    private final Counter delayCounter = new Counter();

    private BlockPos previewBlockPos;
    private Vec3 previewTarget;

    public ClickTeleport() {
        super("ClickTeleport", "Teleports to where you are looking on middle click.", Category.MOVEMENT);
        displayNames = new String[]{"ClickTeleport", "ClickTP", "Teleport", "MidClickTP", "MidClickTeleport"};
    }

    @EventListen
    private void onMiddleClick(MiddleClickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (isChunkedTeleportActive()) {
            ChatUtil.warn("Already teleporting! Wait for it to finish.");
            return;
        }

        if (mc.thePlayer.isRiding()) {
            ChatUtil.error("You cannot teleport while riding an entity!");
            return;
        }

        MovingObjectPosition result = mc.thePlayer.rayTrace(reach.getValue().doubleValue(), 1.0F);

        if (result == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            ChatUtil.error("No block in reach!");
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        double targetX = blockPos.getX() + 0.5D;
        double targetY = blockPos.getY() + 1.0D;
        double targetZ = blockPos.getZ() + 0.5D;

        event.cancelled = true;

        switch (mode.getValue()) {
            case "Direct":
                teleportDirectly(targetX, targetY, targetZ);
                break;
            case "Pathed chunks":
                startPathedChunks(targetX, targetY, targetZ);
                break;
            case "Chunked":
                startStraightChunks(targetX, targetY, targetZ);
                break;
            case "Hybrid":
                startHybridChunks(targetX, targetY, targetZ);
                break;
        }
    }

    private void teleportDirectly(double x, double y, double z) {
        mc.thePlayer.setPosition(x, y, z);
        resetMotionAndFallDamage();

        int packets = directPackets.getValue().intValue();
        for (int i = 0; i < packets; i++) {
            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, true));
        }

        ChatUtil.info("Teleported to &e{}&f, &e{}&f, &e{}&f!", x, y, z);
    }

    private void startPathedChunks(double targetX, double targetY, double targetZ) {
        BlockPos targetBlockPos = new BlockPos(
                MathHelper.floor_double(targetX),
                MathHelper.floor_double(targetY),
                MathHelper.floor_double(targetZ)
        );

        PathEntity path = PathfindingUtil.calculatePath(mc.thePlayer, targetBlockPos, PathfindingUtil.DEFAULT_PATH_RANGE);

        if (path == null || path.getCurrentPathLength() == 0) {
            ChatUtil.error("Could not find a valid path to the target!");
            return;
        }

        chunkedPath = path;
        chunkedIndex = 0;
        chunkedExactX = targetX;
        chunkedExactY = targetY;
        chunkedExactZ = targetZ;
        chunkedReachedTarget = PathfindingUtil.pathReachesTarget(path, targetBlockPos);
        delayCounter.reset();
    }

    private void startStraightChunks(double targetX, double targetY, double targetZ) {
        straightChunks = buildStraightChunks(targetX, targetY, targetZ);

        if (straightChunks.isEmpty()) {
            ChatUtil.warn("You are already at the target!");
            return;
        }

        straightIndex = 0;
        straightTargetX = targetX;
        straightTargetY = targetY;
        straightTargetZ = targetZ;
        delayCounter.reset();
    }

    private void startHybridChunks(double targetX, double targetY, double targetZ) {
        BlockPos targetBlockPos = new BlockPos(
                MathHelper.floor_double(targetX),
                MathHelper.floor_double(targetY),
                MathHelper.floor_double(targetZ)
        );

        PathEntity path = PathfindingUtil.calculatePath(mc.thePlayer, targetBlockPos, PathfindingUtil.DEFAULT_PATH_RANGE);

        if (path != null && path.getCurrentPathLength() > 0) {
            chunkedPath = path;
            chunkedIndex = 0;
            chunkedExactX = targetX;
            chunkedExactY = targetY;
            chunkedExactZ = targetZ;
            chunkedReachedTarget = PathfindingUtil.pathReachesTarget(path, targetBlockPos);
            delayCounter.reset();
            return;
        }

        straightChunks = buildStraightChunks(targetX, targetY, targetZ);

        if (straightChunks.isEmpty()) {
            ChatUtil.warn("You are already at the target!");
            return;
        }

        straightIndex = 0;
        straightTargetX = targetX;
        straightTargetY = targetY;
        straightTargetZ = targetZ;
        delayCounter.reset();
    }

    private List<Vec3> buildStraightChunks(double targetX, double targetY, double targetZ) {
        List<Vec3> chunks = new ArrayList<>();
        double currentX = mc.thePlayer.posX;
        double currentY = mc.thePlayer.posY;
        double currentZ = mc.thePlayer.posZ;

        double length = chunkLength.getValue().doubleValue();

        while (Math.abs(targetY - currentY) > 0.01D) {
            double step = Math.min(Math.abs(targetY - currentY), length);
            currentY += Math.signum(targetY - currentY) * step;
            chunks.add(new Vec3(currentX, currentY, currentZ));
        }

        while (Math.abs(targetX - currentX) > 0.01D || Math.abs(targetZ - currentZ) > 0.01D) {
            double xDiff = targetX - currentX;
            double zDiff = targetZ - currentZ;
            double horizontalDistance = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);

            if (horizontalDistance <= length) {
                currentX = targetX;
                currentZ = targetZ;
            } else {
                double ratio = length / horizontalDistance;
                currentX += xDiff * ratio;
                currentZ += zDiff * ratio;
            }

            chunks.add(new Vec3(currentX, currentY, currentZ));
        }

        return chunks;
    }

    private void updatePreview() {
        MovingObjectPosition result = mc.thePlayer.rayTrace(reach.getValue().doubleValue(), 1.0F);

        if (result == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            previewBlockPos = null;
            previewTarget = null;
            return;
        }

        BlockPos blockPos = result.getBlockPos();

        if (previewBlockPos != null && previewBlockPos.equals(blockPos) && previewTarget != null) {
            return;
        }

        previewBlockPos = blockPos;
        previewTarget = new Vec3(blockPos.getX() + 0.5D, blockPos.getY() + 1.0D, blockPos.getZ() + 0.5D);
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        if (mc.thePlayer != null && mc.theWorld != null && !isChunkedTeleportActive()) {
            updatePreview();
        }

        if (!isChunkedTeleportActive()) {
            return;
        }

        long delayMs = delay.getValue().longValue();

        if (delayMs > 0 && !delayCounter.hasElapsed(delayMs, true)) {
            return;
        }

        int stepsToProcess = delayMs <= 0 ? Integer.MAX_VALUE : 1;

        if (chunkedPath != null) {
            processPathedChunks(stepsToProcess);
        } else if (straightChunks != null) {
            processStraightChunks(stepsToProcess);
        }
    }

    private void processPathedChunks(int stepsToProcess) {
        double length = chunkLength.getValue().doubleValue();

        for (int step = 0; step < stepsToProcess && chunkedIndex < chunkedPath.getCurrentPathLength(); step++) {
            int nextIndex = chunkedIndex;
            double accumulated = 0.0D;

            PathPoint startPoint = chunkedPath.getPathPointFromIndex(nextIndex);
            double prevX = startPoint.xCoord + 0.5D;
            double prevY = startPoint.yCoord;
            double prevZ = startPoint.zCoord + 0.5D;

            while (nextIndex < chunkedPath.getCurrentPathLength() - 1) {
                nextIndex++;
                PathPoint point = chunkedPath.getPathPointFromIndex(nextIndex);
                double x = point.xCoord + 0.5D;
                double y = point.yCoord;
                double z = point.zCoord + 0.5D;

                accumulated += MathHelper.sqrt_double(
                        (x - prevX) * (x - prevX)
                                + (y - prevY) * (y - prevY)
                                + (z - prevZ) * (z - prevZ)
                );

                prevX = x;
                prevY = y;
                prevZ = z;

                if (accumulated >= length) {
                    break;
                }
            }

            PathPoint endPoint = chunkedPath.getPathPointFromIndex(nextIndex);
            teleportTo(endPoint.xCoord + 0.5D, endPoint.yCoord, endPoint.zCoord + 0.5D);
            chunkedIndex = nextIndex + 1;
        }

        if (chunkedIndex >= chunkedPath.getCurrentPathLength()) {
            finishPathedChunks();
        }
    }

    private void processStraightChunks(int stepsToProcess) {
        for (int i = 0; i < stepsToProcess && straightIndex < straightChunks.size(); i++) {
            Vec3 pos = straightChunks.get(straightIndex++);
            teleportTo(pos.xCoord, pos.yCoord, pos.zCoord);
        }

        if (straightIndex >= straightChunks.size()) {
            finishStraightChunks();
        }
    }

    private void finishPathedChunks() {
        if (chunkedReachedTarget) {
            teleportTo(chunkedExactX, chunkedExactY, chunkedExactZ);
            ChatUtil.info("Teleported to &e{}&f, &e{}&f, &e{}&f!", chunkedExactX, chunkedExactY, chunkedExactZ);
            chunkedPath = null;
            return;
        }

        // Continue with straight-line from the closest reachable point.
        ChatUtil.warn("Path could not reach the exact target, switching to straight-line chunks.");
        chunkedPath = null;

        straightChunks = buildStraightChunks(chunkedExactX, chunkedExactY, chunkedExactZ);

        if (!straightChunks.isEmpty()) {
            straightIndex = 0;
            straightTargetX = chunkedExactX;
            straightTargetY = chunkedExactY;
            straightTargetZ = chunkedExactZ;
            delayCounter.reset();
        }
    }

    private void finishStraightChunks() {
        teleportTo(straightTargetX, straightTargetY, straightTargetZ);
        ChatUtil.info("Teleported to &e{}&f, &e{}&f, &e{}&f!", straightTargetX, straightTargetY, straightTargetZ);
        straightChunks = null;
    }

    private void teleportTo(double x, double y, double z) {
        mc.thePlayer.setPosition(x, y, z);
        resetMotionAndFallDamage();
        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, true));
    }

    private void resetMotionAndFallDamage() {
        mc.thePlayer.motionX = 0.0D;
        mc.thePlayer.motionY = 0.0D;
        mc.thePlayer.motionZ = 0.0D;
        mc.thePlayer.fallDistance = 0.0F;
    }

    private boolean isChunkedTeleportActive() {
        return chunkedPath != null || straightChunks != null;
    }

    @EventListen
    private void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || previewTarget == null) {
            return;
        }

        Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];

        double x = previewTarget.xCoord - mc.getRenderManager().viewerPosX;
        double y = previewTarget.yCoord - mc.getRenderManager().viewerPosY;
        double z = previewTarget.zCoord - mc.getRenderManager().viewerPosZ;

        double sizeXZ = 0.5D;
        double sizeY = 0.1D;

        AxisAlignedBB box = new AxisAlignedBB(
                x - sizeXZ, y, z - sizeXZ,
                x + sizeXZ, y + sizeY * 2, z + sizeXZ
        );

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.8f);
        GL11.glLineWidth(2.0F);

        drawBoundingBox(box);

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0F);
        GlStateManager.popMatrix();
    }

    private void drawBoundingBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        // Bottom face
        renderer.pos(box.minX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();

        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();

        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();

        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.minY, box.minZ).endVertex();

        // Top face
        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();

        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();

        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();

        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();

        // Vertical lines
        renderer.pos(box.minX, box.minY, box.minZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.minZ).endVertex();

        renderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.minZ).endVertex();

        renderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();

        renderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        renderer.pos(box.minX, box.maxY, box.maxZ).endVertex();

        tessellator.draw();
    }

    @Override
    public void onDisable() {
        chunkedPath = null;
        straightChunks = null;

        previewBlockPos = null;
        previewTarget = null;
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }
}