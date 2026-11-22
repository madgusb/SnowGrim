package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.config.ConfigReloadable;
import ac.grim.grimac.api.event.events.CommandExecuteEvent;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.events.packets.ProxyAlertMessenger;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PunishmentManager implements ConfigReloadable {
    private final GrimPlayer player;
    private final List<PunishGroup> groups = new ArrayList<>();
    private String experimentalSymbol = "*";
    private String alertString;
    private boolean testMode;
    private String proxyAlertString = "";

    public PunishmentManager(GrimPlayer player) {
        this.player = player;
    }

    @Override
    public void reload(ConfigManager config) {
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());
        experimentalSymbol = config.getStringElse("experimental-symbol", "*");
        alertString = config.getStringElse("alerts-format", "%prefix% &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        testMode = config.getBooleanElse("test-mode", false);
        proxyAlertString = config.getStringElse("alerts-format-proxy", "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name% &f(x&c%vl%&f) &7%verbose%");
        try {
            groups.clear();

            // To support reloading
            for (AbstractCheck check : player.checkManager.allChecks.values()) {
                check.setEnabled(false);
            }

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);
                int punishAfter = (int) map.getOrDefault("punish-after", removeViolationsAfter);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<AbstractCheck> checksList = new ArrayList<>();
                List<AbstractCheck> excluded = new ArrayList<>();
                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (command.startsWith("!")) {
                        exclude = true;
                        command = command.substring(1);
                    }
                    for (AbstractCheck check : player.checkManager.allChecks.values()) { // o(n) * o(n)?
                        if (check.getCheckName() != null &&
                                (check.getCheckName().toLowerCase(Locale.ROOT).contains(command)
                                        || check.getAlternativeName().toLowerCase(Locale.ROOT).contains(command))) { // Some checks have equivalent names like AntiKB and AntiKnockback
                            if (exclude) {
                                excluded.add(check);
                            } else {
                                checksList.add(check);
                                check.setEnabled(true);
                            }
                        }
                    }
                    for (AbstractCheck check : excluded) checksList.remove(check);
                }

                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter * 1000, punishAfter * 20));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!", e);
        }
    }

    private String replaceAlertPlaceholders(String original, int vl, Check check, String verbose) {
        return MessageUtil.replacePlaceholders(player, original
                .replace("[alert]", alertString)
                .replace("[proxy]", proxyAlertString)
                .replace("%check_name%", check.getDisplayName())
                .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "")
                .replace("%vl%", Integer.toString(vl))
                .replace("%description%", check.getDescription())
        ).replace("%verbose%", MiniMessage.miniMessage().escapeTags(verbose));
    }

    public boolean handleAlert(GrimPlayer player, String verbose, Check check) {
        boolean sentDebug = false;

        // Check commands
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                // VL of this specific check (the same number shown in alerts)
                final int vl = getViolations(group, check);
                for (ParsedCommand command : group.commands) {
                    String cmd = replaceAlertPlaceholders(command.command, vl, check, verbose);

                    @Nullable Set<@Nullable PlatformPlayer> verboseListeners = null;

                    // Verbose that prints all flags
                    if (GrimAPI.INSTANCE.getAlertManager().hasVerboseListeners() && command.command.equals("[alert]")) {
                        sentDebug = true;
                        Component component = MessageUtil.miniMessage(cmd);
                        verboseListeners = GrimAPI.INSTANCE.getAlertManager().sendVerbose(component, null);
                    }

                    if (vl >= command.threshold) {
                        int executed = command.executeCounts.getOrDefault(check, 0);
                        // 0 means execute once
                        // Any other number means execute every X interval
                        boolean inInterval = command.interval == 0 ? (executed == 0) : (vl % command.interval == 0);
                        if (inInterval) {
                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(player, check, verbose, cmd);
                            GrimAPI.INSTANCE.getEventBus().post(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            switch (command.command) {
                                case "[webhook]" -> GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, verbose, check.getDisplayName(), vl);
                                case "[log]" -> {
                                    int vls = (int) group.violations.values().stream().filter((e) -> e == check).count();
                                    String verboseWithoutGl = verbose.replaceAll(" /gl .*", "");
                                    GrimAPI.INSTANCE.getViolationDatabaseManager().logAlert(player, verboseWithoutGl, check.getDisplayName(), vls);
                                }
                                case "[proxy]" -> ProxyAlertMessenger.sendPluginMessage(cmd);
                                case "[alert]" -> {
                                    sentDebug = true;
                                    Component message = MessageUtil.miniMessage(cmd);
                                    if (testMode) { // secret test mode
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(message);
                                        }
                                    } else {
                                        GrimAPI.INSTANCE.getAlertManager().sendAlert(message, verboseListeners);
                                    }
                                }
                                default -> {
                                    if (command.command.startsWith("[punish]")) {
                                        player.punishEngine.add(() ->
                                                GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(
                                                        GrimAPI.INSTANCE.getGrimPlugin(),
                                                        () -> GrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                                                                GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                                                                cmd.substring("[punish]".length()).trim()
                                                        )
                                                ), check.getCheckName(), group.punishAfter
                                        );
                                    } else {
                                        GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(GrimAPI.INSTANCE.getGrimPlugin(), () ->
                                                GrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                                                        GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                                                        cmd
                                                )
                                        );
                                    }
                                }
                            }
                        }

                        command.executeCounts.merge(check, 1, Integer::sum);
                    }
                }
            }
        }

        return sentDebug;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.checks.contains(check)) {
                long currentTime = System.currentTimeMillis();

                group.violations.put(currentTime, check);
                // Remove violations older than the defined time in the config
                group.violations.long2ObjectEntrySet().removeIf(time -> currentTime - time.getLongKey() > group.removeViolationsAfter);
            }
        }
    }

    private int getViolations(PunishGroup group, Check check) {
        return (int) check.violations;
    }
}

@RequiredArgsConstructor
class ParsedCommand {
    public final int threshold;
    public final int interval;
    public final String command;
    public final Map<AbstractCheck, Integer> executeCounts = new HashMap<>(); // executions per check
}
