package ac.grim.grimac.utils.raytrace;

import ac.grim.grimac.utils.math.McMath;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class RayTraceUtil {
    public static Vector3d lookVector(float yaw, float pitch) {
        double f = Math.cos(-yaw * 0.017453292F - Math.PI);
        double f1 = Math.sin(-yaw * 0.017453292F - Math.PI);
        double f2 = -Math.cos(-pitch * 0.017453292F);
        double f3 = Math.sin(-pitch * 0.017453292F);
        return new Vector3d(f1 * f2, f3, f * f2);
    }

    public static RayTraceData fromLook(Vector3d eye, Vector3d look, double reach) {
        return new RayTraceData(eye, new Vector3d(
                eye.x + look.x * reach, eye.y + look.y * reach, eye.z + look.z * reach));
    }

    public static RayTraceResult traceBlocks(RayTraceData ctx,
                                             Function<Vector3i, WrappedBlockState> world,
                                             Predicate<StateType> targetable) {
        return doRayTrace(ctx, new BlockRayTracer(world, targetable), BlockRayTracer::miss);
    }

    public static BlockRayTraceResult getTargetBlock(Vector3d eye, Vector3d look, double reach,
                                                     Function<Vector3i, WrappedBlockState> world,
                                                     Predicate<StateType> targetable) {
        RayTraceResult r = traceBlocks(fromLook(eye, look, reach), world, targetable);
        return r.getType() == RayTraceResult.Type.BLOCK ? (BlockRayTraceResult) r : null;
    }

    public static BlockRayTraceResult getTargetBlock(Vector3d eye, float yaw, float pitch, double reach,
                                                     Function<Vector3i, WrappedBlockState> world) {
        return getTargetBlock(eye, lookVector(yaw, pitch), reach, world, RayTraceUtil::isFullCubeTargetable);
    }

    public static BlockRayTraceResult getTargetBlockIncludingNonFull(Vector3d eye, float yaw, float pitch, double reach,
                                                                     Function<Vector3i, WrappedBlockState> world) {
        return getTargetBlock(eye, lookVector(yaw, pitch), reach, world,
                t -> !StateBoxes.requiresHeldItem(t));
    }

    public static boolean isFullCubeTargetable(StateType t) {
        return t != null && !StateBoxes.requiresHeldItem(t) && StateBoxes.isFullCube(t);
    }

    public static boolean isFullCubeTargetable(WrappedBlockState state) {
        return state != null && isFullCubeTargetable(state.getType());
    }

    public static boolean isTargeting(BlockRayTraceResult target, Vector3i expectedPos, BlockFace expectedFace) {
        return target != null && target.getPos().equals(expectedPos)
                && (expectedFace == null || target.getFace() == expectedFace);
    }

    public static boolean isTargetingNear(BlockRayTraceResult target, Vector3i expectedPos, BlockFace expectedFace, int r) {
        if (target == null) return false;
        Vector3i p = target.getPos();
        if (Math.abs(p.x - expectedPos.x) > r) return false;
        if (Math.abs(p.y - expectedPos.y) > r) return false;
        if (Math.abs(p.z - expectedPos.z) > r) return false;
        return expectedFace == null || target.getFace() == expectedFace;
    }

    public static boolean isHitNearCenter(BlockRayTraceResult target, double margin) {
        if (target == null) return false;
        return Math.abs(target.getOffsetX() - 0.5) <= margin
                && Math.abs(target.getOffsetY() - 0.5) <= margin
                && Math.abs(target.getOffsetZ() - 0.5) <= margin;
    }

    public static <T> T doRayTrace(RayTraceData context, BiFunction<RayTraceData, Vector3i, T> rayTracer, Function<RayTraceData, T> missFactory) {
        Vector3d vector3d = context.getStart();
        Vector3d vector3d1 = context.getEnd();

        if (vector3d.equals(vector3d1)) {
            return missFactory.apply(context);
        } else {
            double d0 = McMath.lerp(-1.0E-7D, vector3d1.x, vector3d.x);
            double d1 = McMath.lerp(-1.0E-7D, vector3d1.y, vector3d.y);
            double d2 = McMath.lerp(-1.0E-7D, vector3d1.z, vector3d.z);
            double d3 = McMath.lerp(-1.0E-7D, vector3d.x, vector3d1.x);
            double d4 = McMath.lerp(-1.0E-7D, vector3d.y, vector3d1.y);
            double d5 = McMath.lerp(-1.0E-7D, vector3d.z, vector3d1.z);
            int i = McMath.floor(d3);
            int j = McMath.floor(d4);
            int k = McMath.floor(d5);
            Vector3i blockpos$mutable = new Vector3i(i, j, k);
            T t = rayTracer.apply(context, blockpos$mutable);

            if (t != null) {
                return t;
            } else {
                double d6 = d0 - d3;
                double d7 = d1 - d4;
                double d8 = d2 - d5;
                int l = McMath.signum(d6);
                int i1 = McMath.signum(d7);
                int j1 = McMath.signum(d8);
                double d9 = l == 0 ? Double.MAX_VALUE : (double) l / d6;
                double d10 = i1 == 0 ? Double.MAX_VALUE : (double) i1 / d7;
                double d11 = j1 == 0 ? Double.MAX_VALUE : (double) j1 / d8;
                double d12 = d9 * (l > 0 ? 1.0D - McMath.frac(d3) : McMath.frac(d3));
                double d13 = d10 * (i1 > 0 ? 1.0D - McMath.frac(d4) : McMath.frac(d4));
                double d14 = d11 * (j1 > 0 ? 1.0D - McMath.frac(d5) : McMath.frac(d5));

                while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                    if (d12 < d13) {
                        if (d12 < d14) {
                            i += l;
                            d12 += d9;
                        } else {
                            k += j1;
                            d14 += d11;
                        }
                    } else if (d13 < d14) {
                        j += i1;
                        d13 += d10;
                    } else {
                        k += j1;
                        d14 += d11;
                    }

                    T t1 = rayTracer.apply(context, new Vector3i(i, j, k));

                    if (t1 != null) {
                        return t1;
                    }
                }

                return missFactory.apply(context);
            }
        }
    }
}
