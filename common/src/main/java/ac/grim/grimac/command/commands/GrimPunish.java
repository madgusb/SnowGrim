package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GrimPunish implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder(CommandRegister.cmd1, CommandRegister.cmd2)
                        .literal("punish")
                        .permission("snowgrim.punish")
                        .required("target", GrimAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
                        .required("check", StringParser.stringParser())
                        .required("ticks", IntegerParser.integerParser(0))
                        .required("command", StringParser.greedyStringParser())
                        .handler(this::handlePunish)
        );
    }

    private void handlePunish(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        String checkName = context.get("check");
        int ticks = context.get("ticks");
        String command = context.get("command");

        PlatformPlayer platformPlayer = target.getSinglePlayer().getPlatformPlayer();
        if (Objects.requireNonNull(platformPlayer).isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(
                    sender, "player-not-this-server",
                    "%prefix% &cThis player isn't on this server!"));
            return;
        }

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager()
                .getPlayer(platformPlayer.getUniqueId());

        if (player == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(
                    sender, "player-not-found",
                    "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        String finalCommand = command.replace("%player%", player.getName());

        player.punishEngine.add(() ->
                GrimAPI.INSTANCE.getPlatformServer().dispatchCommand(
                        GrimAPI.INSTANCE.getPlatformServer().getConsoleSender(),
                        finalCommand
                ), checkName, ticks);

        Component message = MessageUtil.getParsedComponent(
                sender, "punish-command-finish",
                "%prefix% &fPunish for &e%player% &f(&e%check%&f): &a/%command% &fscheduled in &e%ticks% &fticks");

        message = message
                .replaceText(b -> b.matchLiteral("%player%").replacement(player.getName()))
                .replaceText(b -> b.matchLiteral("%check%").replacement(checkName))
                .replaceText(b -> b.matchLiteral("%command%").replacement(finalCommand))
                .replaceText(b -> b.matchLiteral("%ticks%").replacement(String.valueOf(ticks)));

        sender.sendMessage(message);
    }
}
