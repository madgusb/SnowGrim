package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "NoPush")
public class NoPush extends Check implements PostPredictionCheck {
    public double offsetToFlag = 0.001;

    public NoPush(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetToFlag = config.getDoubleElse(getConfigName() + ".threshold", 0.001);
    }
}
