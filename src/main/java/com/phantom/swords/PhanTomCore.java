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
        
        // Load saved revived players from config
        dataManager.loadRevivedPlayers();

        // Admin & Sword Commands
        AdminCommand adminCmd = new AdminCommand();
        getCommand("phantomswords").setExecutor(adminCmd);
        getCommand("phantomswords").setTabCompleter(adminCmd);
        
        // Life System Setup
        LifeManager lifeManager = new LifeManager(this);
        getServer().getPluginManager().registerEvents(lifeManager, this);
        
        // Register Life Commands & Tab Completers
        String[] lifeCmds = {"revive", "withdraw", "sethearts"};
        for (String s : lifeCmds) {
            getCommand(s).setExecutor(lifeManager);
            getCommand(s).setTabCompleter(lifeManager);
        }

        getServer().getPluginManager().registerEvents(new RitualManager(), this);
        getServer().getPluginManager().registerEvents(new SwordManager(), this);

        registerUnique9SlotRecipes();
        registerRevivalRecipe(); // Life System Recipe
        
        getLogger().info("§aPhanTom Core: Swords & Lifes System Enabled!");
    }

    public static PhanTomCore get() { return instance; }
    public PlayerDataManager getDataManager() { return dataManager; }

    public ItemStack createLegendary(String type) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD); 
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            var section = getConfig().getConfigurationSection("swords." + type);
            String displayName = (section != null && section.contains("display-name")) 
                ? section.getString("display-name") : type.replace("_", " ");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
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

    // --- LIFE SYSTEM REVIVAL BOOK RECIPE ---
    private void registerRevivalRecipe() {
        ItemStack result = LifeManager.getRevivalBook();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "revival_ritual_book"), result);
        recipe.shape("NTN", "EHE", "BNB");
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('E', Material.ENCHANTED_BOOK);
        recipe.setIngredient('H', Material.HEART_OF_THE_SEA);
        recipe.setIngredient('B', Material.NETHERITE_BLOCK);
        Bukkit.addRecipe(recipe);
    }
        }
