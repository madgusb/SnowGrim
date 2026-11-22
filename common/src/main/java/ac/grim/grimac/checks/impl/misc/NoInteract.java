package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.raytrace.BlockRayTraceResult;
import ac.grim.grimac.utils.raytrace.RayTraceResult;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@CheckData(name = "NoInteract")
public class NoInteract extends Check implements PacketCheck {
    private static final Set<StateType> states = new HashSet<>(Arrays.asList(
            StateTypes.CRAFTING_TABLE,
            StateTypes.SHULKER_BOX,
            StateTypes.WHITE_SHULKER_BOX,
            StateTypes.ORANGE_SHULKER_BOX,
            StateTypes.MAGENTA_SHULKER_BOX,
            StateTypes.LIGHT_BLUE_SHULKER_BOX,
            StateTypes.YELLOW_SHULKER_BOX,
            StateTypes.LIME_SHULKER_BOX,
            StateTypes.PINK_SHULKER_BOX,
            StateTypes.GRAY_SHULKER_BOX,
            StateTypes.LIGHT_GRAY_SHULKER_BOX,
            StateTypes.CYAN_SHULKER_BOX,
            StateTypes.PURPLE_SHULKER_BOX,
            StateTypes.BLUE_SHULKER_BOX,
            StateTypes.BROWN_SHULKER_BOX,
            StateTypes.GREEN_SHULKER_BOX,
            StateTypes.RED_SHULKER_BOX,
            StateTypes.BLACK_SHULKER_BOX,
            StateTypes.FURNACE,
            StateTypes.BLAST_FURNACE,
            StateTypes.SMOKER,
            StateTypes.DROPPER,
            StateTypes.DISPENSER,
            StateTypes.BARREL,
            StateTypes.JUKEBOX,
            StateTypes.NOTE_BLOCK
    ));
    private boolean interactThisTick;

    public NoInteract(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            interactThisTick = false;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) interactThisTick = true;
        } else if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (interactThisTick || player.isSneaking) return;

            RayTraceResult target = player.getTargetBlock();
            if (target.getType() != RayTraceResult.Type.BLOCK) return;
            if (!(target instanceof BlockRayTraceResult blockTarget)) return;

            WrappedBlockState block = target.getState();
            if (block == null || !states.contains(block.getType())) return;

            if (shouldModifyPackets()) {
                player.onPacketCancel();
                event.setCancelled(true);
            }
            alert(String.format(Locale.ROOT,
                    "block=%s  face=%s",
                    block.getType().getName(), blockTarget.getFace()));
        }
    }
}
