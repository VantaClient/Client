package today.vanta.util.game.player;

import net.minecraft.entity.player.EntityPlayer;

public class DistanceCounter {
    private double distanceTravelled = 0;
    private double lastX, lastY, lastZ;
    private boolean initialized = false;

    public void tick(EntityPlayer player) {
        if (player == null) return;

        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;

        if (!initialized) {
            lastX = x;
            lastY = y;
            lastZ = z;
            initialized = true;
            return;
        }

        double deltaX = x - lastX;
        double deltaY = y - lastY;
        double deltaZ = z - lastZ;
        double moved = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        distanceTravelled += moved;

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    public double getTravelled() {
        return distanceTravelled;
    }

    public void reset() {
        distanceTravelled = 0;
    }
}
