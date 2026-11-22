package ac.grim.grimac.utils.raytrace;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.Locale;

public class BlockRayTraceResult extends RayTraceResult {
    private final Vector3i pos;
    private final BlockFace face;
    private final boolean inside;
    private final double offX, offY, offZ;

    public BlockRayTraceResult(Vector3d hitVec, WrappedBlockState state, Vector3i pos,
                               BlockFace face, boolean inside) {
        super(hitVec, state);
        this.pos = pos;
        this.face = face;
        this.inside = inside;
        this.offX = hitVec.x - pos.x;
        this.offY = hitVec.y - pos.y;
        this.offZ = hitVec.z - pos.z;
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    public Vector3i getPos() { return pos; }
    public BlockFace getFace() { return face; }
    public boolean isInside() { return inside; }

    public double getOffsetX() { return offX; }
    public double getOffsetY() { return offY; }
    public double getOffsetZ() { return offZ; }
    public Vector3d getOffset() { return new Vector3d(offX, offY, offZ); }

    @Override
    public String toString() {
        return "BlockRayTraceResult{pos=" + pos + ", face=" + face + ", inside=" + inside
                + ", hit=" + getHitVec() + ", offset=(" + fmt(offX) + "," + fmt(offY) + "," + fmt(offZ) + ")}";
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
