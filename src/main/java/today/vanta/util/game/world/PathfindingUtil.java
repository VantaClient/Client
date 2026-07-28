package today.vanta.util.game.world;

import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.pathfinder.WalkNodeProcessor;
import today.vanta.util.game.Commons;

public class PathfindingUtil implements Commons {

    public static final float DEFAULT_PATH_RANGE = 128.0F;

    public static PathEntity calculatePath(Entity entity, BlockPos targetPos, float range) {
        WalkNodeProcessor nodeProcessor = new WalkNodeProcessor();
        nodeProcessor.setEnterDoors(true);
        nodeProcessor.setCanSwim(true);

        PathFinder pathFinder = new PathFinder(nodeProcessor);

        BlockPos entityPos = new BlockPos(entity);
        int searchOffset = (int) (range + 8.0F);

        ChunkCache chunkCache = new ChunkCache(
                entity.worldObj,
                entityPos.add(-searchOffset, -searchOffset, -searchOffset),
                entityPos.add(searchOffset, searchOffset, searchOffset),
                0
        );

        return pathFinder.createEntityPathTo(chunkCache, entity, targetPos, range);
    }

    public static boolean pathReachesTarget(PathEntity path, BlockPos targetPos) {
        if (path == null) {
            return false;
        }

        PathPoint finalPoint = path.getFinalPathPoint();
        return finalPoint != null
                && finalPoint.xCoord == targetPos.getX()
                && finalPoint.yCoord == targetPos.getY()
                && finalPoint.zCoord == targetPos.getZ();
    }

    public static void teleportAlongPath(PathEntity path) {
        if (path == null) {
            return;
        }

        for (int i = 0; i < path.getCurrentPathLength(); i++) {
            PathPoint point = path.getPathPointFromIndex(i);

            double x = point.xCoord + 0.5D;
            double y = point.yCoord;
            double z = point.zCoord + 0.5D;

            mc.thePlayer.setPosition(x, y, z);
            mc.thePlayer.motionX = 0.0D;
            mc.thePlayer.motionY = 0.0D;
            mc.thePlayer.motionZ = 0.0D;
            mc.thePlayer.fallDistance = 0.0F;
            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, true));
        }
    }
}
