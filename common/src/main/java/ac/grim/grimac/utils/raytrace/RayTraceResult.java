package ac.grim.grimac.utils.raytrace;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.Getter;

public abstract class RayTraceResult {
    protected final Vector3d hitResult;

    @Getter
    protected final WrappedBlockState state;

    protected RayTraceResult(Vector3d hitVec, WrappedBlockState state) {
        this.hitResult = hitVec;
        this.state = state;
    }

    public abstract Type getType();

    public Vector3d getHitVec() {
        return this.hitResult;
    }

    public boolean isMiss() {
        return getType() == Type.MISS;
    }

    public boolean isBlock() {
        return getType() == Type.BLOCK;
    }

    public boolean isEntity() {
        return getType() == Type.ENTITY;
    }

    public enum Type {
        MISS,
        BLOCK,
        ENTITY
    }
}
