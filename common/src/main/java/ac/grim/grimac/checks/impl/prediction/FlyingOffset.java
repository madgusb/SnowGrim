package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "OffsetFlying")
public class FlyingOffset extends Check implements PostPredictionCheck {
    public FlyingOffset(@NotNull GrimPlayer player) {
        super(player);
    }
}
