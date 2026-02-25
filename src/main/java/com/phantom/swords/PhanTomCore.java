package com.phantom.swords;

import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class PhanTomCore extends JavaPlugin {
    private static PhanTomCore instance;
    public Map<UUID, Long> cooldowns = new HashMap<>();
    public static NamespacedKey SWORD_KEY;
    private PlayerDataManager dataManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        SWORD_KEY = new NamespacedKey(this, "sword_type");
        this.dataManager = new PlayerDataManager();

        AdminCommand adminCmd = new AdminCommand();
        getCommand("phantomswords").setExecutor(adminCmd);
        getCommand("phantomswords").setTabCompleter(adminCmd);
        
        getServer().getPluginManager().registerEvents(new RitualManager(), this);
        getServer().getPluginManager().registerEvents(new SwordManager(), this);

        registerUnique9SlotRecipes();
        getLogger().info("§aPhanTom Swords Enabled - Recipes Linked with SwordManager Attributes");
    }

    public static PhanTomCore get() { return instance; }
    public PlayerDataManager getDataManager() { return dataManager; }

    public ItemStack createLegendary(String type) {
        // Recipe ke liye hum basic item banayenge
        // Lore aur baaki attributes SwordManager.java khud apply kar dega jab player ise pakdega
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD); 
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Display name config se le sakte hain ya hardcode kar sakte hain
            var section = getConfig().getConfigurationSection("swords." + type);
            String displayName = (section != null && section.contains("display-name")) 
                ? section.getString("display-name") 
                : type.replace("_", " ");
                
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            
            // Sabse important: Sword ki ID save karna taaki SwordManager ise pehchan sake
            meta.getPersistentDataContainer().set(SWORD_KEY, PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void registerUnique9SlotRecipes() {
        var section = getConfig().getConfigurationSection("swords");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            List<String> recipeItems = getConfig().getStringList("swords." + key + ".recipe_slots");
            if (recipeItems.size() < 9) continue;

            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "unique_9_" + key), createLegendary(key));
            recipe.shape("ABC", "DEF", "GHI");

            char[] keys = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
            for (int i = 0; i < 9; i++) {
                try {
                    Material mat = Material.valueOf(recipeItems.get(i).toUpperCase());
                    recipe.setIngredient(keys[i], mat);
                } catch (Exception e) {
                    getLogger().warning("Invalid Material in config for " + key + ": " + recipeItems.get(i));
                }
            }
            Bukkit.addRecipe(recipe);
        }
    }
                }
