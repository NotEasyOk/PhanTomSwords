package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class RitualManager implements Listener {

    private final List<BossBar> activeBars = new ArrayList<>();

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result == null || !result.hasItemMeta()) return;
        
        String swordID = result.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
        if (swordID == null) return;

        Player p = (Player) event.getWhoClicked();

        if (PhanTomCore.get().getConfig().getBoolean("crafted_swords." + swordID)) {
            p.sendMessage(ChatColor.RED + "This legendary item has already been awakened!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            event.setCancelled(true);
            return;
        }

        Location craftLoc = event.getInventory().getLocation();
        if (craftLoc == null) craftLoc = p.getLocation();
        
        event.setCancelled(true);
        event.getInventory().setMatrix(new ItemStack[9]); 
        
        startLimitlessRitual(p, craftLoc, result);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (BossBar bar : activeBars) {
            bar.addPlayer(event.getPlayer());
        }
    }

    public void startLimitlessRitual(Player p, Location loc, ItemStack sword) {
        String displayName = sword.getItemMeta().getDisplayName();
        final Location center = loc.getBlock().getLocation().add(0.5, 1.2, 0.5); 
        
        BossBar bar = Bukkit.createBossBar("§d§lAWAKENING §f» " + displayName, BarColor.PURPLE, BarStyle.SEGMENTED_20);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        activeBars.add(bar);

        ArmorStand as = center.getWorld().spawn(center.clone().add(0, 0.5, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setBasePlate(false);
            s.getEquipment().setItemInMainHand(sword);
            s.setInvulnerable(true);
        });

        String swordID = sword.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
        int configMinutes = PhanTomCore.get().getConfig().getInt("swords." + swordID + ".ritual_time_minutes", 15);
        int totalTicks = configMinutes * 60 * 20;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= totalTicks || as.isDead()) {
                    finishRitual(center, sword, bar, as);
                    this.cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 1.2f);
                }
                if (ticks % 100 == 0) {
                    center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
                }

                // FIX 6: 1.21.11 Coordinate Fix (Particles ab dikhenge)
                for (double y = 0; y < 50; y += 1.0) {
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.getX(), center.getY() + y, center.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
                    center.getWorld().spawnParticle(Particle.WITCH, center.getX(), center.getY() + y, center.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
                }

                // FIX: Black Galaxy Orbit (Location cloning fix for 1.21.11)
                double radius = 1.8; 
                double spiralSpeed = 0.15; 
                for (int i = 0; i < 4; i++) { 
                    double angle = (ticks * spiralSpeed) + (i * Math.PI / 2);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    double yOffset = (ticks * 0.2 + (i * 10)) % 50; 
                    
                    center.getWorld().spawnParticle(Particle.SQUID_INK, center.getX() + x, center.getY() + yOffset, center.getZ() + z, 3, 0.1, 0.1, 0.1, 0.01);
                }

                if (ticks % 4 == 0) { 
                    double skyAngle = ticks * 0.04;
                    double sx = 20 * Math.cos(skyAngle);
                    double sz = 20 * Math.sin(skyAngle);
                    center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.getX() + sx, center.getY() + 50, center.getZ() + sz, 5, 1, 0.5, 1, 0.02);
                }

                // FIX 7: Sword Rotation Fix (1.21.11 mein teleport lagta hai ghumne ke liye)
                Location asLoc = as.getLocation();
                asLoc.setYaw(asLoc.getYaw() + 7f);
                as.teleport(asLoc);

                double progress = 1.0 - ((double) ticks / totalTicks);
                bar.setProgress(Math.max(0, Math.min(1, progress)));
                long sLeft = (totalTicks - ticks) / 20;
                bar.setTitle(String.format("§d§l%s §f| §b%dm %ds §f| §eX:%d Y:%d Z:%d", 
                    displayName, (sLeft/60), (sLeft%60), center.getBlockX(), center.getBlockY(), center.getBlockZ()));

                ticks++;
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 1); 
    }

    private void finishRitual(Location loc, ItemStack sword, BossBar bar, ArmorStand as) {
        String swordID = sword.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);   
        PhanTomCore.get().getConfig().set("crafted_swords." + swordID, true);
        PhanTomCore.get().saveConfig();
        
        loc.getWorld().strikeLightning(loc);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.getX(), loc.getY() + 2, loc.getZ(), 5);
        loc.getWorld().dropItemNaturally(loc, sword);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
        
        bar.removeAll();
        activeBars.remove(bar);
        as.remove();
    }
            }
