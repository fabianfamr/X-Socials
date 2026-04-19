package com.fabian.xsocials.managers;

import com.fabian.xsocials.XSocials;
import com.fabian.xsocials.commands.DynamicSocialCommand;
import com.fabian.xsocials.models.SocialNetwork;
import com.fabian.xsocials.utils.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class SocialManager {

    private final XSocials plugin;
    private final Map<String, SocialNetwork> socialNetworks;
    private final File socialsFolder;
    private final Map<String, File> socialFiles; // Map social name -> file

    public SocialManager(XSocials plugin) {
        this.plugin = plugin;
        this.socialNetworks = new HashMap<>();
        this.socialFiles = new HashMap<>();
        this.socialsFolder = new File(plugin.getDataFolder(), "socials");

        // Create folder and save defaults
        saveDefaultConfig();

        // Cargar redes sociales
        loadSocialNetworks();
    }

    public void saveDefaultConfig() {
        if (!socialsFolder.exists()) {
            socialsFolder.mkdirs();
        }

        // 1. Ensure default files exist
        String[] defaultSocials = {"discord.yml", "twitter.yml", "youtube.yml"};
        for (String social : defaultSocials) {
            File file = new File(socialsFolder, social);
            if (!file.exists()) {
                plugin.saveResource("socials/" + social, false);
            }
        }

        // 2. Detect and update ALL social files (including custom ones)
        File[] files = socialsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String resourcePath = "socials/" + fileName;
                
                // If the specific file exists in JAR, update normally
                if (plugin.getResource(resourcePath) != null) {
                    ConfigUpdater.update(plugin, "socials/" + fileName, resourcePath);
                } else {
                    // Otherwise, it's a custom social, update using template
                    ConfigUpdater.updateDynamic(plugin, "socials/" + fileName, "socials/template.yml", "template");
                }
            }
        }

        // Update broadcasts
        ConfigUpdater.update(plugin, "broadcasts.yml", "broadcasts.yml");
    }

    public void loadSocialNetworks() {
        socialNetworks.clear();
        socialFiles.clear();

        File[] files = socialsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.log(org.bukkit.ChatColor.YELLOW
                    + "No se encontraron archivos de redes sociales en la carpeta 'socials'");
            return;
        }

        int loadedCount = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Obtener la primera clave (nombre de la red social)
                Set<String> keys = config.getKeys(false);
                if (keys.isEmpty()) {
                    plugin.getLogger().warning("Archivo vacío: " + file.getName());
                    continue;
                }

                String socialName = keys.iterator().next();
                ConfigurationSection section = config.getConfigurationSection(socialName);

                if (section == null) {
                    plugin.getLogger().warning("Sección inválida en: " + file.getName());
                    continue;
                }

                String command = section.getString("command");
                List<String> aliases = section.getStringList("aliases");
                String permission = section.getString("permission", "");
                String link = section.getString("link", "");
                String hover = section.getString("hover", "");
                List<String> messages = section.getStringList("message");
                List<String> rewardCommands = section.getStringList("rewards.commands");
                boolean enabled = section.getBoolean("enabled", true);
                boolean registerCommand = section.getBoolean("register-command", true);
                String itemType = section.getString("gui-item.type", "BLOCK");
                String itemValue = section.getString("gui-item.material", "PAPER");

                if (command == null || command.isEmpty()) {
                    plugin.getLogger().warning("Comando no definido en: " + file.getName());
                    continue;
                }

                SocialNetwork social = new SocialNetwork(socialName, command, aliases, permission, link, hover, messages,
                        rewardCommands, enabled, registerCommand, itemType, itemValue);
                socialNetworks.put(command.toLowerCase(), social);
                socialFiles.put(socialName, file);
                loadedCount++;

            } catch (Exception e) {
                plugin.getLogger().severe("Error al cargar " + file.getName() + ": " + e.getMessage());
            }
        }

        // Cargar datos de recompensas
        loadRewardsData();

        // Log summary message
        if (loadedCount > 0) {
            plugin.log(org.bukkit.ChatColor.GREEN + "Social networks loaded: " + loadedCount);
        }
    }

    private File dataFile;
    private YamlConfiguration dataConfig;

    private void loadRewardsData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (Exception ignored) {
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean hasClaimedReward(UUID uuid, String socialName) {
        return dataConfig.getBoolean("rewards." + uuid.toString() + "." + socialName.toLowerCase(), false);
    }

    public void setClaimedReward(UUID uuid, String socialName) {
        dataConfig.set("rewards." + uuid.toString() + "." + socialName.toLowerCase(), true);
        try {
            dataConfig.save(dataFile);
        } catch (Exception ignored) {
        }
    }

    public void registerSocialCommands() {
        try {
            CommandMap commandMap = null;
            
            // Try via CraftServer
            try {
                Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getServer());
            } catch (Exception ignored) {}

            // Try via SimplePluginManager if CraftServer failed
            if (commandMap == null) {
                try {
                    Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                    f.setAccessible(true);
                    commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
                } catch (Exception ignored) {}
            }

            if (commandMap == null) {
                plugin.getLogger().severe("Could not find CommandMap. Dynamic social commands will not work.");
                return;
            }

            for (SocialNetwork social : socialNetworks.values()) {
                if (social.shouldRegisterCommand()) {
                    DynamicSocialCommand dynamicCommand = new DynamicSocialCommand(social, plugin);
                    commandMap.register(plugin.getName(), dynamicCommand);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error registering dynamic commands: " + e.getMessage());
        }
    }

    public void reload() {
        loadSocialNetworks();
        registerSocialCommands();
    }

    public SocialNetwork getSocialNetwork(String command) {
        return socialNetworks.get(command.toLowerCase());
    }

    public Collection<SocialNetwork> getAllSocialNetworks() {
        return socialNetworks.values();
    }

    public Map<String, SocialNetwork> getSocialNetworks() {
        return socialNetworks;
    }

    public File getSocialFile(String socialName) {
        return socialFiles.get(socialName);
    }

    public void executeSocialCommand(org.bukkit.command.CommandSender sender, SocialNetwork social) {
        if (!social.isEnabled()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("social-not-found").replace("{social}", social.getName()));
            return;
        }

        if (!(sender instanceof org.bukkit.entity.Player)) {
            for (String message : social.getMessages()) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    message.replace("{link}", social.getLink()).replace("{player}", "Console")));
            }
            return;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

        // Reward logic
        if (plugin.getConfig().getBoolean("rewards.enable", true)) {
            if (!hasClaimedReward(player.getUniqueId(), social.getName())) {
                boolean hasReward = social.getRewardCommands() != null && !social.getRewardCommands().isEmpty();
                if (hasReward) {
                    for (String cmd : social.getRewardCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
                    }
                    setClaimedReward(player.getUniqueId(), social.getName());
                    player.sendMessage(plugin.getLanguageManager().getMessage("reward-received", social.getName()));
                }
            }
        }

        // Interactive messages
        String hoverText = org.bukkit.ChatColor.translateAlternateColorCodes('&', social.getHover());
        if (hoverText.isEmpty()) hoverText = "Click to visit!";

        boolean hasSpigot = isSpigot();

        for (String message : social.getMessages()) {
            String processed = message.replace("{link}", social.getLink()).replace("{player}", player.getName());
            processed = org.bukkit.ChatColor.translateAlternateColorCodes('&', processed);

            if (processed.contains(social.getLink()) && hasSpigot) {
                net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(processed);
                component.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, social.getLink()));
                setHoverEvent(component, hoverText);
                player.spigot().sendMessage(component);
            } else {
                player.sendMessage(processed);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setHoverEvent(net.md_5.bungee.api.chat.TextComponent component, String text) {
        try {
            // Modern API (1.16+)
            Class<?> textClass = Class.forName("net.md_5.bungee.api.chat.hover.content.Text");
            Object textContent = textClass.getConstructor(String.class).newInstance(text);
            Object hoverEvent = net.md_5.bungee.api.chat.HoverEvent.class.getConstructor(
                net.md_5.bungee.api.chat.HoverEvent.Action.class, 
                java.util.List.class
            ).newInstance(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                java.util.Collections.singletonList(textContent)
            );
            component.setHoverEvent((net.md_5.bungee.api.chat.HoverEvent) hoverEvent);
        } catch (Exception e) {
            // Legacy API (1.8 - 1.15)
            component.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
                new net.md_5.bungee.api.chat.BaseComponent[]{new net.md_5.bungee.api.chat.TextComponent(text)}
            ));
        }
    }

    private boolean isSpigot() {
        try {
            Class.forName("org.bukkit.entity.Player$Spigot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
