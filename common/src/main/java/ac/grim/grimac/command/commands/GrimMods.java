
package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.init.start.CommandRegister;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GrimMods implements BuildableCommand {

    private static final String DEFAULT_HEADER = "%prefix% &fTracked mods (&b%count%&f):";
    private static final String DEFAULT_LINE =
            "&8 • <click:copy_to_clipboard:%mod%><hover:show_text:'&7Click to copy'>&f%mod%</hover></click> &7— %description%";

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder(CommandRegister.cmd1, CommandRegister.cmd2)
                        .literal("mods", Description.of("List mods tracked by LanguageDetector"))
                        .permission("snowgrim.mods")
                        .handler(this::handleMods)
        );
    }

    private void handleMods(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        ConfigManager config = GrimAPI.INSTANCE.getConfigManager().getConfig();

        Map<String, Object> mods = config.getMapElse(
                "LanguageDetector.mods", Map.<String, Object>of());

        if (mods == null) mods = Map.of();

        String header = config.getStringElse("mods-header", DEFAULT_HEADER)
                .replace("%count%", String.valueOf(mods.size()));

        String lineFormat = config.getStringElse("mods-line", DEFAULT_LINE);

        TextComponent.Builder message = Component.text()
                .append(format(sender, header));

        for (String mod : mods.keySet()) {
            String description = config.getStringElse(
                    "LanguageDetector.mods." + mod + ".description", "");

            message.append(Component.newline())
                    .append(format(sender, lineFormat
                            .replace("%mod%", mod)
                            .replace("%description%", description)
                    ));
        }

        sender.sendMessage(message.build());
    }

    private Component format(Sender sender, String text) {
        return MessageUtil.miniMessage(
                MessageUtil.replacePlaceholders(sender, text));
    }
}
