package com.phantom.swords;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("phantomswords.admin")) return true;

        if (a.length > 0) {
            if (a[0].equalsIgnoreCase("reload")) {
                PhanTomCore.get().reloadConfig();
                // Isse recipes refresh ho jayengi bina restart kiye
                Bukkit.clearRecipes(); 
                // Core class ka method call karke recipes phir se register karein
                // Agar aapne Core mein static rakha hai toh call karein
                s.sendMessage("§a[PhanTom] Config Reloaded!");
                return true;
            }
            
            if (a[0].equalsIgnoreCase("give") && a.length >= 3) {
                Player target = Bukkit.getPlayer(a[1]);
                String swordType = a[2].toUpperCase();
                if (target != null && PhanTomCore.get().getConfig().contains("swords." + swordType)) {
                    target.getInventory().addItem(PhanTomCore.get().createLegendary(swordType));
                    s.sendMessage("§aGave " + swordType + " to " + target.getName());
                }
                return true;
            }

            if (a[0].equalsIgnoreCase("checkrecipe") && a.length >= 2) {
                String type = a[1].toUpperCase();
                var config = PhanTomCore.get().getConfig();
                if (config.contains("swords." + type)) {
                    List<String> slots = config.getStringList("swords." + type + ".recipe_slots");
                    if (slots.size() >= 9) {
                        s.sendMessage("§6--- Recipe: " + type + " ---");
                        s.sendMessage("§eRow 1: §f" + slots.get(0) + ", " + slots.get(1) + ", " + slots.get(2));
                        s.sendMessage("§eRow 2: §f" + slots.get(3) + ", " + slots.get(4) + ", " + slots.get(5));
                        s.sendMessage("§eRow 3: §f" + slots.get(6) + ", " + slots.get(7) + ", " + slots.get(8));
                    } else {
                        s.sendMessage("§cRecipe slots are not properly defined in config.");
                    }
                } else {
                    s.sendMessage("§cSword type not found!");
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> completions = new ArrayList<>();
        
        if (a.length == 1) {
            completions.addAll(Arrays.asList("give", "reload", "checkrecipe"));
        } else if (a.length == 2) {
            if (a[0].equalsIgnoreCase("give")) {
                for (Player p : Bukkit.getOnlinePlayers()) completions.add(p.getName());
            } else if (a[0].equalsIgnoreCase("checkrecipe")) {
                var section = PhanTomCore.get().getConfig().getConfigurationSection("swords");
                if (section != null) completions.addAll(section.getKeys(false));
            }
        } else if (a.length == 3 && a[0].equalsIgnoreCase("give")) {
            var section = PhanTomCore.get().getConfig().getConfigurationSection("swords");
            if (section != null) completions.addAll(section.getKeys(false));
        }

        return StringUtil.copyPartialMatches(a[a.length - 1], completions, new ArrayList<>());
    }
  }
