package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.utils.ConfigUpdater;
import com.fabian.xsocials.utils.DebugLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private final XSocials plugin;
    private YamlConfiguration languageConfig;
    private File languageFile;

    public LanguageManager(XSocials plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    private void loadLanguage() {
        String language = plugin.getConfig().getString("language", "EN");
        DebugLogger.debug("LanguageManager", "Loading language: " + language);
        String fileName = language.toLowerCase();
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        File languagesFolder = new File(plugin.getDataFolder(), "messages");
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs();
        }

        languageFile = new File(languagesFolder, fileName);

        // Copy and update default language files
        saveDefaultConfig();

        // Load language configuration
        try {
            languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(languageFile), StandardCharsets.UTF_8));
        } catch (java.io.FileNotFoundException e) {
            languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        }

        // Load defaults from JAR if the file exists in JAR
        InputStream defaultStream = plugin.getResource("messages/" + fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            languageConfig.setDefaults(defaultConfig);
        }
        DebugLogger.debug("LanguageManager", "Language file loaded: " + fileName);
    }

    public void saveDefaultConfig() {
        String[] defaults = {"en.yml", "es.yml", "pt.yml", "ja.yml", "ru.yml"};
        for (String def : defaults) {
            ConfigUpdater.update(plugin, "messages/" + def, "messages/" + def);
        }
    }

    public String getMessage(String key) {
        String message = languageConfig.getString(key);
        if (message == null) {
            DebugLogger.debug("LanguageManager", "Missing message key: " + key);
            return ChatColor.RED + "Mensaje no encontrado: " + key;
        }
        return com.fabian.xsocials.utils.ColorUtils.translate(message);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);

        // Reemplazar placeholders {0}, {1}, etc.
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", replacements[i]);
        }

        return message;
    }

    public String getPrefix() {
        String prefix = plugin.getConfig().getString("prefix");
        if (prefix == null) {
            return ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "X-Socials" + ChatColor.DARK_GRAY + "] "
                    + ChatColor.RESET;
        }
        return com.fabian.xsocials.utils.ColorUtils.translate(prefix);
    }

    public String getCurrentLanguage() {
        return plugin.getConfig().getString("language", "EN");
    }

    public List<String> getAvailableLanguages() {
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        File[] files = messagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        List<String> languages = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                languages.add(name.substring(0, name.length() - 4));
            }
        }
        return languages;
    }

    public boolean forceReloadMessages(String langCode) {
        String fileName = langCode.toLowerCase();
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }
        File targetFile = new File(plugin.getDataFolder(), "messages/" + fileName);
        if (!targetFile.exists()) {
            return false;
        }
        ConfigUpdater.update(plugin, "messages/" + fileName, "messages/" + fileName);
        if (getCurrentLanguage().equalsIgnoreCase(langCode)) {
            reload();
        }
        return true;
    }

    public int forceReloadAllMessages() {
        List<String> languages = getAvailableLanguages();
        for (String lang : languages) {
            forceReloadMessages(lang);
        }
        return languages.size();
    }

    public boolean forceResetMessages(String langCode) {
        String fileName = langCode.toLowerCase();
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }
        if (plugin.getResource("messages/" + fileName) == null) {
            return false;
        }
        File targetFile = new File(plugin.getDataFolder(), "messages/" + fileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        plugin.saveResource("messages/" + fileName, true);
        if (getCurrentLanguage().equalsIgnoreCase(langCode)) {
            reload();
        }
        return true;
    }

    public int forceResetAllMessages() {
        String[] defaults = {"en.yml", "es.yml", "pt.yml", "ja.yml", "ru.yml"};
        int count = 0;
        for (String def : defaults) {
            String langCode = def.substring(0, def.length() - 4);
            File targetFile = new File(plugin.getDataFolder(), "messages/" + def);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            plugin.saveResource("messages/" + def, true);
            count++;
            if (getCurrentLanguage().equalsIgnoreCase(langCode)) {
                reload();
            }
        }
        return count;
    }

    public void reload() {
        loadLanguage();
    }
}
