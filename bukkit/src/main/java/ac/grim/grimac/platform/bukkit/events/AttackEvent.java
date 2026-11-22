package ac.grim.grimac.platform.bukkit.events;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class AttackEvent implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        double damage = event.getDamage();

        GrimPlayer attackerData = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(attacker.getUniqueId());
        if (attackerData != null && attackerData.punishEngine.isProcessing()) {
            damage *= 0.5;
        }

        Entity victim = event.getEntity();
        if (victim instanceof Player && attacker instanceof Player && !victim.equals(attacker)) {
            GrimPlayer victimData = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(victim.getUniqueId());
            if (victimData != null && victimData.punishEngine.isProcessing()) {
                damage *= 2;
            }
        }

        event.setDamage(damage);
    }

    private Entity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile) {
            ProjectileSource source = ((Projectile) damager).getShooter();
            return source instanceof Entity ? (Entity) source : null;
        }

        if (damager instanceof TNTPrimed) {
            return ((TNTPrimed) damager).getSource();
        }

        if (damager instanceof AreaEffectCloud) {
            ProjectileSource source = ((AreaEffectCloud) damager).getSource();
            return source instanceof Entity ? (Entity) source : null;
        }

        if (damager instanceof Tameable) {
            Object owner = ((Tameable) damager).getOwner();
            if (owner instanceof Entity) {
                return (Entity) owner;
            }
        }

        return damager;
    }
}
