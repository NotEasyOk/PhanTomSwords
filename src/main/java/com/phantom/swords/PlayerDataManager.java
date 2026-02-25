package com.phantom.swords;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerDataManager {

    private final File file;
    private final FileConfiguration config;
    private List<String> revivedPlayers; // Revive list handle karne ke liye

    public PlayerDataManager() {
        this.file = new File(PhanTomCore.get().getDataFolder(), "players.yml");
        if (!file.exists()) {
            try {
                // Folder check taaki error na aaye
                if (!PhanTomCore.get().getDataFolder().exists()) {
                    PhanTomCore.get().getDataFolder().mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        this.revivedPlayers = new ArrayList<>();
    }

    // --- REVIVE DATA METHODS (NAYA) ---

    public void loadRevivedPlayers() {
        this.revivedPlayers = config.getStringList("pending-revivals");
    }

    public void addRevivedPlayer(UUID uuid) {
        if (!revivedPlayers.contains(uuid.toString())) {
            revivedPlayers.add(uuid.toString());
            config.set("pending-revivals", revivedPlayers);
            saveFile();
        }
    }

    public boolean isRevived(UUID uuid) {
        return revivedPlayers.contains(uuid.toString());
    }

    public void removeRevivedPlayer(UUID uuid) {
        revivedPlayers.remove(uuid.toString());
        config.set("pending-revivals", revivedPlayers);
        saveFile();
    }

    // --- COOLDOWN DATA LOGIC ---

    public void saveCooldowns(UUID uuid, HashMap<String, Long> cooldowns) {
        if (cooldowns == null) return;
        for (String action : cooldowns.keySet()) {
            config.set(uuid.toString() + "." + action, cooldowns.get(action));
        }
        saveFile();
    }

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
