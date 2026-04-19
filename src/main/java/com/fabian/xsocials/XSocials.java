package com.fabian.xsocials;

import com.fabian.xsocials.commands.SocialsCommand;
import com.fabian.xsocials.managers.LanguageManager;
import com.fabian.xsocials.managers.SocialManager;
import com.fabian.xsocials.managers.BroadcastManager;
import com.fabian.xsocials.managers.GUIManager;
import com.fabian.xsocials.utils.ConfigUpdater;
import com.fabian.xsocials.utils.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

public class XSocials extends JavaPlugin {

    private static XSocials instance;
    private LanguageManager languageManager;
    private SocialManager socialManager;
    private GUIManager guiManager;
    private com.fabian.xsocials.managers.BroadcastManager broadcastManager;

    public static final String PREFIX = org.bukkit.ChatColor.DARK_GRAY + "[" + org.bukkit.ChatColor.AQUA + "X-Socials"
            + org.bukkit.ChatColor.DARK_GRAY + "] " + org.bukkit.ChatColor.RESET;

    @Override
    public void onEnable() {
        instance = this;

        try {
            // Save and update configuration
            saveDefaultConfig();

            // Initialize managers
            languageManager = new LanguageManager(this);
            socialManager = new SocialManager(this);
            guiManager = new GUIManager(this);
            broadcastManager = new BroadcastManager(this);

            // Register commands
            registerCommands();

            // Register PAPI if available
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new com.fabian.xsocials.utils.XSocialsExpansion(this).register();
                log(org.bukkit.ChatColor.AQUA + "PlaceholderAPI hooks registered!");
            }

            // Check for updates if enabled
            if (getConfig().getBoolean("check-updates", true)) {
                checkForUpdates();
            }

            log(org.bukkit.ChatColor.GREEN + "X-Socials v" + getDescription().getVersion() + " enabled successfully!");

        } catch (Exception e) {
            getLogger().severe("FATAL ERROR DURING ENABLE: " + e.getMessage());
            e.printStackTrace();
            org.bukkit.Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml", "config.yml");
    }

    @Override
    public void onDisable() {
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
        // Reload config
        reloadConfig();

        // Reload language
        languageManager.reload();

        // Reload socials
        socialManager.reload();

        // Reload broadcasts
        broadcastManager.reload();
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
}
