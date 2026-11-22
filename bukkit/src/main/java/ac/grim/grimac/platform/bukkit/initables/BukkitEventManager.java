package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.bukkit.events.AttackEvent;
import ac.grim.grimac.platform.bukkit.events.PistonEvent;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Registering bukkit events...");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), GrimACBukkitLoaderPlugin.LOADER);
        Bukkit.getPluginManager().registerEvents(new AttackEvent(), GrimACBukkitLoaderPlugin.LOADER);
    }
}
