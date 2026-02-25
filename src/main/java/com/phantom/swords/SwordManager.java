package com.phantom.swords;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SwordManager implements Listener {

    private final HashMap<UUID, HashMap<String, Long>> actionCooldowns = new HashMap<>();
    private final PlayerDataManager dataManager = new PlayerDataManager();

    public SwordManager() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ItemStack item = p.getInventory().getItemInMainHand();
                    String type = getSwordType(item);
                    if (type != null) {
                        applyGodAttributes(item, type);
                        sendFancyActionBar(p, type);
                    }
                }
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 5); // Fast update for smooth UI
    }

    // --- 1. HARDCODED MATERIAL, LORE & CUSTOM MODEL DATA ---
    private void applyGodAttributes(ItemStack item, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Force Material to Netherite Sword
        if (item.getType() != Material.NETHERITE_SWORD) {
            item.setType(Material.NETHERITE_SWORD);
        }

        meta.setUnbreakable(true);
        meta.setFireResistant(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

        List<String> lore = new ArrayList<>();
        lore.add("§8§m----------------------------------");
        
        } else if (type.equals("PHANTOM_BLADE")) {
            meta.setCustomModelData(1);
            lore.add("§7Weapon Class: §5§lMYTHIC CURSE");
            lore.add("§8Forged in the depths of the void.");
            lore.add("");
            lore.add("§d§l✦ ABILITIES ✦");
            lore.add(" §f[L] §bVoid Cleave §7- Sweeping dark slashes.");
            lore.add(" §f[R] §dPhantom Dash §7- Burst forward with curse energy.");
            lore.add(" §f[Shift+R] §4§lDOMAIN EXPANSION §7- Slices reality itself.");
        } else if (type.equals("SHADOW_BLADE")) {
            meta.setCustomModelData(2);
            lore.add("§7Weapon Class: §8§lABYSSAL STEALTH");
            lore.add("§8A blade that casts no shadow.");
            lore.add("");
            lore.add("§d§l✦ ABILITIES ✦");
            lore.add(" §f[L] §bSoul Pierce §7- A piercing beam of soul fire.");
            lore.add(" §f[R] §8Assassin's Step §7- Blink behind your target.");
            lore.add(" §f[Shift+R] §5§lHOLLOW AWAKENING §7- Become a god of death.");
        } else if (type.equals("FIRE_LIGHTER")) {
            meta.setCustomModelData(3);
            lore.add("§7Weapon Class: §6§lSOLAR FLARE");
            lore.add("§8Burns with the heat of a dying star.");
            lore.add("");
            lore.add("§d§l✦ ABILITIES ✦");
            lore.add(" §f[L] §eOrbital Strike §7- Call down a solar laser.");
            lore.add(" §f[R] §6Phoenix Flight §7- Leave a trail of hellfire.");
            lore.add(" §f[Shift+R] §c§lSUPERNOVA §7- Erupt and obliterate.");
        }

        lore.add("");
        lore.add("§e§l⚝ GODLY ARTIFACT ⚝");
        lore.add("§8§m----------------------------------");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    // --- 2. ADVANCED PROGRESS BAR UI ---
    private void sendFancyActionBar(Player p, String type) {
        String lBar = getProgressBar(p, type, "LEFT");
        String rBar = getProgressBar(p, type, "RIGHT");
        String sBar = getProgressBar(p, type, "ULT");
        
        String bar = "§fL [" + lBar + "§f]  §fR [" + rBar + "§f]  §fS [" + sBar + "§f]";
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
    }

    private String getProgressBar(Player p, String type, String action) {
        long last = actionCooldowns.getOrDefault(p.getUniqueId(), new HashMap<>()).getOrDefault(action, 0L);
        String path = "swords." + type + ".cooldowns." + (action.equals("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 10);
        
        long passedMillis = System.currentTimeMillis() - last;
        double progress = Math.min(1.0, (double) passedMillis / (maxCD * 1000L));
        
        int totalBars = 8;
        int activeBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        if (progress >= 1.0) {
            bar.append("§a"); // Green if ready
            for (int i=0; i<totalBars; i++) bar.append("|");
        } else {
            bar.append("§c"); // Red for cooling down
            for (int i=0; i<activeBars; i++) bar.append("|");
            bar.append("§7"); // Grey for remaining
            for (int i=activeBars; i<totalBars; i++) bar.append("|");
        }
        return bar.toString();
    }

    // --- 3. GOD LEVEL SKILLS ---

    private void executePhanTomBlade(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT": // Phantom Dash
                p.setVelocity(loc.getDirection().multiply(3.0).setY(0.2));
                p.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        if (ticks++ > 15) cancel();
                        p.getWorld().spawnParticle(Particle.WITCH, p.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
                        p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 1);
                damageNearby(p, 4.0, 10.0);
                break;
                
            case "LEFT": // Void Cleave
                Vector dir = loc.getDirection().normalize();
                new BukkitRunnable() {
                    double dist = 0;
                    public void run() {
                        if (dist > 8) cancel();
                        Location point = p.getEyeLocation().add(dir.clone().multiply(dist));
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, point, 3, 0.5, 0.5, 0.5, 0.1);
                        p.getWorld().spawnParticle(Particle.PORTAL, point, 20, 0.5, 0.5, 0.5, 0.5);
                        p.getWorld().playSound(point, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.5f);
                        damageNearbyLoc(p, point, 2.5, 8.0);
                        dist += 1.5;
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 1);
                break;
                
            case "ULT": // DOMAIN SLASH (Area of Effect Sphere)
                p.sendTitle("§4§lDOMAIN EXPANSION", "§7Malevolent Shrine", 5, 40, 5);
                p.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 2f, 0.5f);
                
                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        if (ticks++ > 40) cancel();
                        Location center = p.getLocation().add(0, 1, 0);
                        
                        // Create Sphere Boundary
                        for (int i = 0; i < 50; i++) {
                            double u = Math.random() * 2 * Math.PI;
                            double v = Math.random() * Math.PI;
                            double x = 8 * Math.sin(v) * Math.cos(u);
                            double y = 8 * Math.sin(v) * Math.sin(u);
                            double z = 8 * Math.cos(v);
                            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
                        }
                        
                        // Internal Slashes
                        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(Math.random()*6-3, Math.random()*6-3, Math.random()*6-3), 5, 1, 1, 1, 0);
                        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.5f);
                        
                        // Continuous Damage inside Domain (No block breaking)
                        p.getNearbyEntities(8, 8, 8).forEach(en -> {
                            if (en instanceof LivingEntity && en != p) {
                                ((LivingEntity) en).damage(5.0, p); // Hits multiple times
                                ((LivingEntity) en).setNoDamageTicks(0);
                            }
                        });
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 2);
                break;
        }
    }

    private void executeShadowBlade(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT": // Exact Target Teleport
                Entity target = getAimTarget(p, 25);
                if (target != null) {
                    Location targetLoc = target.getLocation();
                    Vector inverseDir = targetLoc.getDirection().multiply(-1.5); // Go behind them
                    Location tpLoc = targetLoc.clone().add(inverseDir);
                    tpLoc.setDirection(targetLoc.getDirection()); // Face same way as target
                    
                    p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 50, 0.5, 1, 0.5, 0.05);
                    p.teleport(tpLoc);
                    p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 100, 0.5, 1, 0.5, 0.1);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
                    ((LivingEntity) target).damage(10.0, p); // Backstab damage
                } else {
                    p.sendMessage("§cNo target in sight!");
                    // Refund cooldown if missed
                    actionCooldowns.get(p.getUniqueId()).put("RIGHT", 0L); 
                }
                break;
                
            case "LEFT":
                Vector dir = loc.getDirection();
                for (double d = 0; d < 15; d += 0.5) {
                    Location beam = p.getEyeLocation().add(dir.clone().multiply(d));
                    p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, beam, 5, 0.1, 0.1, 0.1, 0.02);
                }
                p.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.5f);
                damageNearbyLoc(p, getAimTargetLoc(p, 15), 3.0, 12.0);
                break;
                
            case "ULT":
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 4));
                p.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
                p.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ > 40) cancel();
                        p.getWorld().spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.1);
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 5);
                break;
        }
    }

    private void executeFireLighter(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT": // Phoenix Flight
                p.setVelocity(loc.getDirection().multiply(2.5).setY(0.6));
                p.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.5f);
                new BukkitRunnable() {
                    int ticks = 0;
                    public void run() {
                        if (ticks++ > 20) cancel();
                        Location path = p.getLocation();
                        p.getWorld().spawnParticle(Particle.FLAME, path, 20, 0.5, 0.5, 0.5, 0.05);
                        p.getWorld().spawnParticle(Particle.LAVA, path, 5, 0.5, 0.5, 0.5, 0.1);
                        damageNearby(p, 3.0, 8.0);
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 1);
                break;
                
            case "LEFT": // Orbital Laser
                Entity t = getAimTarget(p, 30);
                if (t != null) {
                    Location targetLoc = t.getLocation();
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2f, 2f);
                    
                    new BukkitRunnable() {
                        int ticks = 0;
                        public void run() {
                            if (ticks++ > 20 || t.isDead()) cancel();
                            
                            // Yellow Laser Beam from Sky
                            for(double y = 0; y < 15; y+=0.5) {
                                targetLoc.getWorld().spawnParticle(Particle.DUST, targetLoc.clone().add(0, y, 0), 10, new Particle.DustOptions(Color.YELLOW, 2.0f));
                                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.clone().add(0, y, 0), 2, 0.2, 0.2, 0.2, 0.01);
                            }
                            
                            targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 2f);
                            ((LivingEntity) t).damage(4.0, p); // Continuous damage
                            t.setFireTicks(100);
                        }
                    }.runTaskTimer(PhanTomCore.get(), 0, 2);
                }
                break;
                
            case "ULT": // Supernova (No block break)
                p.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                new BukkitRunnable() {
                    double radius = 1;
                    public void run() {
                        if (radius > 12) cancel();
                        for (int i = 0; i < 360; i += 10) {
                            double angle = Math.toRadians(i);
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            Location ring = loc.clone().add(x, 0.5, z);
                            p.getWorld().spawnParticle(Particle.FLAME, ring, 5, 0, 0, 0, 0.05);
                            p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, ring, 2, 0, 0, 0, 0.02);
                        }
                        p.getNearbyEntities(radius, 3, radius).forEach(en -> {
                            if (en instanceof LivingEntity && en != p) {
                                ((LivingEntity) en).damage(25.0, p);
                                en.setFireTicks(300);
                                en.setVelocity(en.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.5));
                            }
                        });
                        radius += 1.5;
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 1);
                break;
        }
    }

    // --- EVENTS & UTILS ---

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        String type = getSwordType(item);
        if (type == null) return;

        String action = null;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            action = p.isSneaking() ? "ULT" : "RIGHT";
        } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            action = "LEFT";
        }

        if (action != null && checkCD(p, type, action)) handleChoice(p, type, action);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        actionCooldowns.put(e.getPlayer().getUniqueId(), dataManager.loadCooldowns(e.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().startsWith("§0Recipe:")) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (actionCooldowns.containsKey(uuid)) {
            dataManager.saveCooldowns(uuid, actionCooldowns.get(uuid));
            actionCooldowns.remove(uuid); 
        }
    }

    private boolean checkCD(Player p, String type, String action) {
        HashMap<String, Long> pCds = actionCooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long last = pCds.getOrDefault(action, 0L);
        String path = "swords." + type + ".cooldowns." + (action.equals("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 10);
        
        if (System.currentTimeMillis() - last < maxCD * 1000L) {
            return false; // Still on cooldown
        }
        
        pCds.put(action, System.currentTimeMillis());
        return true;
    }

    private void handleChoice(Player p, String type, String action) {
        if (type.equals("PHANTOM_BLADE")) executePhanTomBlade(p, action);
        else if (type.equals("SHADOW_BLADE")) executeShadowBlade(p, action);
        else if (type.equals("FIRE_LIGHTER")) executeFireLighter(p, action);
    }

    private String getSwordType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(PhanTomCore.SWORD_KEY, PersistentDataType.STRING);
    }

    // ADVANCED RAYTRACE AIMING
    private Entity getAimTarget(Player p, double range) {
        RayTraceResult result = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), range, 0.5, e -> e instanceof LivingEntity && e != p);
        return result != null ? result.getHitEntity() : null;
    }
    
    private Location getAimTargetLoc(Player p, double range) {
        RayTraceResult result = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getLocation().getDirection(), range, FluidCollisionMode.NEVER, true);
        return result != null && result.getHitPosition() != null ? new Location(p.getWorld(), result.getHitPosition().getX(), result.getHitPosition().getY(), result.getHitPosition().getZ()) : p.getEyeLocation().add(p.getLocation().getDirection().multiply(range));
    }

    private void damageNearby(Player p, double r, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> { 
            if (e instanceof LivingEntity && e != p) {
                ((LivingEntity) e).damage(d, p); 
                ((LivingEntity) e).setNoDamageTicks(0);
            }
        });
    }
    
    private void damageNearbyLoc(Player p, Location loc, double r, double d) {
        loc.getWorld().getNearbyEntities(loc, r, r, r).forEach(e -> {
            if (e instanceof LivingEntity && e != p) {
                ((LivingEntity) e).damage(d, p);
                ((LivingEntity) e).setNoDamageTicks(0);
            }
        });
    }

// --- PASSIVE ABILITIES ---
    private void applyPassiveEffects(Player p, String id) {
        switch (id) {
            case "PHANTOM_BLADE":
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, false, false));
                break;
                
            case "SHADOW_BLADE":
                p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 60, 7, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, false)); 
                break;
                
            case "FIRE_LIGHTER":
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 1, false, false)); 
                break;
        }
     }
}
