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

    // Naya list banaya taaki server rejoin karne par boss bar wapas dikhe
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

        // FIX 1: Exact Crafting Table ki location lega, Player ki nahi
        Location craftLoc = event.getInventory().getLocation();
        if (craftLoc == null) craftLoc = p.getLocation(); // Agar GUI virtual hai toh fallback
        
        event.setCancelled(true);
        event.getInventory().setMatrix(new ItemStack[9]); 
        
        startLimitlessRitual(p, craftLoc, result);
    }

    // FIX 2: Agar koi crash ke baad ya beech mein join kare, toh boss bar dikhega
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (BossBar bar : activeBars) {
            bar.addPlayer(event.getPlayer());
        }
    }

    public void startLimitlessRitual(Player p, Location loc, ItemStack sword) {
        String displayName = sword.getItemMeta().getDisplayName();
        
        // Exact block ka center
        final Location center = loc.getBlock().getLocation().add(0.5, 1.2, 0.5); 
        
        // FIX 3: BossBar ko thoda "Mota/Segmented" banaya
        BossBar bar = Bukkit.createBossBar("§d§lAWAKENING §f» " + displayName, BarColor.PURPLE, BarStyle.SEGMENTED_20);
        Bukkit.getOnlinePlayers().forEach(bar::addPlayer);
        activeBars.add(bar); // Save to active list

        ArmorStand as = center.getWorld().spawn(center.clone().add(0, 0.5, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setBasePlate(false);
            s.getEquipment().setItemInMainHand(sword);
            s.setInvulnerable(true);
        });

        // FIX 4: Config se time set hoga (Agar nahi mila toh default 15 minutes)
        String swordID = sword.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
        int configMinutes = PhanTomCore.get().getConfig().getInt("swords." + swordID + ".ritual_time_minutes", 15);
        int totalTicks = configMinutes * 60 * 20;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Ritual Complete
                if (ticks >= totalTicks || as.isDead()) {
                    finishRitual(center, sword, bar, as);
                    this.cancel();
                    return;
                }

                // FIX 5: Sound Effects Add Kiye Hain
                if (ticks % 20 == 0) { // Har 1 second mein Beacon jaisi aawaz
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 1.2f);
                }
                if (ticks % 100 == 0) { // Har 5 seconds mein Dragon flap sound
                    center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
                }

                // FIX 6: 50 Block Hight aur Lag Optimization (Crash nahi hoga)
                for (double y = 0; y < 50; y += 1.0) { // Gap badha diya taaki particles kam hon aur crash na ho
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, y, 0), 2, 0.2, 0.2, 0.2, 0.01);
                    center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(0, y, 0), 2, 0.2, 0.2, 0.2, 0.02);
                }

                // Black Galaxy Orbit (Sky tak jayega)
                double radius = 1.8; 
                double spiralSpeed = 0.15; 
                for (int i = 0; i < 4; i++) { 
                    double angle = (ticks * spiralSpeed) + (i * Math.PI / 2);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    double yOffset = (ticks * 0.2 + (i * 10)) % 50; 
                    
                    Location orbitLoc = center.clone().add(x, yOffset, z);
                    center.getWorld().spawnParticle(Particle.SQUID_INK, orbitLoc, 3, 0.1, 0.1, 0.1, 0.01);
                }

                if (ticks % 4 == 0) { 
                    double skyAngle = ticks * 0.04;
                    double sx = 20 * Math.cos(skyAngle);
                    double sz = 20 * Math.sin(skyAngle);
                    center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center.clone().add(sx, 50, sz), 5, 1, 0.5, 1, 0.02);
                }

                // FIX 7: Sword Rotation ko Smooth Kiya
                as.setRotation(as.getLocation().getYaw() + 7f, 0); // 35 ki jagah 7 lagaya, ab makhan chalega

                // FIX 8: Boss Bar Time Format (m aur s ke sath)
                double progress = 1.0 - ((double) ticks / totalTicks);
                bar.setProgress(progress);
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
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.add(0, 2, 0), 5);
        loc.getWorld().dropItemNaturally(loc, sword);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
        
        bar.removeAll();
        activeBars.remove(bar); // Memory clean
        as.remove();
    }
           }
