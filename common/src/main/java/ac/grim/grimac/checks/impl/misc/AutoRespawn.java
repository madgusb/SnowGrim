package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AutoRespawn", experimental = true)
public class AutoRespawn extends Check implements PacketCheck {
    private long enableButtonsTimer = 0;

    public AutoRespawn(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2)) return;

        if (event.getPacketType() == PacketType.Play.Server.DEATH_COMBAT_EVENT) {
            player.sendTransaction();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                enableButtonsTimer = 0;
            });
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2)) return;

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            enableButtonsTimer++;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus status = new WrapperPlayClientClientStatus(event);

            // в майнкрафте блокировка стоит на 20 тиков, но CLIENT_TICK_END отправляется после разблокировки.
            if (status.getAction() == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
                if (enableButtonsTimer < 19 && flagAndAlert("ticks=" + enableButtonsTimer)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
