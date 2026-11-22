package ac.grim.grimac.utils.raytrace;

import com.github.retrooper.packetevents.util.Vector3d;

public class MissRayTraceResult extends RayTraceResult {
    public MissRayTraceResult(Vector3d hitVec) {
        super(hitVec, null);
    }

    @Override
    public Type getType() {
        return Type.MISS;
    }

    @Override
    public String toString() {
        return "MissRayTraceResult{hit=" + getHitVec() + "}";
    }
}
