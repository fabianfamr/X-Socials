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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Initialize config managers first
            saveDefaultConfig();
            DebugLogger.debug("Config", "Default config saved/loaded");
            this.languageManager = new LanguageManager(this);
            DebugLogger.debug("Config", "LanguageManager initialized");
        } catch (Exception e) {
            DebugLogger.debug("Config", "Failed to initialize config managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Load libraries before anything else
        DebugLogger.debug("Dependency", "Initializing DependencyManager...");
        new DependencyManager(this).loadDependencies();

        // Initialize remaining managers
        try {
            DebugLogger.debug("Init", "Initializing remaining managers...");
            socialManager = new SocialManager(this);
            DebugLogger.debug("Init", "SocialManager initialized");
            guiManager = new GUIManager(this);
            DebugLogger.debug("Init", "GUIManager initialized");
            broadcastManager = new BroadcastManager(this);
            DebugLogger.debug("Init", "BroadcastManager initialized");
            statsManager = new StatsManager(this);
            DebugLogger.debug("Init", "StatsManager initialized");

            // Register commands
            DebugLogger.debug("Command", "Registering commands...");
            registerCommands();

            // Register listeners (implicit via commands)
            DebugLogger.debug("Init", "Commands and listeners registered");

            // PlaceholderAPI Integration
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                DebugLogger.debug("PAPI", "PlaceholderAPI found, registering expansion");
                new com.fabian.xsocials.utils.XSocialsExpansion(this).register();
            } else {
                DebugLogger.debug("PAPI", "PlaceholderAPI not found, skipping expansion");
            }

        } catch (Exception e) {
            DebugLogger.debug("Init", "Failed to initialize managers", e);
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check for updates
        if (getConfig().getBoolean("check-updates", true)) {
            DebugLogger.debug("Update", "Update checker enabled, scheduling check");
            this.updateChecker = new UpdateChecker(this);
            this.updateChecker.checkForUpdates();
        }

        // Initialize bStats Metrics
        setupMetrics();

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8]   &aEnabled v" + getDescription().getVersion() + "! Socials are ready."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8]   &7Language: &f" + getConfig().getString("language", "EN").toUpperCase()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8] &7----------------------------------------------"));
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
                logWarning("Found a newer configuration version! ("
                        + diskCode + " -> " + jarCode + ")");
                logWarning("Old config backed up to config_old.yml");
            } catch (Exception e) {
                getLogger().warning("Could not back up config.yml: " + e.getMessage());
            }
        }

        // Run the existing merge/update logic to add any missing keys
        ConfigUpdater.update(this, "config.yml", "config.yml");
    }

    @Override
    public void onDisable() {
        DebugLogger.debug("Init", "Plugin disabling...");

        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8] &7----------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8]   &cDisabled v" + getDescription().getVersion() + "! Out."));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&bX-Socials&8] &7----------------------------------------------"));
    }

    private void setupMetrics() {
        if (getConfig().getBoolean("metrics", true)) {
            try {
                metrics = new Metrics(this, 24072);
                metrics.addCustomChart(new Metrics.SingleLineChart("total_uses", () -> statsManager.getTotalUses()));
                metrics.addCustomChart(new Metrics.SimpleBarChart("social_uses", statsManager::getAllSocialUses));
            } catch (Exception e) {
                logWarning("Could not start bStats Metrics: " + e.getMessage());
            }
        }
    }

    public void logInfo(String message) {
        getLogger().info(message);
    }

    public void logWarning(String message) {
        getLogger().warning(message);
    }

    public void logError(String message) {
        getLogger().severe(message);
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