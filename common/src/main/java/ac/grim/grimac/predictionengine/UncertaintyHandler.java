package ac.grim.grimac.predictionengine;

import ac.grim.grimac.checks.impl.movement.NoPush;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTicker;
import ac.grim.grimac.utils.collisions.BlockPush;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.LastInstance;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.BoundingBoxSize;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static ac.grim.grimac.utils.anticheat.BoatUtil.getBoatStep;

public class UncertaintyHandler {
    private final GrimPlayer player;
    public final EvictingQueue<Double> pistonX = new EvictingQueue<>(5);
    public final EvictingQueue<Double> pistonY = new EvictingQueue<>(5);
    public final EvictingQueue<Double> pistonZ = new EvictingQueue<>(5);
    public boolean isStepMovement, wasStepMovement;
    public HashSet<BlockFace> slimePistonBounces;
    public double xNegativeUncertainty = 0;
    public double xPositiveUncertainty = 0;
    public double zNegativeUncertainty = 0;
    public double zPositiveUncertainty = 0;
    public double yNegativeUncertainty = 0;
    public double yPositiveUncertainty = 0;
    public double thisTickSlimeBlockUncertainty = 0;
    public double nextTickSlimeBlockUncertainty = 0;
    public boolean onGroundUncertain = false;
    public boolean lastPacketWasGroundPacket = false;
    public boolean isSteppingOnSlime = false;
    public boolean isSteppingOnIce = false;
    public boolean isSteppingOnHoney = false;
    public boolean wasSteppingOnBouncyBlock = false;
    public boolean isSteppingOnBouncyBlock = false;
    public boolean isSteppingNearBubbleColumn = false;
    public boolean isSteppingNearScaffolding = false;
    public boolean isSteppingNearShulker = false;
    public boolean isNearGlitchyBlock = false;
    public boolean isOrWasNearGlitchyBlock = false;
    public boolean wasInStuckSpeed = false;
    public boolean claimingLeftStuckSpeed = false;
    public boolean lastMovementWasZeroPointZeroThree = false;
    public boolean lastMovementWasUnknown003VectorReset = false;
    public boolean wasZeroPointThreeVertically = false;
    public boolean isChunkLoaded;
    public Boolean calculatedOnGround;
    private Vector3d hardTeleportPos;
    public final EvictingQueue<Integer> collidingEntities = new EvictingQueue<>(3);
    public final EvictingQueue<Integer> riptideEntities = new EvictingQueue<>(3);
    public final List<Integer> fishingRodPulls = new ArrayList<>();
    public Set<BlockPush> blockPushes = Set.of();
    public SimpleCollisionBox fireworksBox = null;
    public SimpleCollisionBox fishingRodPullBox = null;

    public final LastInstance lastFlyingTicks;
    public final LastInstance lastFlyingStatusChange;
    public final LastInstance lastUnderwaterFlyingHack;
    public final LastInstance lastStuckSpeedMultiplier;
    public final LastInstance lastHardCollidingLerpingEntity;
    public final LastInstance lastThirtyMillionHardBorder;
    public final LastInstance lastTeleportTicks;
    public final LastInstance lastPointThree;
    public final LastInstance stuckOnEdge;
    public final LastInstance lastStuckNorth;
    public final LastInstance lastStuckSouth;
    public final LastInstance lastStuckWest;
    public final LastInstance lastStuckEast;
    public final LastInstance lastVehicleSwitch;
    public double lastHorizontalOffset = 0;
    public double lastVerticalOffset = 0;

    public boolean fishingPullMayBeDelayed;
    private SimpleCollisionBox pendingPull;
    private SimpleCollisionBox sentPull;
    public SimpleCollisionBox boatCollisionBox;
    private int pendingPullTransaction = -1, pendingTeleportTransaction = -1, pendingChunkTransaction = -1;
    private boolean transactionConfirmed;

    public double fishingRodOffset = Double.MAX_VALUE;

    public UncertaintyHandler(GrimPlayer player) {
        this.player = player;
        this.lastFlyingTicks = new LastInstance(player);
        this.lastFlyingStatusChange = new LastInstance(player);
        this.lastUnderwaterFlyingHack = new LastInstance(player);
        this.lastStuckSpeedMultiplier = new LastInstance(player);
        this.lastHardCollidingLerpingEntity = new LastInstance(player);
        this.lastThirtyMillionHardBorder = new LastInstance(player);
        this.lastTeleportTicks = new LastInstance(player);
        this.lastPointThree = new LastInstance(player);
        this.stuckOnEdge = new LastInstance(player);
        this.lastStuckNorth = new LastInstance(player);
        this.lastStuckSouth = new LastInstance(player);
        this.lastStuckWest = new LastInstance(player);
        this.lastStuckEast = new LastInstance(player);
        this.lastVehicleSwitch = new LastInstance(player);
        tick();

        this.riptideEntities.add(0);
        this.collidingEntities.add(0);
    }

    public void tick() {
        pistonX.add(0d);
        pistonY.add(0d);
        pistonZ.add(0d);
        wasStepMovement = isStepMovement;
        isStepMovement = false;

        collidingEntities.clear();

        isSteppingNearShulker = false;
        wasSteppingOnBouncyBlock = isSteppingOnBouncyBlock;
        isSteppingOnSlime = false;
        isSteppingOnBouncyBlock = false;
        isSteppingOnIce = false;
        isSteppingOnHoney = false;
        isSteppingNearBubbleColumn = false;
        isSteppingNearScaffolding = false;

        boatCollisionBox = calculateBoatCollision();

        slimePistonBounces = new HashSet<>();
        tickFireworksBox();
    }

    public boolean hasEntityCollision() {
        final double avgColliding = player.uncertaintyHandler.collidingEntities.isEmpty()
                ? 0.0
                : Collections.max(player.uncertaintyHandler.collidingEntities);

        return avgColliding != 0;
    }

    public boolean wasAffectedByStuckSpeed() {
        return lastStuckSpeedMultiplier.hasOccurredSince(5);
    }

    public void tickFireworksBox() {
        fireworksBox = null;

        SimpleCollisionBox newPull = null;

        for (int owner : fishingRodPulls) {
            PacketEntity entity = player.compensatedEntities.getEntity(owner);
            if (entity == null) continue;

            SimpleCollisionBox entityBox = entity.getPossibleCollisionBoxes();
            final float scale = (float) entity.getAttributeValue(Attributes.SCALE);
            float width = BoundingBoxSize.getWidth(player, entity) * scale;
            float height = BoundingBoxSize.getHeight(player, entity) * scale;

            entityBox.maxY -= height;
            entityBox.expand(-width / 2, 0, -width / 2);

            double targetHeight = player.boundingBox.maxY - player.boundingBox.minY;
            double hookYOff = 0.8 * targetHeight;

            double dx = Math.abs(player.x - player.lastX);
            double dy = Math.abs(player.y - player.lastY);
            double dz = Math.abs(player.z - player.lastZ);

            double hookMinX = Math.min(player.lastX, player.x) - dx;
            double hookMaxX = Math.max(player.lastX, player.x) + dx;
            double hookMinY = Math.min(player.lastY, player.y) - dy + hookYOff;
            double hookMaxY = Math.max(player.lastY, player.y) + dy + hookYOff;
            double hookMinZ = Math.min(player.lastZ, player.z) - dz;
            double hookMaxZ = Math.max(player.lastZ, player.z) + dz;

            double minX = (entityBox.minX - hookMaxX) * 0.1;
            double maxX = (entityBox.maxX - hookMinX) * 0.1;
            double minY = (entityBox.minY - hookMaxY) * 0.1;
            double maxY = (entityBox.maxY - hookMinY) * 0.1;
            double minZ = (entityBox.minZ - hookMaxZ) * 0.1;
            double maxZ = (entityBox.maxZ - hookMinZ) * 0.1;

            SimpleCollisionBox one = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ, false);

            if (newPull == null) newPull = one;
            else {
                newPull.minX += one.minX; newPull.maxX += one.maxX;
                newPull.minY += one.minY; newPull.maxY += one.maxY;
                newPull.minZ += one.minZ; newPull.maxZ += one.maxZ;
            }
        }
        fishingRodPulls.clear();

        if (newPull != null) {
            if (pendingPull == null) pendingPull = newPull;
            else {
                pendingPull.minX += newPull.minX; pendingPull.maxX += newPull.maxX;
                pendingPull.minY += newPull.minY; pendingPull.maxY += newPull.maxY;
                pendingPull.minZ += newPull.minZ; pendingPull.maxZ += newPull.maxZ;
            }
        }

        if (pendingPull != null) {
            fishingRodPullBox = pendingPull;
        } else if (sentPull != null) {
            fishingRodPullBox = sentPull;
        } else {
            fishingRodPullBox = null;
        }
        fishingPullMayBeDelayed = pendingPull != null || sentPull != null;

        int maxFireworks = player.fireworks.getMaxFireworksAppliedPossible() * 2;
        if (maxFireworks <= 0 || (!player.isGliding && !player.wasGliding)) {
            return;
        }

        fireworksBox = new SimpleCollisionBox();

        Vector3dm currentLook = ReachUtils.getLook(player, player.yaw, player.pitch);
        Vector3dm lastLook = ReachUtils.getLook(player, player.lastYaw, player.lastPitch);

        double antiTickSkipping = player.isPointThree() ? 0 : 0.05;

        double minX = Math.min(-antiTickSkipping, currentLook.getX()) + Math.min(-antiTickSkipping, lastLook.getX());
        double minY = Math.min(-antiTickSkipping, currentLook.getY()) + Math.min(-antiTickSkipping, lastLook.getY());
        double minZ = Math.min(-antiTickSkipping, currentLook.getZ()) + Math.min(-antiTickSkipping, lastLook.getZ());
        double maxX = Math.max(antiTickSkipping, currentLook.getX()) + Math.max(antiTickSkipping, lastLook.getX());
        double maxY = Math.max(antiTickSkipping, currentLook.getY()) + Math.max(antiTickSkipping, lastLook.getY());
        double maxZ = Math.max(antiTickSkipping, currentLook.getZ()) + Math.max(antiTickSkipping, lastLook.getZ());

        minX *= 1.7;
        minY *= 1.7;
        minZ *= 1.7;
        maxX *= 1.7;
        maxY *= 1.7;
        maxZ *= 1.7;

        minX = Math.max(-1.7, minX);
        minY = Math.max(-1.7, minY);
        minZ = Math.max(-1.7, minZ);
        maxX = Math.min(1.7, maxX);
        maxY = Math.min(1.7, maxY);
        maxZ = Math.min(1.7, maxZ);

        fireworksBox = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void onTransactionReceived(final int transactionId) {
        if (pendingPullTransaction != -1 && transactionId >= pendingPullTransaction) {
            transactionConfirmed = true;
        }
        if (pendingTeleportTransaction != -1 && transactionId >= pendingTeleportTransaction) {
            if (hardTeleportPos != null) {
                isChunkLoaded = false;
            }
            if (calculatedOnGround != null) {
                player.calculatedOnGround = calculatedOnGround;
                calculatedOnGround = null;
            }
            pendingTeleportTransaction = -1;
        }
        if (pendingChunkTransaction != -1 && transactionId >= pendingChunkTransaction) {
            pendingChunkTransaction = -1;
            if (hardTeleportPos != null) {
                player.calculatedOnGround = MovementTicker.isOnGround(
                        player,
                        hardTeleportPos.x, hardTeleportPos.y, hardTeleportPos.z
                );
            }
            hardTeleportPos = null;
            isChunkLoaded = true;
        }
    }

    public record ChunkPosition(int chunkX, int chunkZ) {

        public boolean equals(@NotNull ChunkPosition position) {
            return position.chunkX == chunkX && position.chunkZ == chunkZ;
        }
    }

    public ChunkPosition getHardChunk() {
        return hardTeleportPos != null ? player.getChunkPosition(hardTeleportPos.x, hardTeleportPos.z) : null;
    }

    public void onTeleport(final Vector3d pos, boolean hard, Boolean calculatedOnGround) {
        if (calculatedOnGround != null) this.calculatedOnGround = calculatedOnGround;
        player.sendTransaction();
        pendingTeleportTransaction = player.lastTransactionSent.get();
        if (hard) hardTeleportPos = pos;
    }

    public void cancelHardTeleport() {
        pendingTeleportTransaction = -1;
        hardTeleportPos = null;
        isChunkLoaded = true;
    }

    public boolean hasHardTeleport() {
        return hardTeleportPos != null;
    }

    public void onHardChunkLoad() {
        player.sendTransaction();
        pendingChunkTransaction = player.lastTransactionSent.get();
    }

    public void endOfTickPendingPull() {
        if (transactionConfirmed) {
            transactionConfirmed = false;
            pendingPullTransaction = -1;

            SimpleCollisionBox p = sentPull;
            sentPull = null;

            if (p != null && !player.disableGrim && !player.compensatedEntities.self.isDead
                    && player.gamemode != GameMode.SPECTATOR) {
                Vector3dm pull = new Vector3dm(
                        (p.minX + p.maxX) / 2,
                        (p.minY + p.maxY) / 2,
                        (p.minZ + p.maxZ) / 2);
                Vector3dm total = player.clientVelocity.clone().add(pull);

                int targetId = player.compensatedEntities.serverPlayerVehicle != null
                        ? player.compensatedEntities.serverPlayerVehicle
                        : player.entityID;

                player.user.sendPacket(new WrapperPlayServerEntityVelocity(
                        targetId, new Vector3d(total.getX(), total.getY(), total.getZ())));

                double offset = fishingRodOffset;
                fishingRodOffset = Double.MAX_VALUE;

                if (player.wasLastPredictionCompleteChecked && offset != Double.MAX_VALUE) {
                    final NoPush noPush = player.checkManager.getCheck(NoPush.class);
                    double offsetToFlag = noPush != null ? noPush.offsetToFlag : 0.001;
                    if (offset > offsetToFlag && noPush != null) {
                        noPush.flagAndAlert(String.format("offset=%.6f", offset));
                    }
                }
            } else {
                fishingRodOffset = Double.MAX_VALUE;
            }
            return;
        }

        if (pendingPull == null) {
            if (sentPull == null) {
                fishingRodOffset = Double.MAX_VALUE;
            }
            return;
        }

        SimpleCollisionBox p = pendingPull;
        pendingPull = null;

        if (player.disableGrim || player.compensatedEntities.self.isDead
                || player.gamemode == GameMode.SPECTATOR) {
            fishingRodOffset = Double.MAX_VALUE;
            return;
        }

        double offset = fishingRodOffset;
        fishingRodOffset = Double.MAX_VALUE;

        if (!player.wasLastPredictionCompleteChecked || offset == Double.MAX_VALUE) return;
        final NoPush noPush = player.checkManager.getCheck(NoPush.class);
        double offsetToFlag = noPush != null ? noPush.offsetToFlag : 0.001;
        if (offset <= offsetToFlag) return;

        sentPull = p;
        player.sendTransaction();
        pendingPullTransaction = player.lastTransactionSent.get();
    }

    public SimpleCollisionBox getPlayerBox() {
        return player.boundingBox.copy().expand(player.getMovementThreshold());
    }

    private SimpleCollisionBox calculateBoatCollision() {
        SimpleCollisionBox expandedBB = GetBoundingBox.getBoundingBoxFromPosAndSize(
                player, player.lastX, player.lastY, player.lastZ,
                player.pose.width, player.pose.height
        );

        expandedBB.expand(player.getMovementThreshold());
        expandedBB.expandToAbsoluteCoordinates(player.x, player.y, player.z);

        SimpleCollisionBox playerBox = expandedBB.copy();

        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (!entity.isBoat || entity.hasPassenger(player.compensatedEntities.self)) {
                continue;
            }

            SimpleCollisionBox boatBox = entity.getPossibleCollisionBoxes();

            if (boatBox.isIntersected(playerBox)) {
                return boatBox;
            }
        }

        return null;
    }

    public double getOffsetHorizontal(VectorData data) {
        double threshold = player.getMovementThreshold();

        boolean newVectorPointThree = player.couldSkipTick && data.isKnockback();
        boolean explicit003 = data.isZeroPointZeroThree() || lastMovementWasZeroPointZeroThree;
        boolean either003 = newVectorPointThree || explicit003;

        double pointThree = newVectorPointThree || lastMovementWasUnknown003VectorReset ? threshold : 0;

        // 0.91 * 0.6 * (offset * 2) = 0.03276 + 0.03 offset
        if (explicit003) {
            pointThree = 0.91 * 0.6 * (threshold * 2) + threshold;
        }

        // (offset * 2) * 0.91 * 0.8 = max + 0.03 offset
        if (either003 && (influencedByBouncyBlock() || isSteppingOnHoney))
            pointThree = 0.91 * 0.8 * (threshold * 2) + threshold;

        // (offset * 2) * 0.91 * 0.989 = max + 0.03 offset
        if (either003 && isSteppingOnIce)
            pointThree = 0.91 * 0.989 * (threshold * 2) + threshold;

        // Reduce second tick uncertainty by minimum friction amount (if not velocity uncertainty)
        if (pointThree > threshold)
            pointThree *= 0.91 * 0.989;

        // 0.06 * 0.91 = max + 0.03 offset
        if (either003 && (player.lastOnGround || player.isFlying))
            pointThree = 0.91 * (threshold * 2) + threshold;

        // Friction while gliding is 0.99 horizontally
        if (either003 && (player.isGliding || player.wasGliding)) {
            pointThree = (0.99 * (threshold * 2)) + threshold;
        }

        // Limiting the error at stuck speed
        if (either003 && !player.inVehicle() && player.stuckSpeedMultiplier.getX() < 0.99) {
            pointThree = Math.min(pointThree, threshold);
        }

        return pointThree;
    }

    public boolean influencedByBouncyBlock() {
        return isSteppingOnBouncyBlock || wasSteppingOnBouncyBlock;
    }

    public double getVerticalOffset(VectorData data) {
        // We don't know if the player was pressing jump or not
        if (player.uncertaintyHandler.wasSteppingOnBouncyBlock && (player.wasTouchingWater || player.wasTouchingLava))
            return 0.06;

        // Not worth my time to fix this because checking flying generally sucks - if player was flying in last 2 ticks
        if ((lastFlyingTicks.hasOccurredSince(5)) && Math.abs(data.vector.getY()) < (4.5 * player.flySpeed - 0.25))
            return 0.06;

        double pointThree = player.getMovementThreshold();
        // This swim hop could be 0.03-influenced movement
        if (data.isTrident())
            return pointThree * 2;

        // Velocity resets velocity, so we only have to give 0.03 uncertainty rather than 0.06
        if (player.couldSkipTick && (data.isKnockback() || player.isClimbing) && !data.isZeroPointZeroThree())
            return pointThree;

        if (player.pointThreeEstimator.controlsVerticalMovement()) {
            if (data.isZeroPointZeroThree() || lastMovementWasZeroPointZeroThree) {
                return pointThree * 2;
            }
        }

        // Handle the player landing on this tick or the next tick
        if (wasZeroPointThreeVertically || player.uncertaintyHandler.onGroundUncertain || player.uncertaintyHandler.lastPacketWasGroundPacket)
            return pointThree;

        return 0;
    }

    public double reduceOffset(double offset) {
        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            offset -= 0.25;
        }

        // This is a section where I hack around current issues with Grim itself...
        if (player.uncertaintyHandler.influencedByBouncyBlock() && (!player.isPointThree() || player.inVehicle())) {
            offset -= 0.03;
        }
        // This is the end of that section.

        // I can't figure out how the client exactly tracks boost time
        if (player.compensatedEntities.self.getRiding() instanceof PacketEntityRideable vehicle) {
            if (vehicle.currentBoostTime < vehicle.boostTimeMax + 20)
                offset -= 0.01;
        }

        return Math.max(0, offset);
    }

    public void checkForHardCollision() {
        if (hasHardCollision()) player.uncertaintyHandler.lastHardCollidingLerpingEntity.reset();
    }

    private boolean hasHardCollision() {
        SimpleCollisionBox expandedBB = getPlayerBox();
        return isSteppingNearShulker || regularHardCollision(expandedBB) || striderCollision(expandedBB) || boatCollision(expandedBB);
    }

    private boolean regularHardCollision(SimpleCollisionBox expandedBB) {
        final PacketEntity riding = player.compensatedEntities.self.getRiding();
        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if ((entity.isBoat || entity.type == EntityTypes.SHULKER || entity.isHappyGhast) && entity != riding
                    && entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                return true;
            }
        }

        return false;
    }

    private boolean striderCollision(SimpleCollisionBox expandedBB) {
        if (player.compensatedEntities.self.getRiding() instanceof PacketEntityStrider) {
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.type == EntityTypes.STRIDER && entity != player.compensatedEntities.self.getRiding()
                        && !entity.hasPassenger(entity) && entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean boatCollision(SimpleCollisionBox expandedBB) {
        final PacketEntity riding = player.compensatedEntities.self.getRiding();
        if (riding == null || !riding.isBoat) return false;

        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (entity != riding && entity.isPushable() && !riding.hasPassenger(entity)
                    && entity.getPossibleCollisionBoxes().isIntersected(expandedBB)) {
                return true;
            }
        }
        return false;
    }
}
