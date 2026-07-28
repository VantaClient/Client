package today.vanta.client.module.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import today.vanta.client.event.impl.game.network.ReceivePacketEvent;
import today.vanta.client.event.impl.game.world.UpdateEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.setting.Setting;
import today.vanta.client.setting.impl.BooleanSetting;
import today.vanta.client.setting.impl.NumberSetting;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.player.ChatUtil;
import today.vanta.util.game.player.RotationUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AntiCheat extends Module {
    private static final double SPEED_LIMIT_WALK_BPS = 4.42; // 0.221 * 20
    private static final double SPEED_LIMIT_SPRINT_BPS = 5.72; // 0.286 * 20
    private static final double SPEED_TOLERANCE = 1.35;
    private static final int FLY_AIR_TICKS_LIMIT = 45; // ~2.25s off ground
    private static final double STEP_MAX = 0.6;
    private static final int FIGHT_SPEED_LIMIT_CPS = 15;
    private static final long FIGHT_SPEED_WINDOW = 1000L;
    private static final int FIGHT_SPEED_SHORTTERM_LIMIT = 6;
    private static final long FIGHT_SPEED_SHORTTERM_WINDOW = 350L; // 7 ticks
    private static final double FIGHT_REACH_CREATIVE = 6.0;
    private static final int FASTBREAK_LIMIT = 7;
    private static final long FASTBREAK_WINDOW = 250L; // 5 ticks
    private static final long REACH_CORRELATION_MS = 250L;
    private static final double RAYCAST_EXPAND = 0.1;

    private final BooleanSetting checkSpeed = Setting.of("Speed", true);
    private final BooleanSetting checkFly = Setting.of("Fly", true);
    private final BooleanSetting checkStep = Setting.of("Step", true);
    private final BooleanSetting checkHighCps = Setting.of("High CPS", true);
    private final BooleanSetting checkReach = Setting.of("Reach", true);
    private final BooleanSetting checkFastBreak = Setting.of("Fast break", true);
    private final NumberSetting maxReach = Setting.of("Max reach", 4.4, 3.0, 6.0, 1, "m");
    private final NumberSetting notifyCooldown = Setting.of("Notify cooldown", 1.5, 0.0, 10.0, 1, "s");

    private final Map<UUID, TrackedPlayer> tracked = new HashMap<>();
    private final Map<Integer, UUID> entityIdToUuid = new HashMap<>();
    private final ConcurrentLinkedQueue<PacketRecord> packetQueue = new ConcurrentLinkedQueue<>();
    private long lastNotify = 0;
    private int cleanupTicks = 0;

    public AntiCheat() {
        super("AntiCheat", "Client side anticheat - NCP.", Category.MISC);
    }

    @Override
    public void onDisable() {
        tracked.clear();
        entityIdToUuid.clear();
        packetQueue.clear();
    }

    @EventListen
    private void onUpdate(UpdateEvent event) {
        long now = System.currentTimeMillis();

        PacketRecord record;
        while ((record = packetQueue.poll()) != null) {
            processPacketRecord(record, now);
        }

        // Movement checks.
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            TrackedPlayer data = getData(player);
            if (data.firstUpdate) {
                data.firstUpdate = false;
                data.lastX = player.posX;
                data.lastY = player.posY;
                data.lastZ = player.posZ;
                data.lastOnGround = player.onGround;
                continue;
            }

            double dx = player.posX - data.lastX;
            double dy = player.posY - data.lastY;
            double dz = player.posZ - data.lastZ;
            double horizontal = Math.hypot(dx, dz);
            double bps = horizontal * 20.0;

            if (player.onGround) {
                data.airTicks = 0;
            } else if (dy >= -0.1) {
                // Only count air time while not clearly falling.
                data.airTicks++;
            }

            if (checkSpeed.getValue()) {
                checkSpeed(player, data, bps);
            }

            if (checkStep.getValue()) {
                checkStep(player, dy);
            }

            if (checkFly.getValue()) {
                checkFly(player, data, dy);
            }

            data.lastX = player.posX;
            data.lastY = player.posY;
            data.lastZ = player.posZ;
            data.lastOnGround = player.onGround;
        }

        if (++cleanupTicks >= 20) {
            cleanupTicks = 0;
            cleanup(now);
        }
    }

    @EventListen
    private void onReceivePacket(ReceivePacketEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        long now = System.currentTimeMillis();

        if (event.packet instanceof S0BPacketAnimation) {
            S0BPacketAnimation packet = (S0BPacketAnimation) event.packet;
            if (packet.getAnimationType() == 0) { // 0 = swing arm
                packetQueue.add(new PacketRecord(packet.getEntityID(), now, RecordType.SWING));
            }
        } else if (event.packet instanceof S25PacketBlockBreakAnim) {
            S25PacketBlockBreakAnim packet = (S25PacketBlockBreakAnim) event.packet;
            packetQueue.add(new PacketRecord(packet.getBreakerId(), now, RecordType.BREAK));
        } else if (event.packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus packet = (S19PacketEntityStatus) event.packet;
            if (packet.getOpCode() == 2) { // entity hurt
                Entity entity = packet.getEntity(mc.theWorld);
                if (entity != null) {
                    packetQueue.add(new PacketRecord(entity.getEntityId(), now, RecordType.HURT));
                }
            }
        } else if (event.packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities packet = (S13PacketDestroyEntities) event.packet;
            for (int id : packet.getEntityIDs()) {
                packetQueue.add(new PacketRecord(id, now, RecordType.DESTROY));
            }
        }
    }

    private void processPacketRecord(PacketRecord record, long now) {
        if (mc.theWorld == null) return;

        switch (record.type) {
            case SWING:
                processSwing(record.entityId, now);
                break;
            case BREAK:
                processBreak(record.entityId, now);
                break;
            case HURT:
                processHurt(record.entityId, now);
                break;
            case DESTROY:
                processDestroy(record.entityId);
                break;
        }
    }

    private void processSwing(int entityId, long now) {
        Entity entity = mc.theWorld.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) return;

        EntityPlayer player = (EntityPlayer) entity;
        TrackedPlayer data = getData(player);
        data.lastSwingTime = now;
        data.swings.addLast(now);
        trim(data.swings, now, FIGHT_SPEED_WINDOW);

        if (checkHighCps.getValue()) {
            int cps = data.swings.size();
            if (cps > FIGHT_SPEED_LIMIT_CPS) {
                notify(player.getName() + " high CPS: " + cps + " (limit " + FIGHT_SPEED_LIMIT_CPS + ").");
            }

            int shortTerm = countWithin(data.swings, now, FIGHT_SPEED_SHORTTERM_WINDOW);
            if (shortTerm > FIGHT_SPEED_SHORTTERM_LIMIT) {
                notify(player.getName() + " high CPS (short-term): " + shortTerm + " swings/" + FIGHT_SPEED_SHORTTERM_WINDOW + "ms.");
            }
        }

        if (checkReach.getValue()) {
            checkSwingReach(player);
        }
    }

    private void processBreak(int entityId, long now) {
        Entity entity = mc.theWorld.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) return;

        EntityPlayer player = (EntityPlayer) entity;
        TrackedPlayer data = getData(player);
        data.breaks.addLast(now);
        trim(data.breaks, now, FASTBREAK_WINDOW);

        if (!checkFastBreak.getValue()) return;

        int count = data.breaks.size();
        if (count > FASTBREAK_LIMIT) {
            notify(player.getName() + " fast break: " + count + " starts/" + FASTBREAK_WINDOW + "ms (limit " + FASTBREAK_LIMIT + ").");
        }
    }

    private void processHurt(int entityId, long now) {
        Entity victim = mc.theWorld.getEntityByID(entityId);
        if (victim == null || victim == mc.thePlayer) return;

        EntityPlayer attacker = findRecentAttacker(victim, now);
        if (attacker == null) return;

        double reach = attacker.getDistanceToEntity(victim);
        double limit = attacker.capabilities.isCreativeMode ? FIGHT_REACH_CREATIVE : maxReach.getValue().doubleValue();
        if (reach > limit) {
            notify(attacker.getName() + " reach: " + format(reach) + "m on " + victim.getName() + " (max " + limit + "m).");
        }
    }

    private void processDestroy(int entityId) {
        UUID uuid = entityIdToUuid.remove(entityId);
        if (uuid != null) {
            tracked.remove(uuid);
        }
    }

    private void checkSpeed(EntityPlayer player, TrackedPlayer data, double bps) {
        // Recent knockback / damage can cause a brief speed spike; ignore it.
        if (player.hurtTime > 0) {
            data.speedTicks = 0;
            return;
        }

        double limit = player.isSprinting() ? SPEED_LIMIT_SPRINT_BPS : SPEED_LIMIT_WALK_BPS;
        if (player.isInWater()) limit *= 1.35;
        limit *= SPEED_TOLERANCE;

        if (bps > limit) {
            data.speedTicks++;
            if (data.speedTicks >= 6) {
                notify(player.getName() + " speed: " + format(bps) + " BPS (limit " + format(limit) + ").");
                data.speedTicks = 0;
            }
        } else {
            data.speedTicks = Math.max(0, data.speedTicks - 1);
        }
    }

    private void checkStep(EntityPlayer player, double dy) {
        if (dy <= STEP_MAX) return;
        if (!player.onGround) return; // jumping / falling handled elsewhere
        notify(player.getName() + " step: +" + format(dy) + " blocks (max " + STEP_MAX + ").");
    }

    private void checkFly(EntityPlayer player, TrackedPlayer data, double dy) {
        if (player.capabilities.isFlying || player.capabilities.isCreativeMode) {
            data.airTicks = 0;
            return;
        }

        if (data.airTicks > FLY_AIR_TICKS_LIMIT) {
            notify(player.getName() + " fly: airborne " + data.airTicks + " ticks (limit " + FLY_AIR_TICKS_LIMIT + ").");
            data.airTicks = FLY_AIR_TICKS_LIMIT - 10; // throttle repeats
        }

        // Vertical ascent limit: >0.6 blocks/tick sustained is not vanilla jump motion.
        if (!player.onGround && dy > 0.6) {
            notify(player.getName() + " fly: vertical +" + format(dy) + " blocks/tick.");
        }
    }

    private EntityPlayer findRecentAttacker(Entity victim, long now) {
        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == victim || player == mc.thePlayer) continue;

            TrackedPlayer data = tracked.get(player.getUniqueID());
            if (data == null) continue;
            if (now - data.lastSwingTime > REACH_CORRELATION_MS) continue;

            double dist = player.getDistanceToEntity(victim);
            if (dist < bestDist) {
                bestDist = dist;
                best = player;
            }
        }

        return best;
    }

    private void checkSwingReach(EntityPlayer player) {
        Entity target = raycastEntity(player);
        if (target == null) return;

        double dist = player.getDistanceToEntity(target);
        double limit = player.capabilities.isCreativeMode ? FIGHT_REACH_CREATIVE : maxReach.getValue().doubleValue();
        if (dist > limit) {
            notify(player.getName() + " swing reach: " + format(dist) + "m to " + target.getName() + " (max " + limit + "m).");
        }
    }

    private Entity raycastEntity(EntityPlayer player) {
        double reach = (player.capabilities.isCreativeMode ? FIGHT_REACH_CREATIVE : maxReach.getValue().doubleValue()) + 1.0;
        Vec3 eyePos = player.getPositionEyes(1.0F);
        Vec3 lookVec = RotationUtil.getVectorForRotation(player.rotationPitch, player.rotationYaw);
        Vec3 endPos = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);

        Entity target = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity == player || entity == mc.thePlayer || !(entity instanceof EntityLivingBase)) continue;

            AxisAlignedBB bb = entity.getEntityBoundingBox().expand(RAYCAST_EXPAND, RAYCAST_EXPAND, RAYCAST_EXPAND);
            MovingObjectPosition intercept = bb.calculateIntercept(eyePos, endPos);
            if (intercept != null) {
                double dist = eyePos.distanceTo(intercept.hitVec);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = entity;
                }
            }
        }

        return target;
    }

    private TrackedPlayer getData(EntityPlayer player) {
        entityIdToUuid.put(player.getEntityId(), player.getUniqueID());
        return tracked.computeIfAbsent(player.getUniqueID(), uuid -> new TrackedPlayer());
    }

    private void cleanup(long now) {
        tracked.entrySet().removeIf(entry -> mc.theWorld.getPlayerEntityByUUID(entry.getKey()) == null);

        for (TrackedPlayer data : tracked.values()) {
            trim(data.swings, now, FIGHT_SPEED_WINDOW * 2L);
            trim(data.breaks, now, FASTBREAK_WINDOW * 2L);
        }
    }

    private void notify(String message) {
        long now = System.currentTimeMillis();
        long cooldown = (long) (notifyCooldown.getValue().doubleValue() * 1000.0);
        if (now - lastNotify < cooldown) return;
        lastNotify = now;
        ChatUtil.sendNoLineNoPrefix("&cNCP: &f " + message);
    }

    private void trim(Deque<Long> deque, long now, long window) {
        while (!deque.isEmpty() && now - deque.peekFirst() > window) {
            deque.pollFirst();
        }
    }

    private int countWithin(Deque<Long> deque, long now, long window) {
        int count = 0;
        for (Long time : deque) {
            if (now - time <= window) count++;
        }
        return count;
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    private enum RecordType {SWING, BREAK, HURT, DESTROY}

    private static class PacketRecord {
        private final int entityId;
        private final long time;
        private final RecordType type;

        PacketRecord(int entityId, long time, RecordType type) {
            this.entityId = entityId;
            this.time = time;
            this.type = type;
        }
    }

    private static class TrackedPlayer {
        private boolean firstUpdate = true;
        private double lastX, lastY, lastZ;
        private boolean lastOnGround;
        private int airTicks;
        private int speedTicks;
        private long lastSwingTime;
        private final Deque<Long> swings = new ArrayDeque<>();
        private final Deque<Long> breaks = new ArrayDeque<>();
    }
}
