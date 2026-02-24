package com.phantom.swords;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
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
                        applyGodAttributes(item, type); // Unbreakable aur Lore yahan apply hoga
                        sendFancyActionBar(p);
                    }
                }
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 10);
    }

    // --- NEW: Attributes & Unique Lore for ALL Swords ---
    private void applyGodAttributes(ItemStack item, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean changed = false;

        // Make Unbreakable and Lava Proof
        if (!meta.isUnbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            changed = true;
        }
        if (!meta.isFireResistant()) {
            meta.setFireResistant(true);
            changed = true;
        }

        // Set Unique Lore for each Sword
        List<String> lore = new ArrayList<>();
        lore.add("§8§m--------------------------");
        
        if (type.equals("PHANTOM_BLADE")) {
            lore.add("§7Type: §5§lCURSE ENERGY");
            lore.add("");
            lore.add("§d§lSKILLS:");
            lore.add(" §f» §fLeft: §bRapid Slash");
            lore.add(" §f» §fRight: §dCurse Dash");
            lore.add(" §f» §fShift+R: §4§lDOMAIN SLASH");
        } else if (type.equals("SHADOW_BLADE")) {
            lore.add("§7Type: §8§lSHADOW STEALTH");
            lore.add("");
            lore.add("§d§lSKILLS:");
            lore.add(" §f» §fLeft: §bSoul Pierce");
            lore.add(" §f» §fRight: §8Shadow Step");
            lore.add(" §f» §fShift+R: §5§lHOLLOW MODE");
        } else if (type.equals("FIRE_LIGHTER")) {
            lore.add("§7Type: §6§lSOLAR FLARE");
            lore.add("");
            lore.add("§d§lSKILLS:");
            lore.add(" §f» §fLeft: §eSolar Strike");
            lore.add(" §f» §fRight: §6Phoenix Dash");
            lore.add(" §f» §fShift+R: §c§lSUPERNOVA");
        }

        lore.add("");
        lore.add("§6§lGODLY ATTRIBUTES:");
        lore.add(" §f» §bUnbreakable Bond");
        lore.add(" §f» §6Lava Immunity");
        lore.add("");
        lore.add("§e§lLEGENDARY ARTIFACT §b§l✔");
        lore.add("§8§m--------------------------");

        // Lore sirf tab set karein jab pehle se na ho ya badal gaya ho
        if (!lore.equals(meta.getLore())) {
            meta.setLore(lore);
            changed = true;
        }

        if (changed) item.setItemMeta(meta);
    }

    private void sendFancyActionBar(Player p) {
        String bar = "§fL " + getIcon(p, "LEFT") + " §fR " + getIcon(p, "RIGHT") + " §fShift+R " + getIcon(p, "ULT");
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
    }

    // --- 1. PHANTOM BLADE ---
    private void executePhanTomBlade(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT":
                p.setVelocity(loc.getDirection().multiply(2.5).setY(0.1));
                p.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, loc, 50, 0.5, 0.5, 0.5, 0.1);
                p.getWorld().spawnParticle(Particle.DUST, loc, 40, new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.5f));
                damageNearby(p, 4.0, 8.0);
                p.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
                break;
            case "LEFT":
                for (int i = 0; i < 3; i++) {
                    Bukkit.getScheduler().runTaskLater(PhanTomCore.get(), () -> {
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getEyeLocation().add(p.getLocation().getDirection().multiply(2)), 5, 1, 1, 1, 0.1);
                        p.getWorld().spawnParticle(Particle.SOUL, p.getLocation(), 20, 2, 1, 2, 0.05);
                        damageNearby(p, 5.0, 6.0);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                    }, i * 3L);
                }
                break;
            case "ULT":
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 500, 6, 2, 6, 0.01);
                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 200, 3, 3, 3, 0.1);
                p.sendTitle("§4§lDOMAIN SLASH", "§7Everything Withers...", 5, 40, 5);
                p.getNearbyEntities(10, 10, 10).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(20.0, p);
                        en.setVelocity(new Vector(0, 1.5, 0));
                    }
                });
                p.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 0.5f);
                break;
        }
    }

    // --- 2. SHADOW BLADE ---
    private void executeShadowBlade(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT":
                Entity target = getAimTarget(p, 15);
                Location tpLoc = (target != null) ? target.getLocation().subtract(target.getLocation().getDirection().multiply(1)) : p.getTargetBlock(null, 12).getLocation();
                p.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 60, 0.5, 1, 0.5, 0.05);
                p.teleport(tpLoc.setDirection(loc.getDirection()));
                p.getWorld().spawnParticle(Particle.DRAGON_BREATH, tpLoc, 60, 0.5, 1, 0.5, 0.02);
                p.playSound(tpLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                break;
            case "LEFT":
                Vector dir = loc.getDirection();
                for (double d = 0; d < 10; d += 0.5) {
                    Location beam = p.getEyeLocation().add(dir.clone().multiply(d));
                    p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, beam, 3, 0, 0, 0, 0.02);
                }
                damageNearby(p, 10.0, 9.0);
                break;
            case "ULT":
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 2));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 3));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 1));
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ > 20) cancel();
                        p.getWorld().spawnParticle(Particle.WITCH, p.getLocation().add(0, 1, 0), 20, 1, 1, 1, 0.1);
                    }
                }.runTaskTimer(PhanTomCore.get(), 0, 5);
                p.playSound(loc, Sound.ENTITY_WITHER_DEATH, 1f, 1f);
                break;
        }
    }

    // --- 3. FIRE LIGHTER ---
    private void executeFireLighter(Player p, String action) {
        Location loc = p.getLocation();
        switch (action) {
            case "RIGHT":
                p.setVelocity(loc.getDirection().multiply(2.2).setY(0.4));
                p.getWorld().spawnParticle(Particle.FLAME, loc, 100, 1, 1, 1, 0.1);
                p.getWorld().spawnParticle(Particle.LAVA, loc, 20, 1, 1, 1, 0.5);
                p.playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                break;
            case "LEFT":
                Entity t = getAimTarget(p, 15);
                if (t != null) {
                    Location tLoc = t.getLocation();
                    tLoc.getWorld().spawnParticle(Particle.EXPLOSION, tLoc, 10);
                    tLoc.getWorld().spawnParticle(Particle.FLAME, tLoc, 50, 0.5, 2, 0.5, 0.1);
                    ((LivingEntity) t).damage(14.0, p);
                    t.setFireTicks(100);
                    p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                }
                break;
            case "ULT":
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * 6;
                    double z = Math.sin(angle) * 6;
                    p.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0.5, z), 10, 0, 0, 0, 0.1);
                }
                p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5);
                p.getNearbyEntities(8, 8, 8).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(18.0, p);
                        en.setFireTicks(200);
                        en.setVelocity(en.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(2));
                    }
                });
                p.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                break;
        }
    }

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

        if (action != null && checkCD(p, action)) handleChoice(p, type, action);
    }

    @EventHandler
public void onJoin(PlayerJoinEvent e) {
    // Player aate hi uska purana cooldown load karo
    actionCooldowns.put(e.getPlayer().getUniqueId(), dataManager.loadCooldowns(e.getPlayer().getUniqueId()));
}

@EventHandler
public void onQuit(PlayerQuitEvent e) {
    UUID uuid = e.getPlayer().getUniqueId();
    if (actionCooldowns.containsKey(uuid)) {
        // Player jaate hi uska data file mein save kar do
        dataManager.saveCooldowns(uuid, actionCooldowns.get(uuid));
        actionCooldowns.remove(uuid); 
    }
}

    private boolean checkCD(Player p, String action) {
        String type = getSwordType(p.getInventory().getItemInMainHand());
        HashMap<String, Long> pCds = actionCooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long last = pCds.getOrDefault(action, 0L);
        String path = "swords." + type + ".cooldowns." + (action.equals("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 10);
        if (System.currentTimeMillis() - last < maxCD * 1000L) return false;
        pCds.put(action, System.currentTimeMillis());
        return true;
    }

    private String getIcon(Player p, String action) {
        String type = getSwordType(p.getInventory().getItemInMainHand());
        long last = actionCooldowns.getOrDefault(p.getUniqueId(), new HashMap<>()).getOrDefault(action, 0L);
        String path = "swords." + type + ".cooldowns." + (action.equals("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 10);
        long diff = (System.currentTimeMillis() - last) / 1000;
        if (diff >= maxCD) return "§a§l✔";
        return "§c" + (maxCD - diff) + "s";
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

    private Entity getAimTarget(Player p, int range) {
        return p.getNearbyEntities(range, range, range).stream().filter(e -> e instanceof LivingEntity && e != p).findFirst().orElse(null);
    }

    private void damageNearby(Player p, double r, double d) {
        p.getNearbyEntities(r, r, r).forEach(e -> { if (e instanceof LivingEntity && e != p) ((LivingEntity) e).damage(d, p); });
    }
    }
