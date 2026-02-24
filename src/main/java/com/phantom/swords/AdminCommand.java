package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) return true;
        Player player = (Player) s;
        if (!player.hasPermission("phantomswords.admin")) return true;

        if (a.length > 0) {
            if (a[0].equalsIgnoreCase("reload")) {
                PhanTomCore.get().reloadConfig();
                Bukkit.clearRecipes();
                player.sendMessage("§a[PhanTom] Config Reloaded!");
                return true;
            }
            
            if (a[0].equalsIgnoreCase("give") && a.length >= 3) {
                Player target = Bukkit.getPlayer(a[1]);
                String swordType = a[2].toUpperCase();
                if (target != null && PhanTomCore.get().getConfig().contains("swords." + swordType)) {
                    target.getInventory().addItem(PhanTomCore.get().createLegendary(swordType));
                    player.sendMessage("§aGave " + swordType + " to " + target.getName());
                }
                return true;
            }

            // --- Updated GUI Recipe Check ---
            if (a[0].equalsIgnoreCase("checkrecipe") && a.length >= 2) {
                String type = a[1].toUpperCase();
                openRecipeGUI(player, type);
                return true;
            }
        }
        return true;
    }

    private void openRecipeGUI(Player player, String type) {
        var config = PhanTomCore.get().getConfig();
        if (!config.contains("swords." + type)) {
            player.sendMessage("§cSword type not found!");
            return;
        }

        // 3x3 Recipe ke liye 27 ya 54 slot ka GUI best hai (Center mein recipe dikhane ke liye)
        Inventory gui = Bukkit.createInventory(null, 27, "§0Recipe: " + type);
        
        // Background Filler (Optional: Taaki GUI khali na lage)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        // Config se slots lena
        List<String> slots = config.getStringList("swords." + type + ".recipe_slots");
        
        // Recipe Slots mapping (GUI ke beech mein 3x3 grid)
        int[] guiSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

        if (slots.size() >= 9) {
            for (int i = 0; i < 9; i++) {
                Material mat = Material.matchMaterial(slots.get(i).toUpperCase());
                if (mat != null) {
                    gui.setItem(guiSlots[i], new ItemStack(mat));
                } else {
                    gui.setItem(guiSlots[i], new ItemStack(Material.BARRIER)); // Agar material galat hai
                }
            }
            player.openInventory(gui);
        } else {
            player.sendMessage();
        }
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
