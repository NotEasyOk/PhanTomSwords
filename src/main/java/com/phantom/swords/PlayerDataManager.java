package com.phantom.swords;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PlayerDataManager {

    private final File file;
    private final FileConfiguration config;

    public PlayerDataManager() {
        // Plugin folder mein players.yml file banayega
        this.file = new File(PhanTomCore.get().getDataFolder(), "players.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // Cooldown Data Save karne ke liye
    public void saveCooldowns(UUID uuid, HashMap<String, Long> cooldowns) {
        for (String action : cooldowns.keySet()) {
            config.set(uuid.toString() + "." + action, cooldowns.get(action));
        }
        saveFile();
    }

    // Cooldown Data Load karne ke liye
    public HashMap<String, Long> loadCooldowns(UUID uuid) {
        HashMap<String, Long> cooldowns = new HashMap<>();
        if (config.contains(uuid.toString())) {
            for (String action : config.getConfigurationSection(uuid.toString()).getKeys(false)) {
                cooldowns.put(action, config.getLong(uuid.toString() + "." + action));
            }
        }
        return cooldowns;
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
          }
