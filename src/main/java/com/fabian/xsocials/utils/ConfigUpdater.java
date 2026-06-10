package com.fabian.xsocials.utils;

import com.fabian.xsocials.XSocials;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ConfigUpdater {

    /**
     * Updates a configuration file by adding missing keys from a resource in the JAR.
     * 
     * @param plugin The plugin instance.
     * @param fileName The name of the file to update (relative to data folder).
     * @param resourcePath The path to the default resource in the JAR.
     */
    public static void update(XSocials plugin, String fileName, String resourcePath) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(resourcePath, false);
            return;
        }

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
        
        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) return;
            
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
            boolean changed = merge(defaultConfig, userConfig);
            
            if (changed) {
                userConfig.save(configFile);
                plugin.log(org.bukkit.ChatColor.AQUA + "Updated configuration file: " + fileName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not update " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Updates a dynamic configuration file (like social networks) where the root key matches
     * a template's root key structure.
     */
    public static void updateDynamic(XSocials plugin, String fileName, String resourcePath, String templateRoot) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) return;

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
        Set<String> userRootKeys = userConfig.getKeys(false);
        if (userRootKeys.isEmpty()) return;
        
        String userRootKey = userRootKeys.iterator().next();
        ConfigurationSection userSection = userConfig.getConfigurationSection(userRootKey);
        if (userSection == null) return;

        try (InputStream is = plugin.getResource(resourcePath)) {
            if (is == null) return;
            
            YamlConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
            ConfigurationSection templateSection = templateConfig.getConfigurationSection(templateRoot);
            if (templateSection == null) return;

            boolean changed = merge(templateSection, userSection);
            
            if (changed) {
                userConfig.save(configFile);
                plugin.log(org.bukkit.ChatColor.AQUA + "Updated dynamic file: " + fileName + " (" + userRootKey + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not update dynamic " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Recursively merges missing keys from source (default) to target (user).
     * 
     * @return true if any keys were added.
     */
    public static boolean merge(ConfigurationSection source, ConfigurationSection target) {
        boolean changed = false;
        Set<String> keys = source.getKeys(true);
        
        for (String key : keys) {
            if (!target.contains(key)) {
                target.set(key, source.get(key));
                changed = true;
            }
        }
        
        return changed;
    }
}
