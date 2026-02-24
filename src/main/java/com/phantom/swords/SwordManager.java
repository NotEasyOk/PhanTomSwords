package com.phantom.swords;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
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
                    if (getSwordType(item) != null) {
                        sendFancyActionBar(p);
                    }
                }
            }
        }.runTaskTimer(PhanTomCore.get(), 0, 10);
    }

    private void sendFancyActionBar(Player p) {
        String bar = "§fL " + getIcon(p, "LEFT") + " §fR " + getIcon(p, "RIGHT") + " §fS " + getIcon(p, "ULT");
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
    }

    // UPDATED: Linked to Config Cooldowns (Line 41-55)
    private String getIcon(Player p, String action) {
        String type = getSwordType(p.getInventory().getItemInMainHand());
        long last = actionCooldowns.getOrDefault(p.getUniqueId(), new HashMap<>()).getOrDefault(action, 0L);
        
        // Dynamic path for Left, Right, and Shift
        String path = "swords." + type + ".cooldowns." + (action.equalsIgnoreCase("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 35);
        
        long diff = (System.currentTimeMillis() - last) / 1000;

        if (diff >= maxCD) return "§8[§f||||||§8] §a✓";
        
        int segments = (int) ((double) diff / maxCD * 6);
        StringBuilder progress = new StringBuilder("§8[§f");
        for (int i = 0; i < 6; i++) {
            if (i < segments) progress.append("|");
            else progress.append("§7|");
        }
        progress.append("§8] §c" + (maxCD - diff) + "s");
        return progress.toString();
    }

    // UPDATED: Linked to Config Cooldowns (Line 64-75)
    private boolean checkCD(Player p, String action) {
        String type = getSwordType(p.getInventory().getItemInMainHand());
        HashMap<String, Long> pCds = actionCooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long last = pCds.getOrDefault(action, 0L);
        
        // Dynamic path check
        String path = "swords." + type + ".cooldowns." + (action.equalsIgnoreCase("ULT") ? "shift" : action.toLowerCase());
        int maxCD = PhanTomCore.get().getConfig().getInt(path, 35);

        if (System.currentTimeMillis() - last < maxCD * 1000L) return false;

        pCds.put(action, System.currentTimeMillis());
        return true;
    }

    // --- 1. PHANTOM BLADE ---
    private void executePhanTomBlade(Player p, String action) {
        switch (action) {
            case "RIGHT":
                p.setVelocity(p.getLocation().getDirection().multiply(2.0).setY(0.2));
                p.getWorld().spawnParticle(Particle.REDSTONE, p.getLocation(), 60, new Particle.DustOptions(Color.RED, 2));
                damageNearby(p, 3.5, 7.0);
                p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 1.5f);
                break;
            case "LEFT":
                p.swingMainHand();
                for (int i = 1; i <= 3; i++) {
                    final double damage = 4.0 + i;
                    Bukkit.getScheduler().runTaskLater(PhanTomCore.get(), () -> {
                        damageNearby(p, 4.0, damage);
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(p.getLocation().getDirection().multiply(1.5)), 1);
                    }, i * 2L);
                }
                break;
            case "ULT":
                p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation(), 300, 5, 2, 5);
                p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, p.getLocation(), 100, 5, 1, 5);
                p.sendTitle("§4§lDOMAIN SLASH", "§7Darkness Consumes...", 5, 30, 5);
                p.getNearbyEntities(8, 8, 8).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(16.0, p);
                        en.setVelocity(en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(2).setY(1.0));
                    }
                });
                break;
        }
    }

    // --- 2. SHADOW BLADE ---
    private void executeShadowBlade(Player p, String action) {
        switch (action) {
            case "RIGHT":
                Location loc = p.getTargetBlock(null, 10).getLocation().add(0, 1, 0);
                p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 40);
                p.teleport(loc.setDirection(p.getLocation().getDirection()));
                p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 40);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                break;
            case "LEFT":
                Entity target = getAimTarget(p, 8);
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).damage(10.0, p);
                    target.getWorld().spawnParticle(Particle.SOUL, target.getLocation(), 30);
                }
                break;
            case "ULT":
                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 400, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
                p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation(), 150, 1, 2, 1);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
                p.sendMessage("§8§l[!] HOLLOW MODE ACTIVATED");
                break;
        }
    }

    // --- 3. FIRE LIGHTER BLADE ---
    private void executeFireLighter(Player p, String action) {
        switch (action) {
            case "RIGHT":
                p.setVelocity(p.getLocation().getDirection().multiply(2.2).setY(0.1));
                p.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
                break;
            case "LEFT":
                Entity t = getAimTarget(p, 12);
                if (t != null) {
                    t.getWorld().strikeLightningEffect(t.getLocation());
                    ((LivingEntity) t).damage(12.0, p);
                    t.setFireTicks(60);
                }
                break;
            case "ULT":
                p.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, p.getLocation(), 15, 3, 3, 3);
                p.getWorld().spawnParticle(Particle.LAVA, p.getLocation(), 50, 4, 1, 4);
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                p.getNearbyEntities(7, 7, 7).forEach(en -> {
                    if (en instanceof LivingEntity && en != p) {
                        ((LivingEntity) en).damage(14.0, p);
                        en.setFireTicks(160);
                    }
                });
                break;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        String type = getSwordType(p.getInventory().getItemInMainHand());
        if (type == null) return;

        String action = e.getAction().name().contains("RIGHT") ? "RIGHT" : (e.getAction().name().contains("LEFT") ? "LEFT" : null);
        if (action != null && checkCD(p, action)) handleChoice(p, type, action);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        String type = getSwordType(p.getInventory().getItemInMainHand());
        if (type != null && p.isSneaking()) {
            e.setCancelled(true);
            if (checkCD(p, "ULT")) handleChoice(p, type, "ULT");
        }
    }

    // --- DATA SAVING HANDLERS ---
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // File se purana cooldown load karke memory mein daal dega
        actionCooldowns.put(uuid, dataManager.loadCooldowns(uuid));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (actionCooldowns.containsKey(uuid)) {
            // Player ke leave karte hi data file mein save ho jayega
            dataManager.saveCooldowns(uuid, actionCooldowns.get(uuid));
            actionCooldowns.remove(uuid); 
        }
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
