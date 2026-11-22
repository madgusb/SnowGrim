package ac.grim.grimac.utils.raytrace;

import ac.grim.grimac.utils.raytrace.StateBoxes.Box;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BlockRayTracer implements BiFunction<RayTraceData, Vector3i, RayTraceResult> {
    private static final double EPS = 1.0E-7D;
    private static final double START_INSET = 1.0E-4D;

    private final Function<Vector3i, WrappedBlockState> world;
    private final Predicate<StateType> targetable;

    public BlockRayTracer(Function<Vector3i, WrappedBlockState> world, Predicate<StateType> targetable) {
        this.world = world;
        this.targetable = targetable;
    }

    public BlockRayTracer(Function<Vector3i, WrappedBlockState> world) {
        this(world, t -> !StateBoxes.requiresHeldItem(t));
    }

    public static RayTraceResult miss(RayTraceData ctx) {
        return new MissRayTraceResult(ctx.getEnd());
    }

    @Override
    public RayTraceResult apply(RayTraceData ctx, Vector3i pos) {
        WrappedBlockState state = world.apply(pos);
        if (state == null) return null;
        if (!targetable.test(state.getType())) return null;

        List<Box> boxes = ShapeResolver.getBoxes(state, pos.x, pos.z);
        if (boxes == null || boxes.isEmpty()) return null;

        Vector3d from = ctx.getStart(), to = ctx.getEnd();
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        if (dx * dx + dy * dy + dz * dz < EPS) return null;

        double ix = from.x + dx * START_INSET;
        double iy = from.y + dy * START_INSET;
        double iz = from.z + dz * START_INSET;
        double lx = ix - pos.x, ly = iy - pos.y, lz = iz - pos.z;
        for (Box b : boxes) {
            if (lx >= b.minX - EPS && lx <= b.maxX + EPS
                    && ly >= b.minY - EPS && ly <= b.maxY + EPS
                    && lz >= b.minZ - EPS && lz <= b.maxZ + EPS) {
                return new BlockRayTraceResult(new Vector3d(ix, iy, iz), state, pos,
                        nearest(dx, dy, dz).getOppositeFace(), true);
            }
        }

        double[] best = {1.0D};
        BlockFace[] faceOut = {null};
        for (Box local : boxes) {
            clipBox(local.offset(pos.x, pos.y, pos.z), from, dx, dy, dz, best, faceOut);
        }
        if (faceOut[0] == null) return null;

        Vector3d hit = new Vector3d(from.x + best[0] * dx, from.y + best[0] * dy, from.z + best[0] * dz);
        return new BlockRayTraceResult(hit, state, pos, faceOut[0], false);
    }

    private static void clipBox(Box b, Vector3d s, double dx, double dy, double dz,
                                double[] best, BlockFace[] faceOut) {
        if (dx > EPS)
            slab(best, faceOut, BlockFace.WEST, (b.minX - s.x) / dx, s.y, dy, s.z, dz, b.minY, b.maxY, b.minZ, b.maxZ);
        else if (dx < -EPS)
            slab(best, faceOut, BlockFace.EAST, (b.maxX - s.x) / dx, s.y, dy, s.z, dz, b.minY, b.maxY, b.minZ, b.maxZ);

        if (dy > EPS)
            slab(best, faceOut, BlockFace.DOWN, (b.minY - s.y) / dy, s.x, dx, s.z, dz, b.minX, b.maxX, b.minZ, b.maxZ);
        else if (dy < -EPS)
            slab(best, faceOut, BlockFace.UP, (b.maxY - s.y) / dy, s.x, dx, s.z, dz, b.minX, b.maxX, b.minZ, b.maxZ);

        if (dz > EPS)
            slab(best, faceOut, BlockFace.NORTH, (b.minZ - s.z) / dz, s.x, dx, s.y, dy, b.minX, b.maxX, b.minY, b.maxY);
        else if (dz < -EPS)
            slab(best, faceOut, BlockFace.SOUTH, (b.maxZ - s.z) / dz, s.x, dx, s.y, dy, b.minX, b.maxX, b.minY, b.maxY);
    }

    private static void slab(double[] best, BlockFace[] faceOut, BlockFace face, double t,
                             double sa, double da, double sb, double db,
                             double minA, double maxA, double minB, double maxB) {
        if (t < 0.0D || t >= best[0]) return;
        double a = sa + t * da, b = sb + t * db;
        if (a >= minA - EPS && a <= maxA + EPS && b >= minB - EPS && b <= maxB + EPS) {
            best[0] = t;
            faceOut[0] = face;
        }
    }

    private static BlockFace nearest(double x, double y, double z) {
        double ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ax >= ay && ax >= az) return x > 0 ? BlockFace.EAST : BlockFace.WEST;
        if (ay >= az) return y > 0 ? BlockFace.UP : BlockFace.DOWN;
        return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }
}
