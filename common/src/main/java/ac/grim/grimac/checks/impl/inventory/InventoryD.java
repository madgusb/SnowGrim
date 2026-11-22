/* Decompiler 102ms, total 611ms, lines 127 */
package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.InventoryCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.player.MoveData;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import java.util.StringJoiner;

@CheckData(
        name = "InventoryD",
        setback = 1.0D,
        decay = 0.25D
)
public class InventoryD extends InventoryCheck {
    public InventoryD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            MoveData move = player.movement;
            boolean isMoving = move.isMoving();
            boolean isJumping = move.isJumping();

            if (isMoving || isJumping) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (predictionComplete.isChecked() && !predictionComplete.getData().isTeleport() && !this.player.getSetbackTeleportUtil().blockOffsets && !this.player.packetStateData.lastPacketWasTeleport && !this.player.packetStateData.isSlowedByUsingItem() && System.currentTimeMillis() - this.player.lastBlockPlaceUseItem >= 50L) {
            boolean inVehicle = this.player.inVehicle();

            MoveData move = player.movement;
            boolean isMoving = move.isMoving();
            boolean isJumping = move.isJumping();

            if (player.hasInventoryOpen) {
                if (!isMoving && !isJumping) {
                    this.reward();
                    return;
                }

                if (this.flag()) {
                    if (!this.isNoSetbackPermission()) {
                        this.shouldSetback();
                        if (this.getViolations() > this.getSetbackVL()) {
                            this.closeInventory();
                        }
                    }

                    StringJoiner joiner = new StringJoiner(" ");
                    if (isMoving) {
                        joiner.add("moving");
                    }

                    if (isJumping) {
                        joiner.add("jumping");
                    }

                    if (inVehicle) {
                        joiner.add("inVehicle");
                    }

                    this.alert(joiner.toString());
                }
            }

        }
    }
}
