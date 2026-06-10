package com.fabian.xsocials;

import com.fabian.xsocials.commands.SocialsCommand;
import com.fabian.xsocials.managers.DependencyManager;
import com.fabian.xsocials.managers.LanguageManager;
import com.fabian.xsocials.managers.SocialManager;
import com.fabian.xsocials.managers.BroadcastManager;
import com.fabian.xsocials.managers.GUIManager;
import com.fabian.xsocials.utils.ConfigUpdater;
import com.fabian.xsocials.utils.DebugLogger;
import com.fabian.xsocials.utils.UpdateChecker;
import com.fabian.xsocials.utils.StatsManager;
import com.fabian.xsocials.metrics.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class XSocials extends JavaPlugin {

    private static XSocials instance;
    private LanguageManager languageManager;
    private SocialManager socialManager;
    private GUIManager guiManager;
    private com.fabian.xsocials.managers.BroadcastManager broadcastManager;
    private StatsManager statsManager;
    private Metrics metrics;

    public static final String PREFIX = org.bukkit.ChatColor.DARK_GRAY + "[" + org.bukkit.ChatColor.AQUA + "X-Socials"
            + org.bukkit.ChatColor.DARK_GRAY + "] " + org.bukkit.ChatColor.RESET;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Load libraries before anything else
            DebugLogger.debug("Enable", "Loading dependencies...");
            new DependencyManager(this).loadDependencies();
            DebugLogger.debug("Enable", "Dependencies loaded successfully");

            String version = getDescription().getVersion();
            log(org.bukkit.ChatColor.DARK_AQUA + "Enabling X-Socials v" + version);

            // Save and update configuration (includes version-based backup + merge)
            DebugLogger.debug("Enable", "Saving/updating default config...");
            saveDefaultConfig();
            DebugLogger.debug("Enable", "Config updated (debug=" + getConfig().getBoolean("debug", false) + ")");

            // Initialize managers
            DebugLogger.debug("Enable", "Initializing LanguageManager...");
            languageManager = new LanguageManager(this);
            DebugLogger.debug("Enable", "Initializing SocialManager...");
            socialManager = new SocialManager(this);
            DebugLogger.debug("Enable", "Initializing GUIManager...");
            guiManager = new GUIManager(this);
            DebugLogger.debug("Enable", "Initializing BroadcastManager...");
            broadcastManager = new BroadcastManager(this);
            DebugLogger.debug("Enable", "Initializing StatsManager...");
            statsManager = new StatsManager(this);

            log(org.bukkit.ChatColor.GREEN + "Successfully enabled!");
            DebugLogger.debug("Enable", "All managers initialized successfully");

            // Initialize metrics
            if (getConfig().getBoolean("metrics", true)) {
                metrics = new Metrics(this, 24072);
                metrics.addCustomChart(new Metrics.SingleLineChart("total_uses", () -> statsManager.getTotalUses()));
                metrics.addCustomChart(new Metrics.SimpleBarChart("social_uses", statsManager::getAllSocialUses));
            }

            // Register commands
            DebugLogger.debug("Enable", "Registering commands...");
            registerCommands();
            DebugLogger.debug("Enable", "Commands registered");

            // Register PAPI if available
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                DebugLogger.debug("Enable", "Registering PlaceholderAPI expansion...");
                new com.fabian.xsocials.utils.XSocialsExpansion(this).register();
                log(org.bukkit.ChatColor.AQUA + "PlaceholderAPI hooks registered!");
                DebugLogger.debug("Enable", "PlaceholderAPI expansion registered");
            } else {
                DebugLogger.debug("Enable", "PlaceholderAPI not found, skipping expansion");
            }

            // Check for updates if enabled
            if (getConfig().getBoolean("check-updates", true)) {
                checkForUpdates();
            }

            log("----------------------------------------------");
            log(org.bukkit.ChatColor.GREEN + "  Enabled v" + version + "! Enjoy socials!");
            log(org.bukkit.ChatColor.AQUA + "  Language: " + getConfig().getString("language", "EN").toUpperCase());
            log("----------------------------------------------");

        } catch (Exception e) {
            getLogger().severe("FATAL ERROR DURING ENABLE: " + e.getMessage());
            e.printStackTrace();
            org.bukkit.Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();

        File configFile = new File(getDataFolder(), "config.yml");

        // Read the 'code' from the JAR default config
        int jarCode = 0;
        try (InputStream is = getResource("config.yml")) {
            if (is != null) {
                YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(is, StandardCharsets.UTF_8));
                jarCode = jarConfig.getInt("code", 0);
            }
        } catch (Exception ignored) {
        }

        // Read the 'code' from the on-disk config
        int diskCode = 0;
        if (configFile.exists()) {
            YamlConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
            diskCode = diskConfig.getInt("code", 0);
        }

        // If the JAR ships a newer config version, back up the old one first
        if (diskCode < jarCode) {
            File backupFile = new File(getDataFolder(), "config_old.yml");
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log(org.bukkit.ChatColor.YELLOW + "Found a newer configuration version! ("
                        + diskCode + " -> " + jarCode + ")");
                log(org.bukkit.ChatColor.YELLOW + "Old config backed up to config_old.yml");
            } catch (Exception e) {
                getLogger().warning("Could not back up config.yml: " + e.getMessage());
            }
        }

        // Run the existing merge/update logic to add any missing keys
        ConfigUpdater.update(this, "config.yml", "config.yml");
    }

    @Override
    public void onDisable() {
        DebugLogger.debug("Disable", "Plugin disabling...");
        log(org.bukkit.ChatColor.RED + "X-Socials disabled successfully!");
    }

    public void log(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(PREFIX + message);
    }

    public void logWithConfigPrefix(String message) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(languageManager.getPrefix() + " " + message);
    }

    private void registerCommands() {
        // Register main command /xsocials (aliases: /xs)
        SocialsCommand socialsCommand = new SocialsCommand(this);
        getCommand("xsocials").setExecutor(socialsCommand);
        getCommand("xsocials").setTabCompleter(socialsCommand);

        // Register dynamic social commands
        socialManager.registerSocialCommands();
    }

    private void checkForUpdates() {
        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
    }

    public void reload() {
        DebugLogger.debug("Reload", "Starting full plugin reload...");
        // Reload config
        reloadConfig();
        DebugLogger.debug("Reload", "Config reloaded");

        // Reload language
        languageManager.reload();
        DebugLogger.debug("Reload", "LanguageManager reloaded");

        // Reload socials
        socialManager.reload();
        DebugLogger.debug("Reload", "SocialManager reloaded");

        // Reload broadcasts
        broadcastManager.reload();
        DebugLogger.debug("Reload", "BroadcastManager reloaded");

        // Reload stats
        statsManager.reload();
        DebugLogger.debug("Reload", "StatsManager reloaded");
        DebugLogger.debug("Reload", "Full reload complete");
    }

    public static XSocials getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public SocialManager getSocialManager() {
        return socialManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public com.fabian.xsocials.managers.BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}