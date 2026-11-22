package ac.grim.grimac.checks.impl.airstuck;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.TickCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

@CheckData(name = "AirStuck", description = "The player responds to transactions, but they remain in limbo", experimental = true)
public class AirStuck extends TickCheck implements PacketCheck {
    @Getter
    private long maxSkips = 2;

    private long confirms;

    public volatile boolean flaggedLastTick;

    public AirStuck(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (player.packetStateData.lastPacketWasTeleport) return;

            if (flying.hasPositionChanged()) {
                confirms = 0;
                flaggedLastTick = false;
            }
        }
    }

    private void onConfirm() {
        if (canSkipTickMove()) {
            confirms = 0;
            flaggedLastTick = false;
            return;
        }

        if (confirms > maxSkips) {
            if (flagAndAlert("skips=" + confirms)) {
                setbackIfAboveSetbackVL();
                flaggedLastTick = true;
            }
        } else flaggedLastTick = false;

        confirms++;
    }

    @Override
    public void onTick() {
        if (canSkipTickMove()) {
            return;
        }

        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), this::onConfirm);
    }

    public boolean canSkipTickMove() {
        return player.calculatedOnGround
                || player.exemptOnGround()
                || !player.uncertaintyHandler.isChunkLoaded
                || player.isFlying
                || player.stuckSpeedMultiplier.getY() != 1
                || player.wasTouchingLava || player.wasTouchingWater
                || (player.gamemode != null && player.gamemode == GameMode.SPECTATOR)
                || player.isGliding
                || player.inVehicle()
                || player.uncertaintyHandler.hasEntityCollision()
                || player.compensatedEntities.self.isDead;
    }

    @Override
    public void onReload(ConfigManager config) {
        maxSkips = config.getLongElse(getConfigName() + ".max-skips", 2);
    }
}
