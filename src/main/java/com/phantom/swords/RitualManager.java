package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class RitualManager implements Listener {

   @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result == null || !result.hasItemMeta()) return;
        
        String swordID = result.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
        if (swordID == null) return;

        Player p = (Player) event.getWhoClicked();

        // Check if the sword has already been crafted
        if (PhanTomCore.get().getConfig().getBoolean("crafted_swords." + swordID)) {
            p.sendMessage(ChatColor.RED + "This legendary item has already been awakened!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            event.setCancelled(true);
            return;
        }

        Location craftLoc = p.getLocation().clone(); 
        event.setCancelled(true);
        event.getInventory().setMatrix(new ItemStack[9]); 
        
        startLimitlessRitual(p, craftLoc, result);
    }
        Location craftLoc = p.getLocation().clone(); 
        event.setCancelled(true);
        event.getInventory().setMatrix(new ItemStack[9]); 
        
        startLimitlessRitual(p, craftLoc, result);
    }

    public void startLimitlessRitual(Player p, Location loc, ItemStack sword) {
        String displayName = sword.getItemMeta().getDisplayName();
        final Location center = loc.clone().add(0.5, 1.0, 0.5);
        
        // BossBar Setup - Purple and High Precision
        BossBar bar = Bukkit.createBossBar("§d§lAWAKENING §f» " + displayName, BarColor.PURPLE, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);

        // Sword Floating - Fast Rotating
        ArmorStand as = center.getWorld().spawn(center.clone().add(0, 1.8, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setBasePlate(false);
            s.getEquipment().setItemInMainHand(sword);
            s.setInvulnerable(true);
        });

        new BukkitRunnable() {
            int ticks = 0;
            int totalTicks = 900 * 20; // 15 Minutes (Precise)

            @Override
            public void run() {
                if (ticks >= totalTicks || as.isDead()) {
                    finishRitual(center, sword, bar, as);
                    this.cancel();
                    return;
                }

                // 1. DENSE PURPLE BEAM (Pillar Effect)
                // Sword se nikalta hua upar ki taraf jata mota pillar
                for (double y = 0; y < 6; y += 0.3) {
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, y, 0), 3, 0.15, 0.1, 0.15, 0.02);
                    center.getWorld().spawnParticle(Particle.SPELL_WITCH, center.clone().add(0, y, 0), 5, 0.2, 0.1, 0.2, 0.05);
                }

                // 2. BLACK GALAXY ORBIT (Spiral to Sky)
                // Ye black particles purple pillar ko orbit karte hue 80 block upar jayenge
                double radius = 1.8; 
                double spiralSpeed = 0.5;
                for (int i = 0; i < 4; i++) { // 4 lines of galaxy spiral
                    double angle = (ticks * spiralSpeed) + (i * Math.PI / 2);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    // Har particle niche se shuru hokar 80 blocks tak jata hai
                    double yOffset = (ticks * 0.4 + (i * 20)) % 80; 
                    
                    Location orbitLoc = center.clone().add(x, yOffset, z);
                    center.getWorld().spawnParticle(Particle.SQUID_INK, orbitLoc, 10, 0.1, 0.1, 0.1, 0.01);
                    center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, orbitLoc, 4, 0.05, 0.05, 0.05, 0.01);
                }

                // 3. SKY LAYER (Massive Black Orbit at 80 Blocks)
                if (ticks % 2 == 0) {
                    double skyAngle = ticks * 0.04;
                    double sx = 50 * Math.cos(skyAngle);
                    double sz = 50 * Math.sin(skyAngle);
                    center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(sx, 80, sz), 15, 2, 0.5, 2, 0.05);
                }

                // 4. FAST SWORD ROTATION
                as.setRotation(as.getLocation().getYaw() + 35f, 0);

                // 5. BOSS BAR (Zero Delay Update)
                double progress = 1.0 - ((double) ticks / totalTicks);
                bar.setProgress(progress);
                long sLeft = (totalTicks - ticks) / 20;
                bar.setTitle(String.format("§d§l%s §f| §b%02d:%02d §f| §eX:%d Y:%d Z:%d", 
                    displayName, (sLeft/60), (sLeft%60), center.getBlockX(), center.getBlockY(), center.getBlockZ()));

                ticks++;
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 1); // 1 tick interval = Smoothest Animation
    }

    private void finishRitual(Location loc, ItemStack sword, BossBar bar, ArmorStand as) {
        String swordID = sword.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);   
        // Save to config to prevent duplicate crafting
        PhanTomCore.get().getConfig().set("crafted_swords." + swordID, true);
        PhanTomCore.get().saveConfig();
        loc.getWorld().strikeLightning(loc);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.add(0, 2, 0), 5);
        loc.getWorld().dropItemNaturally(loc, sword);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
        bar.removeAll();
        as.remove();
    }
                                                }
