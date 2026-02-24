package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.persistence.PersistentDataType;

public class RitualManager implements Listener {

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        // Link with PhanTomCore Sword Key
        if (!result.hasItemMeta() || !result.getItemMeta().getPersistentDataContainer().has(PhanTomCore.SWORD_KEY, PersistentDataType.STRING)) return;

        event.setCancelled(true); // Inventory mein turant nahi aayegi
        event.getInventory().setMatrix(new ItemStack[9]); // Items consume ho jayenge
        
        startRitual((Player) event.getWhoClicked(), event.getInventory().getLocation(), result);
    }

    public void startRitual(Player p, Location loc, ItemStack sword) {
        String displayName = sword.getItemMeta().getDisplayName();
        BossBar bar = Bukkit.createBossBar("§6Awakening: " + displayName, BarColor.PURPLE, BarStyle.SEGMENTED_20);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);

        // Sword Floating Effect
        ArmorStand as = loc.getWorld().spawn(loc.clone().add(0.5, 1.5, 0.5), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.getEquipment().setItemInMainHand(sword);
            s.setCustomNameVisible(false);
        });

        new BukkitRunnable() {
            int time = 900; // 15 Minutes (900 seconds)
            public void run() {
                if (time <= 0) {
                    loc.getWorld().strikeLightning(loc);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5);
                    loc.getWorld().dropItemNaturally(loc.clone().add(0, 1, 0), sword);
                    bar.removeAll();
                    as.remove();
                    cancel();
                    return;
                }
                
                // Animation logic
                as.setRotation(as.getLocation().getYaw() + 10f, 0);
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0.5, 1.8, 0.5), 5, 0.2, 0.2, 0.2, 0.02);
                
                bar.setProgress(time / 900.0);
                bar.setTitle("§6§l" + displayName + " §f| §c" + (time/60) + "m §f| §eX: " + loc.getBlockX() + " Z: " + loc.getBlockZ());
                time--;
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 20);
    }
    }
