package ac.grim.grimac.manager;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PunishGroup {
    public final List<AbstractCheck> checks;
    public final List<ParsedCommand> commands;
    public final Long2ObjectMap<Check> violations = new Long2ObjectOpenHashMap<>();
    public final int removeViolationsAfter, punishAfter; // time to remove violations after in milliseconds
}
