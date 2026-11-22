package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@CheckData(name = "NoServerRotation")
public class NoServerRotation extends Check implements PacketCheck {

    private static final class Pending {
        final float yaw, pitch;
        final boolean relYaw, relPitch;
        volatile boolean confirmed;

        Pending(float yaw, float pitch, boolean relYaw, boolean relPitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.relYaw = relYaw;
            this.relPitch = relPitch;
        }
    }

    private final Queue<Pending> queue = new ConcurrentLinkedQueue<>();

    public NoServerRotation(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) return;

        WrapperPlayServerPlayerPositionAndLook w = new WrapperPlayServerPlayerPositionAndLook(event);
        Pending p = new Pending(w.getYaw(), w.getPitch(),
                w.isRelativeFlag(RelativeFlag.YAW),
                w.isRelativeFlag(RelativeFlag.PITCH));
        queue.add(p);

        final int id = player.lastTransactionSent.get();
        player.latencyUtils.addRealTimeTask(id, () -> p.confirmed = true);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        Pending p = queue.peek();
        if (p == null || !p.confirmed) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        queue.poll();

        if (p.relYaw && p.relPitch) return;
        if (player.inVehicle()) return;

        Location loc = flying.getLocation();
        float dYaw = Math.abs(GrimMath.wrapDegrees(loc.getYaw() - p.yaw));
        float dPitch = Math.abs(loc.getPitch() - GrimMath.clamp(p.pitch, -90f, 90f));

        if ((!p.relYaw && dYaw != 0) || (!p.relPitch && dPitch != 0)) {
            flagAndAlert(String.format("dYaw=%.4f, dPitch=%.4f", dYaw, dPitch));
        }
    }
}
