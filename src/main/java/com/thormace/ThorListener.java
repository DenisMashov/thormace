package com.thormace;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ThorListener implements Listener {

    private final ThorMace plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ThorListener(ThorMace plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim.equals(attacker)) return;

        FileConfiguration config = plugin.getConfig();
        if (!(victim instanceof Player) && !config.getBoolean("affect-mobs", false)) return;
        if (!plugin.isThorMace(attacker.getInventory().getItemInMainHand())) return;
        if (!attacker.hasPermission("thormace.use")) return;

        long now = System.currentTimeMillis();
        long cooldown = (long) (config.getDouble("cooldown-seconds", 3.0) * 1000);
        Long last = cooldowns.get(attacker.getUniqueId());
        if (last != null && now - last < cooldown) return;
        cooldowns.put(attacker.getUniqueId(), now);

        strike(victim);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private void strike(LivingEntity victim) {
        FileConfiguration config = plugin.getConfig();
        final int count = config.getInt("lightning.count", 5);
        final int interval = Math.max(1, config.getInt("lightning.interval-ticks", 6));
        final boolean real = config.getBoolean("lightning.real", false);
        final double damage = config.getDouble("lightning.damage-per-strike", 3.0);
        final double launchY = config.getDouble("launch-victim-y", 0.6);

        final Location[] lastLoc = {victim.getLocation()};

        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= count) {
                    cancel();
                    return;
                }

                if (victim.isValid()) lastLoc[0] = victim.getLocation();
                Location loc = lastLoc[0];
                World world = loc.getWorld();

                if (real) world.strikeLightning(loc);
                else world.strikeLightningEffect(loc);

                if (i == 0) {
                    new BlockBlast(plugin, loc).start();
                    applyCurse(victim);
                    if (launchY > 0 && victim.isValid()) {
                        victim.setVelocity(new Vector(0, launchY, 0));
                    }
                }

                if (damage > 0 && victim.isValid() && !victim.isDead()) {
                    victim.setNoDamageTicks(0);
                    victim.damage(damage);
                }

                i++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void applyCurse(LivingEntity victim) {
        FileConfiguration c = plugin.getConfig();
        if (!victim.isValid()) return;

        int seconds = c.getInt("curse.blindness-seconds", 5);
        if (seconds > 0) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, seconds * 20, 0, false, true, true));
        }

        if (victim instanceof Player target && c.getBoolean("curse.title.enabled", true)) {
            Component main = plugin.color(c.getString("curse.title.text", "&c&lTHOR'S CURSE"));
            Component sub = plugin.color(c.getString("curse.title.subtitle", ""));
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(c.getInt("curse.title.fade-in-ticks", 5) * 50L),
                    Duration.ofMillis(c.getInt("curse.title.stay-ticks", 80) * 50L),
                    Duration.ofMillis(c.getInt("curse.title.fade-out-ticks", 20) * 50L));
            target.showTitle(Title.title(main, sub, times));
        }
    }
}
