package ac.grim.grimac.checks.impl.backtrack;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import org.jetbrains.annotations.NotNull;


@CheckData(name = "BackTrack")
public class BackTrack extends Check implements PacketCheck {
    public BackTrack(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

    }

    @Override
    public void onPacketSend(PacketSendEvent event) {

    }
}
