package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class LifeManager implements Listener, CommandExecutor, TabCompleter {

    private final PhanTomCore plugin;
    public LifeManager(PhanTomCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("life-system.enabled", true)) return;
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        playDeathAnimation(victim);
        if (killer == null || killer.equals(victim)) return;
        double chance = plugin.getConfig().getDouble("life-system.transfer-chance", 0.5);
        if (Math.random() < chance) {
            handleHeartTransfer(killer, victim);
        }
    }

    private void playDeathAnimation(Player victim) {
        Location loc = victim.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
        
        ItemStack graveItem = new ItemStack(Material.GRAY_CONCRETE);
         ItemMeta meta = graveItem.getItemMeta();
          if (meta != null) {
           meta.setCustomModelData(500);
          graveItem.setItemMeta(meta);
     } 
               
        ArmorStand grave = loc.getWorld().spawn(loc.clone().add(0, -0.5, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setCustomName("§7§lRIP §f" + victim.getName());
            as.setCustomNameVisible(true);
            as.setSmall(true);
            as.getEquipment().setHelmet(graveItem);
        });
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 60) {
                    grave.remove();
                    cancel();
                    return;
                }
                loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, ticks * 0.05, 0), 5, 0.2, 0.2, 0.2, 0.02);
                loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, ticks * 0.05, 0), 2, 0.1, 0.1, 0.1, 0.01);
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    @EventHandler
    public void onHeartUse(PlayerInteractEvent e) {
        if (!plugin.getConfig().getBoolean("life-system.enabled", true)) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.FERMENTED_SPIDER_EYE) return;
        if (!item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("Physical Heart")) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            double max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
            if (max >= 40.0) {
                p.sendMessage("§cYou already have maximum hearts!");
                return;
            }
            item.setAmount(item.getAmount() - 1);
            p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(max + 2.0);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.sendMessage("§a§l+1 Heart Consumed!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("life-system.enabled", true)) return;
        Player p = e.getPlayer();
        
        // Agar player revive list mein hai
        if (plugin.getDataManager().isRevived(p.getUniqueId())) {
            double reviveHealth = 6.0; // 3 Hearts
            p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(reviveHealth);
            p.setHealth(reviveHealth);
            
            // Data se remove karo taaki baar baar reset na ho
            plugin.getDataManager().removeRevivedPlayer(p.getUniqueId());
            p.sendMessage("§d§lYou have been brought back to life!");
        }
    }

    private void handleHeartTransfer(Player killer, Player victim) {
        double vMax = victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
        double kMax = killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
        double newVMax = vMax - 2.0;
        if (newVMax <= 0) {
            eliminate(victim);
        } else {
            victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(newVMax);
            victim.sendMessage("§c§l-1 Heart Taken...");
        }
        if (kMax < 40.0) {
            killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(kMax + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, kMax + 2.0));
            killer.sendTitle("§4§l+1 LIFE TAKEN", "§7Your power grows...", 10, 40, 10);
            killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);
        } else {
            killer.getWorld().dropItemNaturally(killer.getLocation(), getHeartItem());
            killer.sendMessage("§eMax Hearts reached! Physical Heart dropped.");
        }
    }

    private void eliminate(Player p) {
        Bukkit.broadcastMessage("§8§m----------------------------------\n§4§lELIMINATION: §f" + p.getName() + " has fallen.\n§8§m----------------------------------");
        p.getWorld().strikeLightningEffect(p.getLocation());
        Bukkit.getBanList(BanList.Type.NAME).addBan(p.getName(), "§cYour story has ended...", null, "PhanTomLife");
        p.kickPlayer("§cYour story has ended...");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!plugin.getConfig().getBoolean("life-system.enabled", true)) {
            sender.sendMessage("§c[PhanTom] Life System is currently disabled in config!");
            return true;
        }

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (label.equalsIgnoreCase("withdraw")) {
            double currentMax = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
            if (currentMax <= 4.0) {
                p.sendMessage("§cYou cannot withdraw your last heart!");
                return true;
            }
            p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(currentMax - 2.0);
            p.getInventory().addItem(getHeartItem());
            p.sendMessage("§a§l-1 Heart §7(Item added to inventory)");
            return true;
        }

        if (label.equalsIgnoreCase("revive") && args.length == 1) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta() || item.getItemMeta().getCustomModelData() != 100) {
                p.sendMessage("§cYou need the §dRevival Book §cto do this!");
                return true;
            }
            String targetName = args[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.isBanned()) {
                p.sendMessage("§cThis player is not eliminated!");
                return true;
            }
            item.setAmount(item.getAmount() - 1);
            Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);
            plugin.getDataManager().addRevivedPlayer(target.getUniqueId());
            Bukkit.broadcastMessage("§a§lREVIVED: §f" + targetName + " has returned!");
            p.getWorld().strikeLightningEffect(p.getLocation());
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            if (cmd.getName().equalsIgnoreCase("revive")) {
                for (OfflinePlayer offline : Bukkit.getBannedPlayers()) sub.add(offline.getName());
            } else {
                for (Player online : Bukkit.getOnlinePlayers()) sub.add(online.getName());
            }
            return sub;
        }
        return null;
    }

    public static ItemStack getHeartItem() {
        ItemStack item = new ItemStack(Material.FERMENTED_SPIDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§l❤ Physical Heart");
            meta.setLore(Arrays.asList("§7Right-click to use.", "§8CustomModelData: 50"));
            meta.setCustomModelData(50);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getRevivalBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§lREVIVAL BOOK");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m-------------------------");
            lore.add("§7A forbidden artifact that can");
            lore.add("§7pull a soul back from the void.");
            lore.add("");
            lore.add("§cUsage:");
            lore.add("§fHold in hand and type:");
            lore.add("§d/revive <player>");
            lore.add("§8§m-------------------------");
            meta.setLore(lore);
            meta.setCustomModelData(100);
            item.setItemMeta(meta);
        }
        return item;
     }
}
