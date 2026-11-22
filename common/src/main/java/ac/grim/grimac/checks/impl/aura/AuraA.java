package ac.grim.grimac.checks.impl.aura;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AuraA", description = "attack with small tick diff from stop sprint", experimental = true)
public class AuraA extends Check implements PostPredictionCheck {
    private long sprintTicks = 0;
    private boolean attack;

    private final double maxBuffer = 10.0;
    private double buffer;

    public AuraA(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        PacketTypeCommon type = event.getPacketType();

        if (type.equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attack = true;
            }
        }

        if (type.equals(PacketType.Play.Client.ENTITY_ACTION)) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            switch (packet.getAction()) {
                case STOP_SPRINTING -> {
                    sprintTicks = 0;
                    attack = false;
                }
                case START_SPRINTING -> {
                    if (attack && player.isCritReady()) {
                        if (sprintTicks <= 1) {
                            buffer = Math.min(maxBuffer * 2, buffer + 1);
                            if (buffer >= maxBuffer) {
                                flag("ticks=" + sprintTicks + ", buffer=" + buffer);
                            }
                        } else {
                            buffer = Math.max(0, buffer - 1);
                        }
                    }
                    attack = false;
                }
            }
        }

    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        sprintTicks++;
    }
}
