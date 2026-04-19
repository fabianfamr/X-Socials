package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.utils.ConfigUpdater;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        String fileName = language.toLowerCase();
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs();
        }

        languageFile = new File(languagesFolder, fileName);

        // Copy and update default language files
        saveDefaultConfig();

        // Load language configuration
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // Load defaults from JAR if the file exists in JAR
        InputStream defaultStream = plugin.getResource("languages/" + fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            languageConfig.setDefaults(defaultConfig);
        }
    }

    public void saveDefaultConfig() {
        String[] defaults = {"en.yml", "es.yml", "pt.yml", "ja.yml", "ru.yml"};
        for (String def : defaults) {
            ConfigUpdater.update(plugin, "languages/" + def, "languages/" + def);
        }
    }

    public String getMessage(String key) {
        String message = languageConfig.getString(key);
        if (message == null) {
            return ChatColor.RED + "Mensaje no encontrado: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
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
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public void reload() {
        loadLanguage();
    }
}
