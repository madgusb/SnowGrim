package ac.grim.grimac.checks.type;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

public class TickCheck extends Check {

    public TickCheck(@NotNull GrimPlayer player) {
        super(player);
    }

    public void onTick() {
    }
}
