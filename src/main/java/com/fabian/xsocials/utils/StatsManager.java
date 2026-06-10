package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final XSocials plugin;
    private final File statsFile;
    private YamlConfiguration statsConfig;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public StatsManager(XSocials plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadStats();
    }

    private void loadStats() {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create stats file: " + e.getMessage());
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        if (!statsConfig.isSet("total-uses")) {
            statsConfig.set("total-uses", 0);
        }
        if (!statsConfig.isSet("socials")) {
            statsConfig.createSection("socials");
        }
    }

    public void incrementUse(String socialName) {
        statsConfig.set("total-uses", statsConfig.getInt("total-uses", 0) + 1);
        statsConfig.set("socials." + socialName.toLowerCase() + ".uses",
            statsConfig.getInt("socials." + socialName.toLowerCase() + ".uses", 0) + 1);
        saveStats();
    }

    public int getTotalUses() {
        return statsConfig.getInt("total-uses", 0);
    }

    public int getSocialUses(String socialName) {
        return statsConfig.getInt("socials." + socialName.toLowerCase() + ".uses", 0);
    }

    public Map<String, Integer> getAllSocialUses() {
        Map<String, Integer> result = new HashMap<>();
        if (statsConfig.isConfigurationSection("socials")) {
            for (String key : statsConfig.getConfigurationSection("socials").getKeys(false)) {
                result.put(key, statsConfig.getInt("socials." + key + ".uses", 0));
            }
        }
        return result;
    }

    public boolean isOnCooldown(UUID playerUuid, String socialName) {
        if (!cooldowns.containsKey(playerUuid)) return false;
        Long lastUse = cooldowns.get(playerUuid).get(socialName.toLowerCase());
        if (lastUse == null) return false;
        return (System.currentTimeMillis() - lastUse) < (getCooldownDuration(socialName) * 1000);
    }

    private long getCooldownDuration(String socialName) {
        com.fabian.xsocials.models.SocialNetwork social = plugin.getSocialManager().getSocialNetwork(socialName);
        if (social != null && social.getCooldownSeconds() > 0) {
            return social.getCooldownSeconds();
        }
        return plugin.getConfig().getLong("cooldown.seconds", 0);
    }

    public long getCooldownRemaining(UUID playerUuid, String socialName) {
        if (!cooldowns.containsKey(playerUuid)) return 0;
        Long lastUse = cooldowns.get(playerUuid).get(socialName.toLowerCase());
        if (lastUse == null) return 0;
        long cooldownSeconds = getCooldownDuration(socialName);
        if (cooldownSeconds <= 0) return 0;
        long remaining = (cooldownSeconds * 1000) - (System.currentTimeMillis() - lastUse);
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void setCooldown(UUID playerUuid, String socialName) {
        cooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>())
            .put(socialName.toLowerCase(), System.currentTimeMillis());
    }

    public void clearCooldowns(UUID playerUuid) {
        cooldowns.remove(playerUuid);
    }

    private void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats: " + e.getMessage());
        }
    }

    public void reload() {
        loadStats();
    }
}